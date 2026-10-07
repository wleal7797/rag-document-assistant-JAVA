package com.wilsonleal.ragassistant.controller;

import com.wilsonleal.ragassistant.exception.AiServiceException;
import com.wilsonleal.ragassistant.exception.DocumentProcessingException;
import com.wilsonleal.ragassistant.model.DocumentInfo;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import com.wilsonleal.ragassistant.service.DocumentIngestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * Controlador de la biblioteca de documentos: listar, subir y eliminar PDFs.
 */
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentRepository documentRepository;

    public DocumentController(DocumentIngestionService ingestionService, DocumentRepository documentRepository) {
        this.ingestionService = ingestionService;
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("documents", documentRepository.findAll());
        return "documents";
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            DocumentInfo document = ingestionService.ingest(file.getOriginalFilename(), file.getBytes());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Documento listo: " + document.fileName() + " (" + document.pageCount() + " páginas, "
                            + document.chunkCount() + " fragmentos).");
        } catch (DocumentProcessingException | AiServiceException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo leer el archivo enviado. Inténtalo de nuevo.");
        }
        return "redirect:/documents";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        DocumentInfo deleted = ingestionService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Documento eliminado: " + deleted.fileName() + ".");
        return "redirect:/documents";
    }
}
