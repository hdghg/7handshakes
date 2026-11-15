package io.github.hdghg.sevenhandshakes.data.share

import org.bouncycastle.util.encoders.Base64
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/**
 * What the app's links have in common. A link is plain text that can be sent in a message
 * or shown as a QR code, and holds everything it is about:
 *
 * ```
 * hs7://<host>/<payload>
 * ```
 *
 * The host tells what the link holds; see [ReviewLink] and [ContactLink].
 * The payload is URL-safe Base64 without padding.
 */
object Hs7Link {
    /** Must match the scheme of the link intent filter in the manifest. */
    const val SCHEME = "hs7"

    internal fun of(host: String, payload: ByteArray): String =
        prefixOf(host) + Base64.toBase64String(payload).trimEnd('=').replace('+', '-').replace('/', '_')

    /** True if [text] is a link with this [host], whatever its payload. Whitespace around it is ignored. */
    internal fun hasHost(text: String, host: String): Boolean = text.trim().startsWith(prefixOf(host))

    /** The payload bytes of [link], or null if it is not a link with this [host] or its payload is not Base64. */
    internal fun payloadOf(link: String, host: String): ByteArray? {
        val trimmed = link.trim()
        val prefix = prefixOf(host)
        if (!trimmed.startsWith(prefix)) return null
        val payload = trimmed.substring(prefix.length).replace('-', '+').replace('_', '/')
        return try {
            Base64.decode(payload + "=".repeat((4 - payload.length % 4) % 4))
        } catch (e: RuntimeException) {
            null
        }
    }

    /** The rest of [buffer] as text, or null if it is not UTF-8: wrong bytes are refused rather than replaced. */
    internal fun utf8(buffer: ByteBuffer): String? = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(buffer)
            .toString()
    } catch (e: CharacterCodingException) {
        null
    }

    private fun prefixOf(host: String) = "$SCHEME://$host/"
}
