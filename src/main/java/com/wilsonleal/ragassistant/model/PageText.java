package com.wilsonleal.ragassistant.model;

/**
 * Texto extraído de una página concreta de un PDF.
 *
 * @param pageNumber número de página, empezando en 1
 * @param text       texto normalizado de la página
 */
public record PageText(int pageNumber, String text) {
}
