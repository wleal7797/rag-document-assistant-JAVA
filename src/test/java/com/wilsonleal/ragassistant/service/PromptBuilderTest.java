package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.model.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void userPromptNumbersSourcesAndIncludesFileAndPage() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk("d1", "manual-bomba.pdf", 4, "Presión máxima: 10 bar.", 0.91),
                new RetrievedChunk("d2", "ficha-motor.pdf", 2, "Temperatura de operación: 40 °C.", 0.77));

        String prompt = promptBuilder.userPrompt("  ¿Cuál es la presión máxima?  ", chunks);

        assertThat(prompt)
                .contains("[1] (manual-bomba.pdf, página 4)")
                .contains("Presión máxima: 10 bar.")
                .contains("[2] (ficha-motor.pdf, página 2)")
                .endsWith("Pregunta: ¿Cuál es la presión máxima?");
    }

    @Test
    void systemPromptForbidsInventingData() {
        assertThat(promptBuilder.systemPrompt())
                .contains("Responde siempre en español")
                .contains("no inventes datos");
    }
}
