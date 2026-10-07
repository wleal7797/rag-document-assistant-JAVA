package com.wilsonleal.ragassistant.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Documento indexado en el asistente.
 *
 * @param id           identificador único
 * @param fileName     nombre original del archivo
 * @param sizeBytes    tamaño del archivo en bytes
 * @param pageCount    total de páginas del PDF
 * @param chunkCount   cantidad de fragmentos indexados
 * @param embeddingIds identificadores de los embeddings en el almacén vectorial
 * @param uploadedAt   momento de la carga
 */
public record DocumentInfo(
        String id,
        String fileName,
        long sizeBytes,
        int pageCount,
        int chunkCount,
        List<String> embeddingIds,
        Instant uploadedAt) {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", SPANISH).withZone(ZoneId.systemDefault());

    public DocumentInfo {
        embeddingIds = List.copyOf(embeddingIds);
    }

    /** Tamaño legible, por ejemplo "2,4 MB". */
    public String formattedSize() {
        if (sizeBytes < 1024 * 1024) {
            return String.format(SPANISH, "%.0f KB", Math.max(1, sizeBytes / 1024.0));
        }
        return String.format(SPANISH, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }

    /** Fecha de carga en formato local. */
    public String uploadedAtLabel() {
        return DATE_FORMAT.format(uploadedAt);
    }
}
