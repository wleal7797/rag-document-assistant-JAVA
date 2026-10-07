package com.wilsonleal.ragassistant.model;

/**
 * Fragmento recuperado por la búsqueda semántica, con el texto completo que se envía al LLM.
 *
 * @param documentId identificador del documento de origen
 * @param fileName   nombre del archivo de origen
 * @param page       página de origen
 * @param text       texto completo del fragmento
 * @param score      relevancia entre 0 y 1
 */
public record RetrievedChunk(String documentId, String fileName, int page, String text, double score) {

    private static final int EXCERPT_LENGTH = 280;

    /** Convierte el fragmento en una referencia apta para mostrar en la vista. */
    public SourceReference toSourceReference() {
        return new SourceReference(documentId, fileName, page, excerpt(), score);
    }

    private String excerpt() {
        String clean = text.replaceAll("\\s+", " ").strip();
        if (clean.length() <= EXCERPT_LENGTH) {
            return clean;
        }
        int cut = clean.lastIndexOf(' ', EXCERPT_LENGTH);
        return clean.substring(0, cut > 0 ? cut : EXCERPT_LENGTH) + "…";
    }
}
