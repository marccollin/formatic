package com.formatic.core.form;

import java.util.List;

/**
 * Renderer-neutral form description without submitted values or validation errors.
 * The title may be null. The field list is copied and unmodifiable, but its
 * metadata elements remain shared and mutable; consumers must not mutate them.
 */
public record FormDefinition(String title, List<FormFieldMetadata> fields) {
    public FormDefinition {
        fields = List.copyOf(fields);
    }
}
