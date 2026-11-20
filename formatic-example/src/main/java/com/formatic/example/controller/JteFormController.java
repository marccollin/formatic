package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.example.dto.CustomerForm;
import com.formatic.render.FormRenderer;
import com.formatic.render.jte.JteFormRenderer;
import com.formatic.spring.render.SpringFormRenderModels;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping(path = "/jte/customer", produces = "text/html;charset=UTF-8")
@ResponseBody
public class JteFormController {
    private final ReflectionFormFieldMetadataBuilder metadata;
    private final MessageSource messages;
    private final FormRenderer renderer = new JteFormRenderer();

    public JteFormController(ReflectionFormFieldMetadataBuilder metadata, MessageSource messages) {
        this.metadata = metadata;
        this.messages = messages;
    }

    @GetMapping
    public String show(HttpServletRequest request, Locale locale) {
        return render(new BeanPropertyBindingResult(new CustomerForm(), "formData"), request, locale, null);
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("formData") CustomerForm form, BindingResult binding,
                         HttpServletRequest request, Locale locale) {
        return render(binding, request, locale, binding.hasErrors() ? null : "Customer details received (not saved).");
    }

    private String render(BindingResult binding, HttpServletRequest request, Locale locale, String success) {
        String fragment = renderer.render(SpringFormRenderModels.fromBinding("JTE customer form",
                request.getContextPath() + "/jte/customer", metadata.buildMetadata(CustomerForm.class),
                binding, messages, locale, success, Map.of()));
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>JTE customer form</title>
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css"
                          integrity="sha384-LN+7fdVzj6u52u30Kp6M/trliBMCMKTyK833zpbD+pXdCLuTusPj697FH4R/5mcr"
                          crossorigin="anonymous">
                </head>
                <body class="container py-4">
                """ + fragment + "</body></html>";
    }
}
