package com.wilsonleal.ragassistant.exception;

/**
 * El documento solicitado no existe o ya fue eliminado.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String documentId) {
        super("El documento solicitado no existe o ya fue eliminado (id: " + documentId + ").");
    }
}
