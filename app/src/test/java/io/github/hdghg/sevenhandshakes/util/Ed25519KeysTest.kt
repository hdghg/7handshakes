package io.github.hdghg.sevenhandshakes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Ed25519KeysTest {
    @Test
    fun generatedKey_hasPublicKey() {
        val privateKey = Ed25519Keys.generatePrivateKey()
        val publicKey = Ed25519Keys.publicKeyOf(privateKey)
        assertThat(privateKey).startsWith("MC4CAQAwBQYDK2VwBCIEI")
        assertThat(publicKey).startsWith("MCowBQYDK2VwAyEA")
        assertThat(Ed25519Keys.publicKeyOf(privateKey)).isEqualTo(publicKey)
    }

    @Test
    fun rfc8410Example_givesKnownPublicKey() {
        // Key pair from RFC 8410, section 10.3.
        val privateKey = "MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp+K06/nwoy/HU++CXqI9EdVhC"
        assertThat(Ed25519Keys.publicKeyOf(privateKey))
            .isEqualTo("MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE=")
    }

    @Test
    fun canonicalPrivateKey_dropsEmbeddedPublicKey() {
        // PKCS#8 v2 form (with embedded public key), as written by BouncyCastle's PrivateKeyInfoFactory.
        val v2 = "MFECAQEwBQYDK2VwBCIEIBuqE2CI52Q0lHMG0JKua2vxaEqVXvE1BlheiCL4mLolgSEAxBq9nE8d6sz74TrRfBjom+LlfL3+ZKGJddjWEV8A2/4="
        val v1 = "MC4CAQAwBQYDK2VwBCIEIBuqE2CI52Q0lHMG0JKua2vxaEqVXvE1BlheiCL4mLol"
        assertThat(Ed25519Keys.canonicalPrivateKey(v2)).isEqualTo(v1)
        assertThat(Ed25519Keys.canonicalPrivateKey(" $v1\n")).isEqualTo(v1)
        assertThat(Ed25519Keys.canonicalPrivateKey("AAAA")).isNull()
    }

    @Test
    fun openSshKey_isAccepted() {
        // Throwaway key: ssh-keygen -t ed25519 -N "" -C test@example
        val openSsh = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
            QyNTUxOQAAACBZCF3/zjVrB/cT2pXwbfB/HZwIX4Lx4BsMri3RR9CvAwAAAJD72mJE+9pi
            RAAAAAtzc2gtZWQyNTUxOQAAACBZCF3/zjVrB/cT2pXwbfB/HZwIX4Lx4BsMri3RR9CvAw
            AAAEDCXOVxHFnTunXT+I69qpsuLNDqtajaPn4zqWqlbYHIIVkIXf/ONWsH9xPalfBt8H8d
            nAhfgvHgGwyuLdFH0K8DAAAADHRlc3RAZXhhbXBsZQE=
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()
        // Same 32-byte public key as the matching .pub file, in X.509 form.
        val publicKey = "MCowBQYDK2VwAyEAWQhd/841awf3E9qV8G3wfx2cCF+C8eAbDK4t0UfQrwM="
        assertThat(Ed25519Keys.publicKeyOf(openSsh)).isEqualTo(publicKey)
        assertThat(Ed25519Keys.isEncryptedOpenSshKey(openSsh)).isFalse()

        val canonical = Ed25519Keys.canonicalPrivateKey(openSsh)
        assertThat(canonical).startsWith("MC4CAQAwBQYDK2VwBCIEI")
        assertThat(Ed25519Keys.publicKeyOf(canonical!!)).isEqualTo(publicKey)
    }

    @Test
    fun encryptedOpenSshKey_isRejected() {
        // Throwaway key: ssh-keygen -t ed25519 -N secret -C test@example
        val encrypted = """
            -----BEGIN OPENSSH PRIVATE KEY-----
            b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABDRSKzh05
            g3RNJqmPlWG/kiAAAAGAAAAAEAAAAzAAAAC3NzaC1lZDI1NTE5AAAAIOKdty882pQz8Ku9
            9LGuHpN8GMRF9TTT8VgcFkFFjLqlAAAAkEm/Bu7NaZk+f/OHJiS9krCua72hnh9bvdtDDy
            FqUZ91NVVGyInSHcTi4rhziAqtM6pGPUaVm8gjnIVAU0eO6cse7ug/CFDcIon4bn2UdPUa
            tNTKDAaaSlQ/2uHuuMayGG+XzN51wLajdKUIOwoQrmFyCPl8Cefv7c4ZbdCayqnYDf9HEE
            sZUDLTwz8UIcKLng==
            -----END OPENSSH PRIVATE KEY-----
        """.trimIndent()
        assertThat(Ed25519Keys.publicKeyOf(encrypted)).isNull()
        assertThat(Ed25519Keys.isEncryptedOpenSshKey(encrypted)).isTrue()
    }

    @Test
    fun pemPkcs8Key_isAccepted() {
        // Throwaway key: openssl genpkey -algorithm ed25519
        val pem = """
            -----BEGIN PRIVATE KEY-----
            MC4CAQAwBQYDK2VwBCIEIIDaIwE+wRTdaqiH+c5oF6x78ECR+z9WG/bBepfOXcO0
            -----END PRIVATE KEY-----
        """.trimIndent()
        assertThat(Ed25519Keys.publicKeyOf(pem))
            .isEqualTo("MCowBQYDK2VwAyEAmx4/Fk2GAmI00M0/tKJw8KIpbbmkVwIWjyIn9m8uq5I=")
        assertThat(Ed25519Keys.canonicalPrivateKey(pem))
            .isEqualTo("MC4CAQAwBQYDK2VwBCIEIIDaIwE+wRTdaqiH+c5oF6x78ECR+z9WG/bBepfOXcO0")
        assertThat(Ed25519Keys.isEncryptedOpenSshKey(pem)).isFalse()
    }

    @Test
    fun isValidPublicKey() {
        val publicKey = "MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE="
        assertThat(Ed25519Keys.isValidPublicKey(publicKey)).isTrue()
        assertThat(Ed25519Keys.isValidPublicKey(" $publicKey\n")).isTrue()
        assertThat(Ed25519Keys.isValidPublicKey(Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!)).isTrue()

        assertThat(Ed25519Keys.isValidPublicKey("")).isFalse()
        assertThat(Ed25519Keys.isValidPublicKey("not base64 !!")).isFalse()
        assertThat(Ed25519Keys.isValidPublicKey("AAAA")).isFalse()
        // A private key is not a public key.
        assertThat(Ed25519Keys.isValidPublicKey("MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp+K06/nwoy/HU++CXqI9EdVhC")).isFalse()
    }

    @Test
    fun invalidInput_returnsNull() {
        assertThat(Ed25519Keys.publicKeyOf("")).isNull()
        assertThat(Ed25519Keys.publicKeyOf("not base64 !!")).isNull()
        assertThat(Ed25519Keys.publicKeyOf("AAAA")).isNull()
        // A public key is not a private key.
        assertThat(Ed25519Keys.publicKeyOf("MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE=")).isNull()
    }
}
