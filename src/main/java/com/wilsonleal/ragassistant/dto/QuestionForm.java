package com.wilsonleal.ragassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Objeto de formulario para enviar una pregunta desde la vista de chat.
 */
public class QuestionForm {

    @NotBlank(message = "Escribe una pregunta.")
    @Size(max = 1000, message = "La pregunta no puede superar 1000 caracteres.")
    private String question;

    /** Documento sobre el que se pregunta; vacío significa todos los documentos. */
    private String documentId;

    public QuestionForm() {
    }

    public QuestionForm(String question, String documentId) {
        this.question = question;
        this.documentId = documentId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
