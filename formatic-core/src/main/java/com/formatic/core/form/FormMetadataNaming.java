package com.formatic.core.form;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FormMetadataNaming {
    private FormMetadataNaming() {}

    public static List<FormFieldMetadata> apply(List<FormFieldMetadata> metadata,
                                                FieldIdStyle style, String location) {
        Set<String> names = new HashSet<>();
        Set<String> ids = new HashSet<>();
        List<FormFieldMetadata> result = new ArrayList<>(metadata.size());
        for (FormFieldMetadata source : metadata) {
            FormFieldMetadata field = new FormFieldMetadata(source);
            field.setIdStyle(style);
            if (!names.add(field.getName())) {
                throw new ConstraintValidationException(location + ": duplicate field name " + field.getName());
            }
            if (!ids.add(field.getId())) {
                throw new ConstraintValidationException(location + ": duplicate HTML ID " + field.getId()
                        + " with style " + style);
            }
            result.add(field);
        }
        return result;
    }
}
