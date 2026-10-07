package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.exception.DocumentProcessingException;
import com.wilsonleal.ragassistant.model.PageText;
import com.wilsonleal.ragassistant.model.PdfContent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrae el texto de un PDF página por página con Apache PDFBox. Conservar el número
 * de página permite que el asistente cite la ubicación exacta de cada fuente.
 */
@Component
public class PdfTextExtractor {

    public PdfContent extract(byte[] content) {
        try (PDDocument pdf = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int pageCount = pdf.getNumberOfPages();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = normalize(stripper.getText(pdf));
                if (!text.isBlank()) {
                    pages.add(new PageText(page, text));
                }
            }
            return new PdfContent(pageCount, pages);
        } catch (InvalidPasswordException e) {
            throw new DocumentProcessingException("El PDF está protegido con contraseña. Quita la protección e inténtalo de nuevo.", e);
        } catch (IOException e) {
            throw new DocumentProcessingException("No se pudo leer el PDF. Verifica que el archivo no esté dañado.", e);
        }
    }

    /** Limpia saltos de línea y espacios redundantes, y une palabras cortadas con guion al final de línea. */
    static String normalize(String raw) {
        return raw
                .replace("\r", "")
                .replaceAll("(\\p{L})-\\n(\\p{L})", "$1$2")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }
}
