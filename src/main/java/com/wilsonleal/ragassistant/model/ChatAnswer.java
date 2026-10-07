package com.wilsonleal.ragassistant.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Pregunta del usuario y respuesta generada, con sus fuentes.
 *
 * @param question      pregunta original
 * @param answer        respuesta del asistente
 * @param sources       fragmentos usados como contexto
 * @param scopeLabel    alcance de la búsqueda (un documento o todos)
 * @param elapsedMillis tiempo total de respuesta
 * @param askedAt       momento de la pregunta
 */
public record ChatAnswer(
        String question,
        String answer,
        List<SourceReference> sources,
        String scopeLabel,
        long elapsedMillis,
        Instant askedAt) {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", SPANISH).withZone(ZoneId.systemDefault());

    public ChatAnswer {
        sources = List.copyOf(sources);
    }

    public boolean hasSources() {
        return !sources.isEmpty();
    }

    /** Tiempo de respuesta legible, por ejemplo "3,2 s". */
    public String elapsedLabel() {
        return String.format(SPANISH, "%.1f s", elapsedMillis / 1000.0);
    }

    public String askedAtLabel() {
        return TIME_FORMAT.format(askedAt);
    }
}
