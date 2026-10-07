package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.config.RagProperties;
import com.wilsonleal.ragassistant.exception.AiServiceException;
import com.wilsonleal.ragassistant.exception.DocumentNotFoundException;
import com.wilsonleal.ragassistant.model.ChatAnswer;
import com.wilsonleal.ragassistant.model.DocumentInfo;
import com.wilsonleal.ragassistant.model.RetrievedChunk;
import com.wilsonleal.ragassistant.model.SourceReference;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Responde preguntas con el patrón RAG:
 * 1) genera el embedding de la pregunta, 2) recupera los fragmentos más similares,
 * 3) construye el prompt con ese contexto y 4) genera la respuesta con el LLM.
 */
@Service
public class QuestionAnsweringService {

    static final String ALL_DOCUMENTS_LABEL = "Todos los documentos";
    static final String NO_DOCUMENTS_MESSAGE =
            "Todavía no hay documentos cargados. Sube al menos un PDF en la sección Documentos para empezar a preguntar.";
    static final String NO_CONTEXT_MESSAGE =
            "No encontré información relacionada con tu pregunta en los documentos consultados. "
                    + "Prueba a reformularla con otros términos o a buscar en todos los documentos.";

    private static final Logger log = LoggerFactory.getLogger(QuestionAnsweringService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final PromptBuilder promptBuilder;
    private final DocumentRepository documentRepository;
    private final RagProperties properties;

    public QuestionAnsweringService(EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore,
                                    ChatModel chatModel,
                                    PromptBuilder promptBuilder,
                                    DocumentRepository documentRepository,
                                    RagProperties properties) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.promptBuilder = promptBuilder;
        this.documentRepository = documentRepository;
        this.properties = properties;
    }

    /**
     * Responde una pregunta sobre un documento concreto o sobre todos.
     *
     * @param question   pregunta en lenguaje natural
     * @param documentId documento a consultar; nulo o vacío para consultar todos
     */
    public ChatAnswer answer(String question, String documentId) {
        long start = System.currentTimeMillis();
        String cleanQuestion = question.strip();

        Filter filter = null;
        String scopeLabel = ALL_DOCUMENTS_LABEL;
        if (StringUtils.hasText(documentId)) {
            DocumentInfo document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));
            filter = metadataKey(SegmentMetadata.DOCUMENT_ID).isEqualTo(documentId);
            scopeLabel = document.fileName();
        }

        if (documentRepository.count() == 0) {
            return buildAnswer(cleanQuestion, NO_DOCUMENTS_MESSAGE, List.of(), scopeLabel, start);
        }

        List<RetrievedChunk> chunks = retrieve(cleanQuestion, filter);
        if (chunks.isEmpty()) {
            // Sin contexto relevante no se llama al LLM: así se evitan respuestas inventadas.
            return buildAnswer(cleanQuestion, NO_CONTEXT_MESSAGE, List.of(), scopeLabel, start);
        }

        String answer = generate(cleanQuestion, chunks);
        List<SourceReference> sources = chunks.stream().map(RetrievedChunk::toSourceReference).toList();
        ChatAnswer chatAnswer = buildAnswer(cleanQuestion, answer, sources, scopeLabel, start);
        log.info("Pregunta respondida con {} fuentes en {} ms", sources.size(), chatAnswer.elapsedMillis());
        return chatAnswer;
    }

    private List<RetrievedChunk> retrieve(String question, Filter filter) {
        Embedding questionEmbedding;
        try {
            questionEmbedding = embeddingModel.embed(question).content();
        } catch (RuntimeException e) {
            throw unavailable(properties.ollama().embeddingModel(), e);
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(properties.retrieval().maxResults())
                .minScore(properties.retrieval().minScore())
                .filter(filter)
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(QuestionAnsweringService::toChunk)
                .toList();
    }

    private String generate(String question, List<RetrievedChunk> chunks) {
        List<ChatMessage> messages = List.of(
                SystemMessage.from(promptBuilder.systemPrompt()),
                UserMessage.from(promptBuilder.userPrompt(question, chunks)));
        try {
            return chatModel.chat(messages).aiMessage().text().strip();
        } catch (RuntimeException e) {
            throw unavailable(properties.ollama().chatModel(), e);
        }
    }

    private static RetrievedChunk toChunk(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();
        Integer page = metadata.getInteger(SegmentMetadata.PAGE);
        return new RetrievedChunk(
                metadata.getString(SegmentMetadata.DOCUMENT_ID),
                metadata.getString(SegmentMetadata.FILE_NAME),
                page == null ? 0 : page,
                segment.text(),
                match.score());
    }

    private AiServiceException unavailable(String modelName, RuntimeException cause) {
        return new AiServiceException(
                "No se pudo obtener respuesta de Ollama en " + properties.ollama().baseUrl()
                        + ". Verifica que el servicio esté en ejecución y que el modelo '" + modelName
                        + "' esté descargado (ollama pull " + modelName + ").", cause);
    }

    private static ChatAnswer buildAnswer(String question, String answer, List<SourceReference> sources,
                                          String scopeLabel, long start) {
        return new ChatAnswer(question, answer, sources, scopeLabel,
                System.currentTimeMillis() - start, Instant.now());
    }
}
