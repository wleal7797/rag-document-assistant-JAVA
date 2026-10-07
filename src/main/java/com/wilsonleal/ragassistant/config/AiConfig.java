package com.wilsonleal.ragassistant.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Define los componentes de IA como beans de Spring. Al depender de interfaces
 * (ChatModel, EmbeddingModel, EmbeddingStore), cambiar de proveedor o de almacén
 * vectorial solo requiere modificar esta clase.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatModel chatModel(RagProperties properties) {
        RagProperties.Ollama ollama = properties.ollama();
        return OllamaChatModel.builder()
                .baseUrl(ollama.baseUrl())
                .modelName(ollama.chatModel())
                .temperature(ollama.temperature())
                .timeout(ollama.timeout())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(RagProperties properties) {
        RagProperties.Ollama ollama = properties.ollama();
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollama.baseUrl())
                .modelName(ollama.embeddingModel())
                .timeout(ollama.timeout())
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public DocumentSplitter documentSplitter(RagProperties properties) {
        return DocumentSplitters.recursive(
                properties.ingestion().chunkSize(),
                properties.ingestion().chunkOverlap());
    }
}
