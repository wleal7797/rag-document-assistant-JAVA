package com.wilsonleal.ragassistant.controller;

import com.wilsonleal.ragassistant.exception.DocumentProcessingException;
import com.wilsonleal.ragassistant.model.DocumentInfo;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import com.wilsonleal.ragassistant.service.DocumentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentIngestionService ingestionService;

    @MockitoBean
    private DocumentRepository documentRepository;

    @Test
    void listShowsTheDocumentsView() throws Exception {
        when(documentRepository.findAll()).thenReturn(List.of(
                new DocumentInfo("1", "manual.pdf", 2048, 3, 6, List.of("e1"), Instant.now())));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attributeExists("documents"));
    }

    @Test
    void successfulUploadRedirectsWithConfirmation() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", "%PDF-1.7".getBytes());
        when(ingestionService.ingest(anyString(), any())).thenReturn(
                new DocumentInfo("1", "manual.pdf", 8, 3, 6, List.of("e1"), Instant.now()));

        mockMvc.perform(multipart("/documents").file(file))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void failedUploadRedirectsWithError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "notas.txt", "text/plain", "hola".getBytes());
        when(ingestionService.ingest(anyString(), any()))
                .thenThrow(new DocumentProcessingException("Solo se admiten archivos PDF."));

        mockMvc.perform(multipart("/documents").file(file))
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attribute("errorMessage", "Solo se admiten archivos PDF."));
    }
}
