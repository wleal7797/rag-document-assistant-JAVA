package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.config.RagProperties;
import com.wilsonleal.ragassistant.exception.AiServiceException;
import com.wilsonleal.ragassistant.exception.DocumentNotFoundException;
import com.wilsonleal.ragassistant.exception.DocumentProcessingException;
import com.wilsonleal.ragassistant.model.DocumentInfo;
import com.wilsonleal.ragassistant.model.PageText;
import com.wilsonleal.ragassistant.model.PdfContent;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Orquesta la indexación de documentos: validación, extracción de texto,
 * fragmentación, generación de embeddings y almacenamiento.
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final PdfTextExtractor pdfTextExtractor;
    private final DocumentSplitter documentSplitter;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final RagProperties properties;

    public DocumentIngestionService(PdfTextExtractor pdfTextExtractor,
                                    DocumentSplitter documentSplitter,
                                    EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore,
                                    DocumentRepository documentRepository,
                                    RagProperties properties) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.documentSplitter = documentSplitter;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
        this.properties = properties;
    }

    /**
     * Indexa un PDF y lo deja disponible para preguntas.
     *
     * @param originalFileName nombre del archivo tal como lo envió el usuario
     * @param content          bytes del archivo
     * @return información del documento indexado
     */
    public DocumentInfo ingest(String originalFileName, byte[] content) {
        String fileName = sanitizeFileName(originalFileName);
        validate(fileName, content);

        long start = System.currentTimeMillis();
        PdfContent pdf = pdfTextExtractor.extract(content);
        if (!pdf.hasText()) {
            throw new DocumentProcessingException(
                    "El PDF no tiene texto seleccionable. Si es un documento escaneado, primero aplícale OCR.");
        }

        String documentId = UUID.randomUUID().toString();
        List<TextSegment> segments = split(documentId, fileName, pdf.pages());
        List<String> embeddingIds = embedAndStore(segments);

        DocumentInfo document = new DocumentInfo(documentId, fileName, content.length,
                pdf.pageCount(), segments.size(), embeddingIds, Instant.now());
        documentRepository.save(document);

        log.info("Documento '{}' indexado: {} páginas, {} fragmentos, {} ms",
                fileName, pdf.pageCount(), segments.size(), System.currentTimeMillis() - start);
        return document;
    }

    /** Elimina un documento y todos sus fragmentos del almacén vectorial. */
    public DocumentInfo delete(String documentId) {
        DocumentInfo document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (!document.embeddingIds().isEmpty()) {
            embeddingStore.removeAll(document.embeddingIds());
        }
        documentRepository.deleteById(documentId);
        log.info("Documento '{}' eliminado", document.fileName());
        return document;
    }

    private List<TextSegment> split(String documentId, String fileName, List<PageText> pages) {
        List<TextSegment> segments = new ArrayList<>();
        for (PageText page : pages) {
            Metadata metadata = new Metadata()
                    .put(SegmentMetadata.DOCUMENT_ID, documentId)
                    .put(SegmentMetadata.FILE_NAME, fileName)
                    .put(SegmentMetadata.PAGE, page.pageNumber());
            segments.addAll(documentSplitter.split(Document.from(page.text(), metadata)));
        }
        return segments;
    }

    /** Genera embeddings por lotes; si algo falla, revierte lo que ya se había guardado. */
    private List<String> embedAndStore(List<TextSegment> segments) {
        int batchSize = properties.ingestion().embeddingBatchSize();
        List<String> storedIds = new ArrayList<>();
        try {
            for (int from = 0; from < segments.size(); from += batchSize) {
                List<TextSegment> batch = segments.subList(from, Math.min(from + batchSize, segments.size()));
                List<Embedding> embeddings = embeddingModel.embedAll(batch).content();
                storedIds.addAll(embeddingStore.addAll(embeddings, batch));
            }
            return storedIds;
        } catch (RuntimeException e) {
            if (!storedIds.isEmpty()) {
                embeddingStore.removeAll(storedIds);
            }
            throw new AiServiceException(
                    "No se pudieron generar los embeddings. Verifica que Ollama esté en ejecución en "
                            + properties.ollama().baseUrl() + " y que el modelo '"
                            + properties.ollama().embeddingModel() + "' esté descargado.", e);
        }
    }

    private void validate(String fileName, byte[] content) {
        if (content == null || content.length == 0) {
            throw new DocumentProcessingException("Selecciona un archivo PDF para subir.");
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new DocumentProcessingException("Solo se admiten archivos PDF.");
        }
        if (content.length < PDF_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(content, PDF_SIGNATURE.length), PDF_SIGNATURE)) {
            throw new DocumentProcessingException("El archivo tiene extensión .pdf, pero su contenido no es un PDF válido.");
        }
    }

    private static String sanitizeFileName(String originalFileName) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(
                originalFileName == null ? "" : originalFileName));
        return StringUtils.hasText(name) ? name : "documento.pdf";
    }
}
