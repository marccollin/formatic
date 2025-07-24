package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.HiddenInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link HiddenInput}.
 * <p>
 * This class processes metadata for hidden  input fields in dynamic form generation.
 * <p>
 * Maps the {@code @HiddenInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class HiddenInputHandler implements FormFieldAnnotationHandler<HiddenInput> {

    public HiddenInputHandler() {

    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(HiddenInput.class);
    }

    @Override
    public FormFieldMetadata handle(Field field) {

        HiddenInput annotation = field.getAnnotation(HiddenInput.class);
        FormFieldMetadata metadata = new FormFieldMetadata();

        if (annotation.name() == null || annotation.name().isEmpty()) {
            metadata.setName(field.getName());
        } else {
            metadata.setName(annotation.name());
        }

        metadata.setType(FormFieldType.HIDDEN);
        metadata.setDefaultValue(annotation.defaultValue());

        return metadata;
    }
}
