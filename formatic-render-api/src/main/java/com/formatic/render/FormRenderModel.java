package com.formatic.render;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request-local input for a renderer. Messages are already localized. Extra hidden
 * fields can carry application-supplied values, such as a CSRF token.
 */
public record FormRenderModel(String title, String action, List<RenderField> fields,
                              List<String> globalErrors, String successMessage,
                              Map<String, String> hiddenFields) {
    public FormRenderModel {
        Objects.requireNonNull(action, "action");
        fields = List.copyOf(fields);
        globalErrors = List.copyOf(globalErrors);
        hiddenFields = Map.copyOf(hiddenFields);
    }

    public FormRenderModel(String title, String action, List<RenderField> fields) {
        this(title, action, fields, List.of(), null, Map.of());
    }
}
