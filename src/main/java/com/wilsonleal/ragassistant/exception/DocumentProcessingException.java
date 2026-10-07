package com.wilsonleal.ragassistant.exception;

/**
 * Error al validar, leer o indexar un documento. El mensaje se muestra al usuario.
 */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
