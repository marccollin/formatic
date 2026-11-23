package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.example.dto.User;
import com.formatic.spring.render.SpringFormFieldLabels;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Locale;

@Controller
public class UserFormController {

    private final ReflectionFormFieldMetadataBuilder metadataBuilder;
    private final MessageSource messages;

    public UserFormController(ReflectionFormFieldMetadataBuilder metadataBuilder, MessageSource messages) {
        this.metadataBuilder = metadataBuilder;
        this.messages = messages;
    }

    @GetMapping("/user-form")
    public String showForm(Model model, Locale locale) {
        model.addAttribute("fields", SpringFormFieldLabels.resolve(metadataBuilder.buildMetadata(User.class), messages, locale));
        model.addAttribute("formName", "User");
        model.addAttribute("formData", new User());
        return "bootstrap/forms/vertical/form-base";
    }
}
