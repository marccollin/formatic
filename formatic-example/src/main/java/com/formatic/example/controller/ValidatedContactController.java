package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.example.dto.ValidatedContactForm;
import com.formatic.render.FormLayout;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormTheme;
import com.formatic.render.thymeleaf.ThymeleafFormRenderer;
import com.formatic.spring.config.FormaticProperties;
import com.formatic.spring.render.FormRendererViewResolver;
import com.formatic.spring.render.SpringFormRenderModels;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Demonstrates the same Formatic DTO validation for browser and JSON submissions. */
@Controller
@Import(ValidatedContactController.Views.class)
public class ValidatedContactController {
    private final ReflectionFormFieldMetadataBuilder metadata;
    private final MessageSource messages;

    public ValidatedContactController(ReflectionFormFieldMetadataBuilder metadata, MessageSource messages) {
        this.metadata = metadata;
        this.messages = messages;
    }

    @GetMapping("/validated-contact")
    public ModelAndView show(HttpServletRequest request, Locale locale) {
        return view(new BeanPropertyBindingResult(new ValidatedContactForm(), "formData"), request, locale, null);
    }

    @PostMapping("/validated-contact")
    public ModelAndView submit(@Valid @ModelAttribute("formData") ValidatedContactForm form, BindingResult errors,
                               HttpServletRequest request, Locale locale) {
        return view(errors, request, locale, errors.hasErrors() ? null : "Contact received (not saved).");
    }

    @PostMapping("/api/validated-contact")
    @ResponseBody
    public ResponseEntity<ContactResponse> submitJson(@Valid @RequestBody ValidatedContactForm form,
                                                      BindingResult errors, Locale locale) {
        if (errors.hasErrors()) {
            List<ContactError> fieldErrors = errors.getFieldErrors().stream()
                    .map(error -> new ContactError(error.getField(), messages.getMessage(error, locale))).toList();
            List<ContactError> globalErrors = errors.getGlobalErrors().stream()
                    .map(error -> new ContactError(null, messages.getMessage(error, locale))).toList();
            var all = new java.util.ArrayList<>(fieldErrors);
            all.addAll(globalErrors);
            return ResponseEntity.badRequest().body(new ContactResponse(false, List.copyOf(all)));
        }
        return ResponseEntity.ok(new ContactResponse(true, List.of()));
    }

    private ModelAndView view(BindingResult binding, HttpServletRequest request, Locale locale, String success) {
        var form = SpringFormRenderModels.fromBinding("Validated contact", request.getContextPath() + "/validated-contact",
                metadata.buildMetadata(ValidatedContactForm.class), binding, messages, locale, success, Map.of());
        return new ModelAndView("validated-contact-example", FormRendererViewResolver.FORM_MODEL_ATTRIBUTE, form);
    }

    public record ContactError(String field, String message) {}
    public record ContactResponse(boolean accepted, List<ContactError> errors) {}

    @Configuration(proxyBeanMethods = false)
    static class Views {
        @Bean
        FormRendererViewResolver validatedContactView(FormaticProperties properties) {
            FormRenderer renderer = new ThymeleafFormRenderer(properties.getThymeleaf().getTemplatePrefix(),
                    "form.html", FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
            FormRenderer page = form -> """
                    <!doctype html>
                    <html lang="en"><head><meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Validated contact</title>
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css"
                          integrity="sha384-LN+7fdVzj6u52u30Kp6M/trliBMCMKTyK833zpbD+pXdCLuTusPj697FH4R/5mcr"
                          crossorigin="anonymous">
                    </head><body><main class="container py-4">
                    """ + renderer.render(form) + "</main></body></html>";
            return new FormRendererViewResolver(Map.of("validated-contact-example", page));
        }
    }
}
