package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.model.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Construye los mensajes que se envían al LLM. Separar el prompt del servicio
 * permite probarlo y ajustarlo sin tocar la lógica de recuperación.
 */
@Component
public class PromptBuilder {

    static final String SYSTEM_PROMPT = """
            Eres un asistente técnico que responde preguntas sobre manuales y fichas técnicas.
            Reglas:
            1. Responde siempre en español, de forma clara, precisa y concisa.
            2. Usa únicamente la información de los fragmentos de contexto.
            3. Cita las fuentes con su número entre corchetes, por ejemplo [1] o [2][3],
               justo después de la afirmación que respaldan.
            4. Si el contexto no contiene la respuesta, dilo explícitamente y no inventes datos.
            5. Si la respuesta incluye pasos o procedimientos, preséntalos en orden.
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder("Contexto:\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            prompt.append('[').append(i + 1).append("] (")
                    .append(chunk.fileName()).append(", página ").append(chunk.page()).append(")\n")
                    .append(chunk.text().strip()).append("\n\n");
        }
        prompt.append("Pregunta: ").append(question.strip());
        return prompt.toString();
    }
}
