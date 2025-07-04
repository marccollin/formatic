package com.formatic.annotation.handler;

import com.formatic.annotation.CommonFieldAttributes;
import com.formatic.form.FormFieldMetadata;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public abstract class BaseFormFieldHandler<T extends Annotation>
        implements FormFieldAnnotationHandler<T> {

    private final Class<T> annotationType;

    protected BaseFormFieldHandler(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(annotationType);
    }

    @Override
    public FormFieldMetadata handle(Field field) {
        T annotation = field.getAnnotation(annotationType);
        FormFieldMetadata metadata = new FormFieldMetadata(); // ou builder selon votre implémentation

        // Traitement des attributs communs
        processCommonAttributes(annotation, metadata, field);

        // Traitement spécifique à chaque type
        processSpecificAttributes(annotation, metadata, field);

        return metadata;
    }

    protected void processCommonAttributes(T annotation, FormFieldMetadata metadata, Field field) {
        CommonFieldAttributes common = extractCommonAttributes(annotation);

        // Logique pour name
        if(common.name() == null || common.name().length() == 0) {
            metadata.setName(field.getName());
        } else {
            metadata.setName(common.name());
        }

        // Logique pour label
        if(common.label() == null || common.label().length() == 0) {
            metadata.setLabel(capitalizeFirstLetter(field.getName()));
        } else {
            metadata.setLabel(common.label());
        }

        // Autres attributs communs
        metadata.setRequired(common.required());
        //metadata.setHtmlAttributes(common.htmlAttributes());
    }

    protected CommonFieldAttributes extractCommonAttributes(T annotation) {
        try {
            String name = (String) annotation.getClass().getMethod("name").invoke(annotation);
            String label = (String) annotation.getClass().getMethod("label").invoke(annotation);
            boolean required = (Boolean) annotation.getClass().getMethod("required").invoke(annotation);
            boolean disabled = (Boolean) annotation.getClass().getMethod("disabled").invoke(annotation);
            String[] htmlAttributes = (String[]) annotation.getClass().getMethod("htmlAttributes").invoke(annotation);

            return new CommonFieldAttributes() {
                @Override public String name() { return name; }
                @Override public String label() { return label; }
                @Override public boolean required() { return required; }
                @Override public boolean disabled() { return disabled; }
                @Override public String[] htmlAttributes() { return htmlAttributes; }
            };
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction des attributs communs", e);
        }
    }

    protected abstract void processSpecificAttributes(T annotation, FormFieldMetadata metadata, Field field);


    private String capitalizeFirstLetter(String str) {
        return StringUtils.hasText(str) ?
                str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }

}
