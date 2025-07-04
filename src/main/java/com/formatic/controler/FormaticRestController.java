package com.formatic.controler;

import com.formatic.dto.Editor2;
import com.formatic.service.EditorService;

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
    public ResponseEntity<Editor2> getEditorById(@PathVariable Long id) {
        Optional<Editor2> editor = editorService.getEditorById(id);

        if (editor.isPresent()) {
            return ResponseEntity.ok(editor.get()); // Retourne l'éditeur trouvé avec un statut HTTP 200 OK
        } else {
            // Si l'éditeur n'est pas trouvé, retourne un statut HTTP 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }




}
