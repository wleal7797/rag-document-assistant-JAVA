package com.wilsonleal.ragassistant.controller;

import com.wilsonleal.ragassistant.dto.QuestionForm;
import com.wilsonleal.ragassistant.model.ChatAnswer;
import com.wilsonleal.ragassistant.repository.DocumentRepository;
import com.wilsonleal.ragassistant.service.QuestionAnsweringService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la conversación. El historial se guarda en la sesión HTTP de cada usuario.
 */
@Controller
public class ChatController {

    static final String HISTORY_ATTRIBUTE = "chatHistory";
    private static final int MAX_HISTORY = 20;

    private final QuestionAnsweringService questionAnsweringService;
    private final DocumentRepository documentRepository;

    public ChatController(QuestionAnsweringService questionAnsweringService, DocumentRepository documentRepository) {
        this.questionAnsweringService = questionAnsweringService;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/chat";
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(name = "documento", required = false) String documentId,
                       Model model, HttpSession session) {
        model.addAttribute("questionForm", new QuestionForm(null, documentId));
        populate(model, session);
        return "chat";
    }

    @PostMapping("/chat")
    public String ask(@Valid @ModelAttribute("questionForm") QuestionForm questionForm, BindingResult bindingResult,
                      Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populate(model, session);
            return "chat";
        }

        ChatAnswer answer = questionAnsweringService.answer(questionForm.getQuestion(), questionForm.getDocumentId());
        List<ChatAnswer> history = history(session);
        history.add(answer);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

        if (StringUtils.hasText(questionForm.getDocumentId())) {
            redirectAttributes.addAttribute("documento", questionForm.getDocumentId());
        }
        return "redirect:/chat#respuesta-reciente";
    }

    @PostMapping("/chat/clear")
    public String clear(HttpSession session) {
        session.removeAttribute(HISTORY_ATTRIBUTE);
        return "redirect:/chat";
    }

    private void populate(Model model, HttpSession session) {
        model.addAttribute("documents", documentRepository.findAll());
        model.addAttribute("history", history(session));
    }

    @SuppressWarnings("unchecked")
    private static List<ChatAnswer> history(HttpSession session) {
        Object history = session.getAttribute(HISTORY_ATTRIBUTE);
        if (history instanceof List<?> list) {
            return (List<ChatAnswer>) list;
        }
        List<ChatAnswer> newHistory = new ArrayList<>();
        session.setAttribute(HISTORY_ATTRIBUTE, newHistory);
        return newHistory;
    }
}
