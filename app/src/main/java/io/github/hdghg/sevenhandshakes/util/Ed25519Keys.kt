package io.github.hdghg.sevenhandshakes.util

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory
import org.bouncycastle.util.encoders.Base64
import java.security.SecureRandom

/**
 * Text encoding of Ed25519 keys used across the app:
 * - private key: Base64 of PKCS#8 DER ("MC4CAQAwBQYDK2VwBCIEI...")
 * - public key: Base64 of X.509 SubjectPublicKeyInfo DER ("MCowBQYDK2VwAyEA...")
 */
object Ed25519Keys {
    /** Ed25519 algorithm OID (RFC 8410). */
    private val ID_ED25519 = ASN1ObjectIdentifier("1.3.101.112")

    /** Generates a new private key, Base64 PKCS#8 encoded. */
    fun generatePrivateKey(): String {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val privateKey = generator.generateKeyPair().private as Ed25519PrivateKeyParameters
        // Plain PKCS#8 v1 (RFC 8410), without the optional embedded public key
        // that PrivateKeyInfoFactory would add.
        val info = PrivateKeyInfo(
            AlgorithmIdentifier(ID_ED25519),
            DEROctetString(privateKey.encoded),
        )
        return Base64.toBase64String(info.encoded)
    }

    /**
     * Returns the Base64 X.509 public key for [privateKey],
     * or null if [privateKey] is not a valid Base64 PKCS#8 Ed25519 private key.
     */
    fun publicKeyOf(privateKey: String): String? {
        val trimmed = privateKey.trim()
        if (trimmed.isEmpty()) return null
        val params = try {
            PrivateKeyFactory.createKey(Base64.decode(trimmed))
        } catch (e: Exception) {
            return null
        }
        if (params !is Ed25519PrivateKeyParameters) return null
        val publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(params.generatePublicKey())
        return Base64.toBase64String(publicKeyInfo.encoded)
    }
}
