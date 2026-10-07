package com.wilsonleal.ragassistant.exception;

/**
 * Falla de comunicación con el proveedor de IA (Ollama). El mensaje se muestra al usuario.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
