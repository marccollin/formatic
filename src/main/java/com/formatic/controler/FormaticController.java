package com.formatic.controler;

import com.formatic.dto.EditorDto;
import com.formatic.form.FormFieldMetadata;
import com.formatic.service.FormMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class FormaticController {

    private static final Logger logger = LoggerFactory.getLogger(FormaticController.class);


    private final FormMetadataService formMetadataService;

    FormaticController(FormMetadataService formMetadataService){
        this.formMetadataService = formMetadataService;
    }

    @GetMapping("/form")
    public String showForm(@RequestParam("class") String className, Model model) {
        try {
            Class<?> formClass = Class.forName(className);
            List<FormFieldMetadata> formMetadata = formMetadataService.getMetadataForClass(formClass);

            model.addAttribute("fields", formMetadata);
            model.addAttribute("formData", formClass.getDeclaredConstructor().newInstance());
            model.addAttribute("formTitle", "Formulaire " + formClass.getSimpleName());

            return "forms/form-base";
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du formulaire pour la classe {}: {}",
                    className, e.getMessage());
            model.addAttribute("error", "Impossible de générer le formulaire");
            return "error";
        }
    }

    @GetMapping("/form2")
    public String showFormValue(@RequestParam("class") String className, Model model) {
        try {
            Class<?> formClass = Class.forName(className);
            List<FormFieldMetadata> formMetadata = formMetadataService.getMetadataForClass(formClass);

            EditorDto editorDto = (EditorDto) formClass.getDeclaredConstructor().newInstance();

            editorDto.setCity("LONG");
            editorDto.setCodePostal("J4J1T5");
            editorDto.setBirthDate(LocalDate.of(1979,3,7));
            editorDto.setInterest(List.of("sportCheck"));
            editorDto.setConfidential(true);

            editorDto.setPaymentMethod("paypal");

            model.addAttribute("fields", formMetadata);
            model.addAttribute("formData", editorDto);
            model.addAttribute("formTitle", "Formulaire " + formClass.getSimpleName());

            return "forms/form-base";
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du formulaire pour la classe {}: {}",
                    className, e.getMessage());
            model.addAttribute("error", "Impossible de générer le formulaire");
            return "error";
        }
    }

}
