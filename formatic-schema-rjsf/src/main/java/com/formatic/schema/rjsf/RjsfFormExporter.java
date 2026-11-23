package com.formatic.schema.rjsf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.form.FormDataType;
import com.formatic.core.form.FormDefinition;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.schema.json.DefaultJsonSchemaExporter;
import com.formatic.schema.json.JsonSchemaExportException;

import java.util.Objects;

/** RJSF 5-compatible UI schema paired with the default Draft 2020-12 data schema. */
public final class RjsfFormExporter {
    public JsonFormDefinition export(FormDefinition form) {
        Objects.requireNonNull(form, "form");
        ObjectNode schema = new DefaultJsonSchemaExporter().export(form);
        ObjectNode ui = JsonNodeFactory.instance.objectNode();
        for (FormFieldMetadata field : form.fields()) {
            validatePresentation(field);
            String[] segments = field.getName().split("\\.");
            ObjectNode parent = ui;
            for (String segment : segments) {
                if (segment.startsWith("ui:")) {
                    throw error(field, "binding path conflicts with reserved RJSF ui: keys");
                }
                addOrder(parent, segment);
                JsonNode existing = parent.get(segment);
                parent = existing == null ? parent.putObject(segment) : (ObjectNode) existing;
            }
            parent.put("ui:widget", widget(field));
            if (hasText(field.getPlaceholder())) {
                parent.put("ui:placeholder", field.getPlaceholder());
            }
            if (hasText(field.getHelpText())) {
                parent.put("ui:help", field.getHelpText());
            }
            if (field.isDisabled()) {
                parent.put("ui:disabled", true);
            }
            if (field.isReadonly()) {
                parent.put("ui:readonly", true);
            }
            if (field.getType() == FormFieldType.TEXTAREA && field.getRows() > 0) {
                parent.putObject("ui:options").put("rows", field.getRows());
            }
        }
        return new JsonFormDefinition(schema, ui);
    }

    private String widget(FormFieldMetadata field) {
        return switch (field.getType()) {
            case AUTO -> switch (field.getDataType()) {
                case STRING -> "text";
                case INTEGER, NUMBER -> "updown";
                case BOOLEAN -> "checkbox";
                case ARRAY -> throw error(field, "AUTO arrays are unsupported");
            };
            case TEXT, PHONE, TIME, DATETIME_LOCAL -> {
                if (field.getDataType() != FormDataType.STRING) {
                    throw error(field, "text widget requires string dataType");
                }
                yield "text";
            }
            case TEXTAREA -> {
                if (field.getDataType() != FormDataType.STRING) {
                    throw error(field, "textarea widget requires string dataType");
                }
                yield "textarea";
            }
            case NUMBER -> "updown";
            case RANGE -> {
                if (!Double.isFinite(field.getMin()) || !Double.isFinite(field.getMax())) {
                    throw error(field, "range widget requires explicit min and max");
                }
                yield "range";
            }
            case SELECT -> {
                requireChoices(field);
                yield "select";
            }
            case RADIO -> {
                requireChoices(field);
                yield "radio";
            }
            case CHECKBOX -> {
                if (field.getDataType() == FormDataType.ARRAY) {
                    requireChoices(field);
                    yield "checkboxes";
                }
                if (field.getDataType() != FormDataType.BOOLEAN) {
                    throw error(field, "single checkbox widget requires boolean dataType");
                }
                yield "checkbox";
            }
            case HIDDEN -> "hidden";
            case PASSWORD -> "password";
            case EMAIL -> "email";
            case URL -> "uri";
            case COLOR -> "color";
            case DATE -> "date";
            default -> throw error(field, "unsupported RJSF control " + field.getType());
        };
    }

    private void validatePresentation(FormFieldMetadata field) {
        if (hasText(field.getGroup())) {
            throw error(field, "group layout is unsupported");
        }
        if (hasText(field.getDisplayCondition())) {
            throw error(field, "displayCondition is unsupported");
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

    private static void addOrder(ObjectNode parent, String name) {
        ArrayNode order = parent.has("ui:order") ? (ArrayNode) parent.get("ui:order") : parent.putArray("ui:order");
        for (JsonNode value : order) {
            if (value.asText().equals(name)) {
                return;
            }
        }
        order.add(name);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static JsonSchemaExportException error(FormFieldMetadata field, String feature) {
        return new JsonSchemaExportException(field.getName(), feature);
    }
}
