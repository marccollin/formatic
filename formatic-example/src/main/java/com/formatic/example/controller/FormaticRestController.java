package com.formatic.example.controller;

import com.formatic.example.dto.Editor;
import com.formatic.example.service.EditorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RequestMapping("/api")
@RestController
public class FormaticRestController {

    private final EditorService editorService;

    public FormaticRestController(EditorService editorService) {
        this.editorService = editorService;
    }

    @GetMapping("/editors/{id}")
    public ResponseEntity<Editor> getEditorById(@PathVariable Long id) {
        Optional<Editor> editor = editorService.getEditorById(id);

        return editor.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
