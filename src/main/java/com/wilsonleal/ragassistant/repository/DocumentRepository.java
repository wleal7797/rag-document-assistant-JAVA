package com.wilsonleal.ragassistant.repository;

import com.wilsonleal.ragassistant.model.DocumentInfo;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria de los documentos indexados. Es seguro para uso concurrente.
 * Los datos se pierden al reiniciar la aplicación (ver hoja de ruta en el README).
 */
@Repository
public class DocumentRepository {

    private final Map<String, DocumentInfo> documents = new ConcurrentHashMap<>();

    public DocumentInfo save(DocumentInfo document) {
        documents.put(document.id(), document);
        return document;
    }

    public Optional<DocumentInfo> findById(String id) {
        return Optional.ofNullable(id).map(documents::get);
    }

    /** Devuelve los documentos ordenados del más reciente al más antiguo. */
    public List<DocumentInfo> findAll() {
        return documents.values().stream()
                .sorted(Comparator.comparing(DocumentInfo::uploadedAt).reversed())
                .toList();
    }

    public Optional<DocumentInfo> deleteById(String id) {
        return Optional.ofNullable(id).map(documents::remove);
    }

    public int count() {
        return documents.size();
    }
}
