package com.wilsonleal.ragassistant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuración tipada del pipeline RAG, enlazada desde el prefijo {@code rag} de application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        @Valid @NotNull Ollama ollama,
        @Valid @NotNull Ingestion ingestion,
        @Valid @NotNull Retrieval retrieval) {

    /** Conexión y modelos de Ollama. */
    public record Ollama(
            @NotBlank String baseUrl,
            @NotBlank String chatModel,
            @NotBlank String embeddingModel,
            @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
            @NotNull Duration timeout) {
    }

    /** Parámetros de fragmentación e indexación de documentos. */
    public record Ingestion(
            @Min(200) int chunkSize,
            @Min(0) int chunkOverlap,
            @Min(1) int embeddingBatchSize) {
    }

    /** Parámetros de búsqueda semántica. */
    public record Retrieval(
            @Min(1) @Max(20) int maxResults,
            @DecimalMin("0.0") @DecimalMax("1.0") double minScore) {
    }
}
