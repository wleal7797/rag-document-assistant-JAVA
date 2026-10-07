package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.config.RagProperties;
import com.wilsonleal.ragassistant.model.ChatAnswer;
import com.wilsonleal.ragassistant.model.DocumentInfo;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionAnsweringServiceTest {

    private EmbeddingModel embeddingModel;
    private ChatModel chatModel;
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private DocumentRepository documentRepository;
    private QuestionAnsweringService service;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        chatModel = mock(ChatModel.class);
        embeddingStore = new InMemoryEmbeddingStore<>();
        documentRepository = new DocumentRepository();
        RagProperties properties = new RagProperties(
                new RagProperties.Ollama("http://localhost:11434", "llama3.2", "nomic-embed-text", 0.2, Duration.ofSeconds(30)),
                new RagProperties.Ingestion(800, 120, 32),
                new RagProperties.Retrieval(5, 0.6));
        service = new QuestionAnsweringService(embeddingModel, embeddingStore, chatModel,
                new PromptBuilder(), documentRepository, properties);
    }

    @Test
    void withoutDocumentsReturnsGuidanceAndDoesNotCallTheModels() {
        ChatAnswer answer = service.answer("¿Qué es esto?", null);

        assertThat(answer.answer()).isEqualTo(QuestionAnsweringService.NO_DOCUMENTS_MESSAGE);
        assertThat(answer.hasSources()).isFalse();
        verify(embeddingModel, never()).embed(anyString());
        verify(chatModel, never()).chat(anyList());
    }

    @Test
    void answersWithSourcesWhenRelevantContextExists() {
        indexSegment("doc-1", "manual-bomba.pdf", 4, "La presión máxima de trabajo es 10 bar.", new float[]{1f, 0f, 0f});
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{1f, 0f, 0f})));
        when(chatModel.chat(anyList())).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("La presión máxima es 10 bar [1]."))
                .build());

        ChatAnswer answer = service.answer("¿Cuál es la presión máxima?", null);

        assertThat(answer.answer()).isEqualTo("La presión máxima es 10 bar [1].");
        assertThat(answer.sources()).singleElement().satisfies(source -> {
            assertThat(source.fileName()).isEqualTo("manual-bomba.pdf");
            assertThat(source.page()).isEqualTo(4);
        });
        assertThat(answer.scopeLabel()).isEqualTo(QuestionAnsweringService.ALL_DOCUMENTS_LABEL);
    }

    @Test
    void withoutRelevantContextDoesNotCallTheLlm() {
        indexSegment("doc-1", "manual-bomba.pdf", 1, "Texto sin relación.", new float[]{1f, 0f, 0f});
        // Vector ortogonal: similitud baja, por debajo del puntaje mínimo.
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{0f, 1f, 0f})));

        ChatAnswer answer = service.answer("¿Cuál es el horario del casino?", null);

        assertThat(answer.answer()).isEqualTo(QuestionAnsweringService.NO_CONTEXT_MESSAGE);
        verify(chatModel, never()).chat(anyList());
    }

    @Test
    void filtersByDocumentWhenOneIsSelected() {
        indexSegment("doc-1", "manual-bomba.pdf", 1, "Bomba: 10 bar.", new float[]{1f, 0f, 0f});
        indexSegment("doc-2", "ficha-motor.pdf", 7, "Motor: 40 °C.", new float[]{1f, 0f, 0f});
        when(embeddingModel.embed(anyString())).thenReturn(Response.from(Embedding.from(new float[]{1f, 0f, 0f})));
        when(chatModel.chat(anyList())).thenReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("El motor opera a 40 °C [1]."))
                .build());

        ChatAnswer answer = service.answer("¿A qué temperatura opera?", "doc-2");

        assertThat(answer.sources()).extracting(source -> source.fileName()).containsOnly("ficha-motor.pdf");
        assertThat(answer.scopeLabel()).isEqualTo("ficha-motor.pdf");
    }

    private void indexSegment(String documentId, String fileName, int page, String text, float[] vector) {
        Metadata metadata = new Metadata()
                .put(SegmentMetadata.DOCUMENT_ID, documentId)
                .put(SegmentMetadata.FILE_NAME, fileName)
                .put(SegmentMetadata.PAGE, page);
        String embeddingId = embeddingStore.add(Embedding.from(vector), TextSegment.from(text, metadata));
        documentRepository.save(new DocumentInfo(documentId, fileName, 1024, page, 1,
                List.of(embeddingId), Instant.now()));
    }
}
