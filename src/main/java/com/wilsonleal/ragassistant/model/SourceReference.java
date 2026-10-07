package com.wilsonleal.ragassistant.model;

/**
 * Fuente citada en una respuesta.
 *
 * @param documentId identificador del documento
 * @param fileName   nombre del archivo
 * @param page       página de origen
 * @param excerpt    extracto resumido del fragmento
 * @param score      relevancia entre 0 y 1
 */
public record SourceReference(String documentId, String fileName, int page, String excerpt, double score) {

    /** Relevancia expresada como porcentaje entero. */
    public int scorePercent() {
        return (int) Math.round(score * 100);
    }
}
