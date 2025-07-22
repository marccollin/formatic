package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.CommonFieldAttributes;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Abstract base class for handling form field annotations.
 *
 * This class provides a generic framework for extracting metadata from annotated fields
 * and populating {@link FormFieldMetadata} objects.
 *
 * Key responsibilities:
 * <ul>
 *   <li>Checks if a field is supported based on a specific annotation type.</li>
 *   <li>Handles both common and annotation-specific attributes.</li>
 *   <li>Provides a reflection-based mechanism to extract shared attributes
 *       like name, label, required, disabled, and htmlAttributes.</li>
 * </ul>
 *
 * Subclasses are responsible for implementing {@link #processSpecificAttributes}
 * to handle annotation-specific logic (e.g., for text inputs, selects, checkboxes, etc.).
 *
 * This base class ensures consistent handling of field names, labels,
 * and other shared properties across all form field types.
 */
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
        FormFieldMetadata metadata = new FormFieldMetadata();

        // Processing common attributes
        processCommonAttributes(annotation, metadata, field);

        // Processing specific attributes
        processSpecificAttributes(annotation, metadata, field);

        return metadata;
    }

    protected void processCommonAttributes(T annotation, FormFieldMetadata metadata, Field field) {
        CommonFieldAttributes common = extractCommonAttributes(annotation);

        // Logic for the name field
        if (common.name() == null || common.name().isEmpty()) {
            metadata.setName(field.getName());
        } else {
            metadata.setName(common.name());
        }

        // Logic for the label field
        if (common.label() == null || common.label().isEmpty()) {
            metadata.setLabel(capitalizeFirstLetter(field.getName()));
        } else {
            metadata.setLabel(common.label());
        }

        // Other common attributes
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
                @Override
                public String name() {
                    return name;
                }

                @Override
                public String label() {
                    return label;
                }

                @Override
                public boolean required() {
                    return required;
                }

                @Override
                public boolean disabled() {
                    return disabled;
                }

                @Override
                public String[] htmlAttributes() {
                    return htmlAttributes;
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Error while extracting common attributes", e);
        }
    }

    protected abstract void processSpecificAttributes(T annotation, FormFieldMetadata metadata, Field field);


    private String capitalizeFirstLetter(String str) {
        return StringUtils.hasText(str) ?
                str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }

}
