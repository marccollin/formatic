package com.formatic.render.internal;

import com.formatic.core.form.FormFieldType;
import com.formatic.render.FormLayout;
import com.formatic.render.FormTheme;

import java.util.Objects;

/** CSS presentation shared by the bundled JTE and Pebble templates. */
public record FormStyle(FormTheme theme, FormLayout layout) {
    private static final String TAILWIND_CONTROL =
            "w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-500";

    public FormStyle {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(layout, "layout");
    }

    public boolean bootstrapHorizontal() {
        return theme == FormTheme.BOOTSTRAP && layout == FormLayout.HORIZONTAL;
    }

    public String formClass() {
        return switch (theme) {
            case BOOTSTRAP -> bootstrapHorizontal() ? null : "row g-3";
            case TAILWIND -> "space-y-6";
            case BULMA -> null;
        };
    }

    public String fieldsGrid() {
        if (layout != FormLayout.HORIZONTAL) return null;
        return switch (theme) {
            case BOOTSTRAP -> null;
            case TAILWIND -> "grid grid-cols-1 gap-6 md:grid-cols-2";
            case BULMA -> "columns is-multiline";
        };
    }

    public String fieldWrapper(String custom) {
        String base = switch (theme) {
            case BOOTSTRAP -> bootstrapHorizontal() ? "row mb-3" : null;
            case TAILWIND -> layout == FormLayout.VERTICAL ? "mb-4" : null;
            case BULMA -> layout == FormLayout.HORIZONTAL ? "column is-half-desktop" : "field";
        };
        return classes(base, custom);
    }

    public String labelWrapper(String custom) {
        return classes(theme == FormTheme.BOOTSTRAP && bootstrapHorizontal() ? "mb-3" : fieldWrapper(null), custom);
    }

    public String labelClass() {
        return switch (theme) {
            case BOOTSTRAP -> bootstrapHorizontal() ? "col-sm-2 col-form-label" : "form-label";
            case TAILWIND -> "mb-2 block text-sm font-medium text-gray-700";
            case BULMA -> "label";
        };
    }

    public String fieldBody(boolean singleCheckbox) {
        if (bootstrapHorizontal()) return singleCheckbox ? "col-sm-10 offset-sm-2" : "col-sm-10";
        return theme == FormTheme.BULMA ? "control" : null;
    }

    public String checkWrapper() {
        return switch (theme) {
            case BOOTSTRAP -> "form-check";
            case TAILWIND -> "flex items-center gap-2";
            case BULMA -> "field";
        };
    }

    public String checkLabel(boolean radio) {
        return switch (theme) {
            case BOOTSTRAP -> "form-check-label";
            case TAILWIND -> "text-sm text-gray-700";
            case BULMA -> radio ? "radio" : "checkbox";
        };
    }

    public String controlClass(FormFieldType type, boolean invalid, String custom) {
        String base = switch (theme) {
            case BOOTSTRAP -> switch (type) {
                case CHECKBOX, RADIO -> "form-check-input";
                case SELECT -> "form-select";
                case RANGE -> "form-range";
                case COLOR -> "form-control form-control-color";
                default -> "form-control";
            };
            case TAILWIND -> switch (type) {
                case CHECKBOX, RADIO -> "size-4 accent-blue-600";
                case RANGE -> "w-full accent-blue-600";
                default -> TAILWIND_CONTROL;
            };
            case BULMA -> switch (type) {
                case CHECKBOX, RADIO, SELECT -> null;
                case TEXTAREA -> "textarea";
                default -> "input";
            };
        };
        String error = !invalid ? null : switch (theme) {
            case BOOTSTRAP -> "is-invalid";
            case TAILWIND -> switch (type) {
                case CHECKBOX, RADIO, RANGE -> "accent-red-600";
                default -> "border-red-500! focus:border-red-500! focus:ring-red-500!";
            };
            case BULMA -> type == FormFieldType.SELECT ? null : "is-danger";
        };
        return classes(classes(base, error), custom);
    }

    public String selectWrapper(boolean multiple, boolean invalid) {
        if (theme != FormTheme.BULMA) return null;
        return classes(classes("select", multiple ? "is-multiple" : null), invalid ? "is-danger" : null);
    }

    public String helpClass() {
        return switch (theme) {
            case BOOTSTRAP -> "form-text";
            case TAILWIND -> "mt-1 text-sm text-gray-600";
            case BULMA -> "help";
        };
    }

    public String errorClass() {
        return switch (theme) {
            case BOOTSTRAP -> "invalid-feedback d-block";
            case TAILWIND -> "mt-1 text-sm text-red-600";
            case BULMA -> "help is-danger";
        };
    }

    public String titleClass() {
        return switch (theme) {
            case BOOTSTRAP -> null;
            case TAILWIND -> "text-2xl font-semibold text-gray-900";
            case BULMA -> "title";
        };
    }

    public String successClass() {
        return switch (theme) {
            case BOOTSTRAP -> "alert alert-success";
            case TAILWIND -> "rounded-md bg-green-50 p-4 text-green-800";
            case BULMA -> "notification is-success";
        };
    }

    public String globalErrorClass() {
        return switch (theme) {
            case BOOTSTRAP -> "alert alert-danger";
            case TAILWIND -> "rounded-md bg-red-50 p-4 text-red-800";
            case BULMA -> "notification is-danger";
        };
    }

    public String submitWrapper() {
        return switch (theme) {
            case BOOTSTRAP -> bootstrapHorizontal() ? "offset-sm-2" : "col-12";
            case TAILWIND -> layout == FormLayout.HORIZONTAL ? "md:col-span-2" : "pt-4";
            case BULMA -> layout == FormLayout.HORIZONTAL ? "column is-full" : "field";
        };
    }

    public String submitClass() {
        return switch (theme) {
            case BOOTSTRAP -> "btn btn-primary";
            case TAILWIND -> "w-full rounded-md bg-blue-600 px-4 py-2 text-white hover:bg-blue-700";
            case BULMA -> "button is-primary";
        };
    }

    private static String classes(String first, String second) {
        if (second == null || second.isBlank()) return first;
        return first == null ? second : first + " " + second;
    }
}
