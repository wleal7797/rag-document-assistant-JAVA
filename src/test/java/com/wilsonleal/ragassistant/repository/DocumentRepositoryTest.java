package com.wilsonleal.ragassistant.repository;

import com.wilsonleal.ragassistant.model.DocumentInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentRepositoryTest {

    private final DocumentRepository repository = new DocumentRepository();

    @Test
    void findAllReturnsMostRecentFirst() {
        repository.save(document("a", Instant.parse("2026-01-01T10:00:00Z")));
        repository.save(document("b", Instant.parse("2026-03-01T10:00:00Z")));

        assertThat(repository.findAll()).extracting(DocumentInfo::id).containsExactly("b", "a");
    }

    @Test
    void deleteRemovesTheDocument() {
        repository.save(document("a", Instant.now()));

        assertThat(repository.deleteById("a")).isPresent();
        assertThat(repository.findById("a")).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    void nullIdsAreHandledSafely() {
        assertThat(repository.findById(null)).isEmpty();
        assertThat(repository.deleteById(null)).isEmpty();
    }

    private static DocumentInfo document(String id, Instant uploadedAt) {
        return new DocumentInfo(id, id + ".pdf", 2048, 3, 5, List.of("e1"), uploadedAt);
    }
}
