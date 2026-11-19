package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.example.dto.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserFormController {

    private final ReflectionFormFieldMetadataBuilder metadataBuilder;

    public UserFormController(ReflectionFormFieldMetadataBuilder metadataBuilder) {
        this.metadataBuilder = metadataBuilder;
    }

    @GetMapping("/user-form")
    public String showForm(Model model) {
        model.addAttribute("fields", metadataBuilder.buildMetadata(User.class));
        model.addAttribute("formName", "User");
        model.addAttribute("formData", new User());
        return "bootstrap/forms/vertical/form-base";
    }
}
