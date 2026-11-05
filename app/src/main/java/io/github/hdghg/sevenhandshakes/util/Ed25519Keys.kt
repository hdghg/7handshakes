package io.github.hdghg.sevenhandshakes.util

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.util.encoders.Base64
import java.security.SecureRandom

/**
 * Text encoding of Ed25519 keys used across the app:
 * - private key: Base64 of PKCS#8 DER ("MC4CAQAwBQYDK2VwBCIEI...")
 * - public key: Base64 of X.509 SubjectPublicKeyInfo DER ("MCowBQYDK2VwAyEA...")
 *
 * Private keys are also read from PEM and OpenSSH text; see [parsePrivateKey].
 * [canonicalPrivateKey] converts any of them to the format above.
 */
object Ed25519Keys {
    /** Ed25519 algorithm OID (RFC 8410). */
    private val ID_ED25519 = ASN1ObjectIdentifier("1.3.101.112")

    /** Generates a new private key, Base64 PKCS#8 encoded. */
    fun generatePrivateKey(): String {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        return encodePrivateKey(generator.generateKeyPair().private as Ed25519PrivateKeyParameters)
    }

    /**
     * Returns the Base64 X.509 public key for [privateKey],
     * or null if [privateKey] is not a valid Base64 PKCS#8 Ed25519 private key.
     */
    fun publicKeyOf(privateKey: String): String? {
        val params = parsePrivateKey(privateKey) ?: return null
        val publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(params.generatePublicKey())
        return Base64.toBase64String(publicKeyInfo.encoded)
    }

    /**
     * Re-encodes [privateKey] in the app's standard form, so one key always has one text value.
     * Returns null if [privateKey] is not a valid Base64 PKCS#8 Ed25519 private key.
     */
    fun canonicalPrivateKey(privateKey: String): String? = parsePrivateKey(privateKey)?.let(::encodePrivateKey)

    /**
     * True if [privateKey] is a passphrase-protected OpenSSH key (from `ssh-keygen -N <passphrase>`).
     * Such keys are not supported; the user has to remove the passphrase first.
     */
    fun isEncryptedOpenSshKey(privateKey: String): Boolean {
        val bytes = decodeBase64(stripPem(privateKey)) ?: return false
        if (!isOpenSshBlob(bytes)) return false
        // The cipher name ("none" when unencrypted) follows the magic as a length-prefixed string.
        val offset = OPENSSH_MAGIC.size
        if (bytes.size < offset + 4) return false
        val length = ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
        if (length < 0 || bytes.size < offset + 4 + length) return false
        return String(bytes, offset + 4, length, Charsets.US_ASCII) != "none"
    }

    /**
     * Accepts, ignoring whitespace and line breaks:
     * - Base64 PKCS#8, with or without the "-----BEGIN PRIVATE KEY-----" lines (`openssl genpkey`)
     * - unencrypted OpenSSH, with or without the "-----BEGIN OPENSSH PRIVATE KEY-----" lines (`ssh-keygen`)
     */
    private fun parsePrivateKey(privateKey: String): Ed25519PrivateKeyParameters? {
        val bytes = decodeBase64(stripPem(privateKey)) ?: return null
        val params = try {
            if (isOpenSshBlob(bytes)) {
                OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(bytes)
            } else {
                PrivateKeyFactory.createKey(bytes)
            }
        } catch (e: Exception) {
            return null
        }
        return params as? Ed25519PrivateKeyParameters
    }

    /** Removes "-----BEGIN ...-----" / "-----END ...-----" lines and all whitespace. */
    private fun stripPem(text: String): String = text
        .replace(Regex("-----(BEGIN|END) [A-Z ]+-----"), "")
        .filterNot { it.isWhitespace() }

    private fun decodeBase64(text: String): ByteArray? {
        if (text.isEmpty()) return null
        return try {
            Base64.decode(text)
        } catch (e: Exception) {
            null
        }
    }

    private val OPENSSH_MAGIC = "openssh-key-v1\u0000".toByteArray(Charsets.US_ASCII)

    private fun isOpenSshBlob(bytes: ByteArray): Boolean =
        bytes.size >= OPENSSH_MAGIC.size && OPENSSH_MAGIC.indices.all { bytes[it] == OPENSSH_MAGIC[it] }

    /**
     * Plain PKCS#8 v1 (RFC 8410), without the optional embedded public key
     * that PrivateKeyInfoFactory would add.
     */
    private fun encodePrivateKey(privateKey: Ed25519PrivateKeyParameters): String {
        val info = PrivateKeyInfo(
            AlgorithmIdentifier(ID_ED25519),
            DEROctetString(privateKey.encoded),
        )
        return Base64.toBase64String(info.encoded)
    }
}
