package com.wilsonleal.ragassistant.controller;

import com.wilsonleal.ragassistant.exception.AiServiceException;
import com.wilsonleal.ragassistant.exception.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Convierte las excepciones conocidas en mensajes claros para el usuario.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "El archivo supera el tamaño máximo permitido de 20 MB.");
        return "redirect:/documents";
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public String handleDocumentNotFound(DocumentNotFoundException e, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/documents";
    }

    @ExceptionHandler(AiServiceException.class)
    public String handleAiService(AiServiceException e, RedirectAttributes redirectAttributes) {
        log.error("Error al comunicarse con Ollama", e);
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/chat";
    }
}
