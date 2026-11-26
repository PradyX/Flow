package io.github.aedev.flow.innertube.pages

import io.github.aedev.flow.player.sabr.proto.ProtobufReader
import io.github.aedev.flow.utils.decodeBase64Lenient
import io.ktor.http.decodeURLPart

/**
 * Reads which release shelf an artist's discography link opens. The link's params are a protobuf
 * whose field 48 holds field 15 holding field 1: 1 for albums, 2 for singles and EPs. This is the
 * only signal that survives every interface language.
 */
internal object ArtistDiscographyParams {
    private const val OUTER_FIELD = 48
    private const val FILTER_FIELD = 15
    private const val TYPE_FIELD = 1
    private const val ALBUMS = 1L
    private const val SINGLES = 2L

    fun releaseKind(params: String?): ArtistSectionKind? {
        if (params.isNullOrBlank()) return null
        val type =
            runCatching {
                val base64 = params.decodeURLPart().replace('-', '+').replace('_', '/')
                ProtobufReader(requireNotNull(base64.decodeBase64Lenient()))
                    .messageField(OUTER_FIELD)
                    ?.messageField(FILTER_FIELD)
                    ?.varintField(TYPE_FIELD)
            }.getOrNull()
        return when (type) {
            ALBUMS -> ArtistSectionKind.ALBUMS
            SINGLES -> ArtistSectionKind.SINGLES
            else -> null
        }
    }

    private fun ProtobufReader.messageField(number: Int): ProtobufReader? =
        readAllFields()[number]
            ?.firstOrNull { it.wireType == ProtobufReader.WIRE_LENGTH_DELIMITED }
            ?.asMessage()

    private fun ProtobufReader.varintField(number: Int): Long? =
        readAllFields()[number]
            ?.firstOrNull { it.wireType == ProtobufReader.WIRE_VARINT }
            ?.asLong()
}
