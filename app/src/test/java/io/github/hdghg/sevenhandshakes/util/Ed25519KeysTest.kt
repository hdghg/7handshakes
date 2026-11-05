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
    fun invalidInput_returnsNull() {
        assertThat(Ed25519Keys.publicKeyOf("")).isNull()
        assertThat(Ed25519Keys.publicKeyOf("not base64 !!")).isNull()
        assertThat(Ed25519Keys.publicKeyOf("AAAA")).isNull()
        // A public key is not a private key.
        assertThat(Ed25519Keys.publicKeyOf("MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE=")).isNull()
    }
}
