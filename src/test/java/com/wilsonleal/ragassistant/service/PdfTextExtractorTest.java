package com.wilsonleal.ragassistant.service;

import com.wilsonleal.ragassistant.exception.DocumentProcessingException;
import com.wilsonleal.ragassistant.model.PdfContent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractsTextPageByPageAndSkipsEmptyPages() throws IOException {
        byte[] pdf = createPdf("Manual de operacion", null, "Procedimiento de arranque");

        PdfContent content = extractor.extract(pdf);

        assertThat(content.pageCount()).isEqualTo(3);
        assertThat(content.pages()).hasSize(2);
        assertThat(content.pages().get(0).pageNumber()).isEqualTo(1);
        assertThat(content.pages().get(1).pageNumber()).isEqualTo(3);
        assertThat(content.pages().get(1).text()).contains("Procedimiento de arranque");
    }

    @Test
    void rejectsCorruptedFiles() {
        assertThatThrownBy(() -> extractor.extract("%PDF-esto no es un pdf".getBytes()))
                .isInstanceOf(DocumentProcessingException.class);
    }

    @Test
    void normalizeJoinsHyphenatedWordsAndCollapsesSpaces() {
        assertThat(PdfTextExtractor.normalize("tempe-\nratura   de\r\n\n\n\noperación  "))
                .isEqualTo("temperatura de\n\noperación");
    }

    /** Crea un PDF en memoria; una entrada nula genera una página en blanco. */
    private static byte[] createPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text != null) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(font, 12);
                        stream.newLineAtOffset(72, 700);
                        stream.showText(text);
                        stream.endText();
                    }
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
