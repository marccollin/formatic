package com.formatic.render;

import com.formatic.core.form.FormFieldMetadata;

import java.util.List;
import java.util.Objects;

/**
 * A field's metadata and request-local state. Null values mean no bound value
 * (defaults may apply); an empty list represents an explicit empty selection.
 * Values are strings in the format expected by the HTML control.
 */
public record RenderField(FormFieldMetadata metadata, List<String> values, List<String> errors) {
    public RenderField {
        Objects.requireNonNull(metadata, "metadata");
        values = values == null ? null : List.copyOf(values);
        errors = List.copyOf(errors);
    }

    public RenderField(FormFieldMetadata metadata) {
        this(metadata, null, List.of());
    }
}
