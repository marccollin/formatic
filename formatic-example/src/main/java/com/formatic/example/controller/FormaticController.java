package com.formatic.example.controller;

import com.formatic.example.dto.CssLibrary;
import com.formatic.example.dto.Editor;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldMetadataBuilder;
import com.formatic.example.dto.Form;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class FormaticController {

    private static final Logger logger = LoggerFactory.getLogger(FormaticController.class);

    private final FormFieldMetadataBuilder formFieldMetadataBuilder;

    FormaticController(FormFieldMetadataBuilder formFieldMetadataBuilder) {
        this.formFieldMetadataBuilder = formFieldMetadataBuilder;
    }

    @GetMapping("/form")
    public String showFormNew(@RequestParam("class") String className, @RequestParam("cssLibrary") CssLibrary cssLibrary, Form form, Model model) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> formClass = Class.forName(className);

        List<FormFieldMetadata> formMetadata = formFieldMetadataBuilder.buildMetadata(formClass);
        model.addAttribute("fields", formMetadata);
        model.addAttribute("formName", formClass.getSimpleName());
        model.addAttribute("formData", formClass.getDeclaredConstructor().newInstance());

        if(form==Form.HORIZONTAL){
            return cssLibrary.name().toLowerCase() + "/forms/horizontal/form-base";
        }

        return cssLibrary.name().toLowerCase() + "/forms/vertical/form-base";
    }

    @GetMapping("/formhybrid/{id}")
    public String showFormHybrid(@RequestParam("class") String className, Model model, @PathVariable Long id) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> formClass = Class.forName(className);

        List<FormFieldMetadata> formMetadata = formFieldMetadataBuilder.buildMetadata(formClass);
        model.addAttribute("fields", formMetadata);
        model.addAttribute("formData", formClass.getDeclaredConstructor().newInstance());
        model.addAttribute("editorId", id);
        return "bootstrap/forms/form-base-hybrid";
    }


    @GetMapping("/formfeeded")
    public String showFormValue(Model model) {
        try {

            List<FormFieldMetadata> formMetadata = formFieldMetadataBuilder.buildMetadata(Editor.class);

            Editor editorDto = new Editor();

            editorDto.setCity("LONG");
            editorDto.setCodePostal("J4J1T5");
            editorDto.setBirthDate(LocalDate.of(1979, 3, 7));
            editorDto.setInterest(List.of("sportCheck"));
            editorDto.setConfidential(true);

            editorDto.setPaymentMethod("paypal");

            model.addAttribute("fields", formMetadata);
            model.addAttribute("formData", editorDto);
            model.addAttribute("formTitle", "Form " + Editor.class.getSimpleName());

            return "bootstrap/forms/form-base";
        } catch (Exception e) {
            logger.error("Error while generating the form for the class {}: {}",
                    Editor.class.getSimpleName(), e.getMessage());
            model.addAttribute("error", "Unable to generate the form");
            return "error";
        }
    }


}
