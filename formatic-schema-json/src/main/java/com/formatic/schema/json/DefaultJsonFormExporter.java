package com.formatic.schema.json;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.form.FormDataType;
import com.formatic.core.form.FormDefinition;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.util.Locale;
import java.util.Objects;

/** Exports JSON Schema with Formatic neutral UI contract version 1. */
public final class DefaultJsonFormExporter {
    public JsonFormDefinition export(FormDefinition form) {
        Objects.requireNonNull(form, "form");
        ObjectNode schema = new DefaultJsonSchemaExporter().export(form);
        ObjectNode ui = JsonNodeFactory.instance.objectNode().put("version", 1);
        var fields = ui.putArray("fields");
        for (FormFieldMetadata field : form.fields()) {
            validatePresentation(field);
            ObjectNode control = fields.addObject();
            control.put("path", field.getName());
            control.put("widget", widget(field));
            if (field.getLabel() != null) {
                control.put("label", field.getLabel());
            }
            if (hasText(field.getPlaceholder())) {
                control.put("placeholder", field.getPlaceholder());
            }
            if (hasText(field.getHelpText())) {
                control.put("helpText", field.getHelpText());
            }
            control.put("disabled", field.isDisabled());
            control.put("readOnly", field.isReadonly());
            if (field.getType() == FormFieldType.TEXTAREA && field.getRows() > 0) {
                control.put("rows", field.getRows());
            }
        }
        return new JsonFormDefinition(schema, ui);
    }

    private String widget(FormFieldMetadata field) {
        return switch (field.getType()) {
            case AUTO -> switch (field.getDataType()) {
                case STRING -> "text";
                case INTEGER, NUMBER -> "number";
                case BOOLEAN -> "checkbox";
                case ARRAY -> throw error(field, "AUTO arrays are unsupported");
            };
            case TEXT, TEXTAREA -> {
                if (field.getDataType() != FormDataType.STRING) {
                    throw error(field, "text/textarea widget requires string dataType");
                }
                yield field.getType().name().toLowerCase(Locale.ROOT);
            }
            case SELECT, RADIO -> {
                requireChoices(field);
                yield field.getType() == FormFieldType.RADIO ? "radio"
                        : field.isMultiple() ? "multiselect" : "select";
            }
            case CHECKBOX -> {
                if (field.getDataType() == FormDataType.ARRAY) {
                    requireChoices(field);
                    yield "checkboxes";
                }
                if (field.getDataType() != FormDataType.BOOLEAN) {
                    throw error(field, "single checkbox requires boolean dataType");
                }
                yield "checkbox";
            }
            case RANGE -> {
                if (!Double.isFinite(field.getMin()) || !Double.isFinite(field.getMax())) {
                    throw error(field, "range widget requires explicit min and max");
                }
                yield "range";
            }
            case DATETIME_LOCAL -> "datetime-local";
            case NUMBER, HIDDEN, PASSWORD, EMAIL, URL, PHONE, COLOR, DATE, TIME ->
                    field.getType().name().toLowerCase(Locale.ROOT);
            default -> throw error(field, "unsupported neutral UI control " + field.getType());
        };
    }

    private void validatePresentation(FormFieldMetadata field) {
        if (hasText(field.getGroup()) || hasText(field.getDisplayCondition())) {
            throw error(field, "group/displayCondition presentation is unsupported");
        }
        if (field.getOptions() != null) {
            for (var option : field.getOptions()) {
                if (option.isDisabled() || hasText(option.getGroup()) || option.isSelected()) {
                    throw error(field, "disabled/grouped/selected options are unsupported; use explicit defaults");
                }
            }
        }
        if (field.getCols() > 0 || hasText(field.getWrap())) {
            throw error(field, "cols/wrap presentation is unsupported");
        }
        if (field.getRows() > 0 && field.getType() != FormFieldType.TEXTAREA) {
            throw error(field, "rows requires a textarea");
        }
    }

    private void requireChoices(FormFieldMetadata field) {
        if (field.getOptions() == null || field.getOptions().isEmpty()) {
            throw error(field, "choice widget requires prepared options");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static JsonSchemaExportException error(FormFieldMetadata field, String feature) {
        return new JsonSchemaExportException(field.getName(), feature);
    }
}
