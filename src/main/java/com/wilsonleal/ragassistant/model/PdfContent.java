package com.wilsonleal.ragassistant.model;

import java.util.List;

/**
 * Resultado de la extracción de un PDF.
 *
 * @param pageCount total de páginas del archivo
 * @param pages     páginas que contienen texto (las vacías se omiten)
 */
public record PdfContent(int pageCount, List<PageText> pages) {

    public PdfContent {
        pages = List.copyOf(pages);
    }

    public boolean hasText() {
        return !pages.isEmpty();
    }
}
