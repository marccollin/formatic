package com.formatic.render.internal;

import com.formatic.core.form.FormFieldType;
import com.formatic.render.FormRenderModel;

import java.net.URI;

/** Shared form-level rules for the bundled renderers. */
public final class FormView {
    private FormView() {
    }

    public static void validateAction(String action) {
        if (!action.isEmpty()) {
            URI uri = URI.create(action);
            if (!action.startsWith("/") || action.startsWith("//") || uri.getRawAuthority() != null) {
                throw new IllegalArgumentException("Form action must be empty or a root-relative application URL");
            }
        }
    }

    public static boolean multipart(FormRenderModel form) {
        return form.fields().stream().anyMatch(field -> field.metadata().getType() == FormFieldType.FILE);
    }
}
