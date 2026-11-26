package io.github.aedev.flow.utils

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Base64 from YouTube's protobuf parameters, which arrive without padding. The stdlib decoder
 * requires the padding, so it is restored here; unreadable input answers null.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun String.decodeBase64Lenient(): ByteArray? = runCatching { Base64.decode(padded()) }.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
internal fun String.decodeBase64UrlLenient(): ByteArray? = runCatching { Base64.UrlSafe.decode(padded()) }.getOrNull()

@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.encodeBase64(): String = Base64.Default.encode(this)

/** Base64url without padding, the form the sequence-seed `params` token must take. */
@OptIn(ExperimentalEncodingApi::class)
internal fun ByteArray.encodeBase64UrlNoPadding(): String = Base64.UrlSafe.encode(this).trimEnd('=')

private fun String.padded(): String =
    when (length % 4) {
        2 -> "$this=="
        3 -> "$this="
        else -> this
    }

/** Latin-1 view of the bytes, for scanning protobuf payloads for ASCII identifiers. */
internal fun ByteArray.toIso88591(): String = buildString(size) { for (byte in this@toIso88591) append((byte.toInt() and 0xFF).toChar()) }
