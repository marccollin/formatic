package com.formatic.schema.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.form.FormConstraintValidator;
import com.formatic.core.form.FormDataType;
import com.formatic.core.form.FormDataValue;
import com.formatic.core.form.FormDefinition;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.FormFieldValues;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Strict Draft 2020-12 projection. Export is read-only and returns a fresh tree.
 * Presentation belongs to a separate UI schema adapter.
 */
public final class DefaultJsonSchemaExporter implements JsonSchemaExporter {
    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;
    public static final String DIALECT = "https://json-schema.org/draft/2020-12/schema";
    private static final JsonSchemaFactory VALIDATOR = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    private static final SchemaValidatorsConfig VALIDATION =
            SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();
    private static final String TIME_PATTERN = "(?:[01][0-9]|2[0-3]):[0-5][0-9]"
            + "(?::[0-5][0-9](?:\\.[0-9]{1,3})?)?";
    private static final String YEAR_PATTERN = "(?!0000)[0-9]{4}";
    private static final String DATE_PATTERN = "(?:" + YEAR_PATTERN
            + "-(?:(?:01|03|05|07|08|10|12)-(?:0[1-9]|[12][0-9]|3[01])"
            + "|(?:04|06|09|11)-(?:0[1-9]|[12][0-9]|30)|02-(?:0[1-9]|1[0-9]|2[0-8]))"
            + "|(?!0000)(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:[02468][048]|[13579][26])00)-02-29)";

    @Override
    public ObjectNode export(FormDefinition form) {
        Objects.requireNonNull(form, "form");
        ObjectNode root = objectSchema();
        root.put("$schema", DIALECT);
        if (form.title() != null) {
            root.put("title", form.title());
        }
        for (FormFieldMetadata field : form.fields()) {
            String path = field.getName();
            if (path == null || path.isEmpty()) {
                throw new JsonSchemaExportException("<form>", "field name is required");
            }
            String[] segments = path.split("\\.", -1);
            for (String segment : segments) {
                if (segment.isEmpty()) {
                    throw error(field, "empty binding path segment");
                }
            }
            ObjectNode leaf;
            try {
                leaf = fieldSchema(field);
            } catch (JsonSchemaExportException exception) {
                throw exception;
            } catch (IllegalArgumentException exception) {
                throw new JsonSchemaExportException(path, "invalid metadata: " + exception.getMessage(), exception);
            }
            ObjectNode parent = root;
            for (int i = 0; i < segments.length - 1; i++) {
                ObjectNode properties = (ObjectNode) parent.get("properties");
                JsonNode existing = properties.get(segments[i]);
                if (existing == null) {
                    existing = objectSchema();
                    properties.set(segments[i], existing);
                } else if (!existing.path("type").asText().equals("object")) {
                    throw error(field, "leaf/object binding path collision");
                }
                if (field.isRequired()) {
                    require(parent, segments[i]);
                }
                parent = (ObjectNode) existing;
            }
            String name = segments[segments.length - 1];
            ObjectNode properties = (ObjectNode) parent.get("properties");
            if (properties.has(name)) {
                throw error(field, "duplicate or leaf/object binding path collision");
            }
            properties.set(name, leaf);
            if (field.isRequired()) {
                require(parent, name);
            }
        }
        return root;
    }

    private ObjectNode fieldSchema(FormFieldMetadata field) {
        FormDataType type = field.getDataType();
        if (type == null) {
            throw error(field, "unknown dataType");
        }
        validateControl(field);
        FormConstraintValidator.validate(field, field.getName());
        FormConstraintValidator.validateNumber(field.getName(), field.getMin(), field.getMax(), field.getStep());
        FormConstraintValidator.validateLengths(field.getName(), field.getMinLength(), field.getMaxLength());
        if (!Double.isNaN(field.getStep())) {
            throw error(field, "step is unsupported: HTML stepping is not necessarily multipleOf");
        }
        if (hasText(field.getPattern())) {
            throw error(field, "pattern translation is not supported yet");
        }
        if (hasText(field.getMinDate()) || hasText(field.getMaxDate()) || hasText(field.getDateStep())
                || hasText(field.getMinDateTime()) || hasText(field.getMaxDateTime()) || hasText(field.getDateTimeStep())
                || hasText(field.getMinTime()) || hasText(field.getMaxTime()) || hasText(field.getTimeStep())) {
            throw error(field, "temporal bounds/step are unsupported");
        }
        boolean numeric = type == FormDataType.INTEGER || type == FormDataType.NUMBER;
        if (!numeric && (!Double.isNaN(field.getMin()) || !Double.isNaN(field.getMax()))) {
            throw error(field, "numeric bounds require an integer or number dataType");
        }
        if (type != FormDataType.STRING && (field.getMinLength() >= 0 || field.getMaxLength() >= 0)) {
            throw error(field, "length constraints require a string dataType");
        }
        ObjectNode schema = typedSchema(type);
        addControlConstraints(field, schema);
        if (type == FormDataType.ARRAY) {
            if (field.getItemDataType() == null || field.getItemDataType() == FormDataType.ARRAY) {
                throw error(field, "unknown or non-scalar itemDataType");
            }
            schema.set("items", typedSchema(field.getItemDataType()));
            schema.put("uniqueItems", true);
        }
        if (field.getLabel() != null) {
            schema.put("title", field.getLabel());
        }
        if (hasText(field.getHelpText())) {
            schema.put("description", field.getHelpText());
        }
        if (field.isReadonly()) {
            schema.put("readOnly", true);
        }
        if (!Double.isNaN(field.getMin())) {
            schema.put("minimum", BigDecimal.valueOf(field.getMin()));
        }
        if (!Double.isNaN(field.getMax())) {
            schema.put("maximum", BigDecimal.valueOf(field.getMax()));
        }
        if (field.getMinLength() >= 0) {
            schema.put("minLength", field.getMinLength());
        }
        if (field.getMaxLength() >= 0) {
            schema.put("maxLength", field.getMaxLength());
        }
        if (hasText(field.getOptionsProvider()) && (field.getOptions() == null || field.getOptions().isEmpty())) {
            throw error(field, "optionsProvider is unresolved or has no choices");
        }
        List<FormDataValue> choices = FormFieldValues.optionValues(field);
        if (!choices.isEmpty()) {
            ObjectNode choiceSchema = type == FormDataType.ARRAY ? (ObjectNode) schema.get("items") : schema;
            var choiceValidator = VALIDATOR.getSchema(choiceSchema, VALIDATION);
            for (FormDataValue choice : choices) {
                if (!choiceValidator.validate(jsonValue(choice)).isEmpty()) {
                    throw error(field, "option violates control constraints or format");
                }
            }
            ArrayNode alternatives = choiceSchema.putArray("oneOf");
            for (int i = 0; i < choices.size(); i++) {
                ObjectNode alternative = alternatives.addObject();
                alternative.set("const", jsonValue(choices.get(i)));
                String label = field.getOptions().get(i).getLabel();
                if (label != null) {
                    alternative.put("title", label);
                }
            }
        }
        if (field.getType() == FormFieldType.PASSWORD
                && (field.getTypedDefaultValue() != null || hasText(field.getDefaultValue()))) {
            throw error(field, "password defaults must not be exported");
        }
        FormFieldValues.defaultValue(field).ifPresent(value -> {
            JsonNode defaultValue = jsonValue(value);
            validateDefault(field, schema, defaultValue);
            if (!VALIDATOR.getSchema(schema, VALIDATION).validate(defaultValue).isEmpty()) {
                throw error(field, "default violates control format or pattern");
            }
            schema.set("default", defaultValue);
        });
        return schema;
    }

    private void validateControl(FormFieldMetadata field) {
        if (field.getType() == null) {
            throw error(field, "unknown control type");
        }
        switch (field.getType()) {
            case AUTO, TEXT, TEXTAREA, NUMBER, RANGE, SELECT, RADIO, CHECKBOX, HIDDEN, PASSWORD,
                 EMAIL, URL, PHONE, COLOR, DATE, TIME, DATETIME_LOCAL -> { }
            default -> throw error(field, "unsupported control type " + field.getType());
        }
        switch (field.getType()) {
            case EMAIL, URL, PHONE, COLOR, DATE, TIME, DATETIME_LOCAL, PASSWORD -> {
                if (field.getDataType() != FormDataType.STRING) {
                    throw error(field, "string control requires a string dataType");
                }
            }
            default -> { }
        }
        if (field.getType() == FormFieldType.COLOR && field.isAlpha()) {
            throw error(field, "alpha color representation is unsupported");
        }
        if ((field.getType() == FormFieldType.NUMBER || field.getType() == FormFieldType.RANGE)
                && field.getDataType() != FormDataType.NUMBER && field.getDataType() != FormDataType.INTEGER) {
            throw error(field, "numeric control requires an integer or number dataType");
        }
        if (field.getDataType() == FormDataType.ARRAY
                && (field.getType() != FormFieldType.SELECT && field.getType() != FormFieldType.CHECKBOX
                || !field.isMultiple())) {
            throw error(field, "array dataType requires a multiple select or checkbox control");
        }
        if (field.isMultiple() && field.getDataType() != FormDataType.ARRAY) {
            throw error(field, "multiple control requires an array dataType");
        }
    }

    private void addControlConstraints(FormFieldMetadata field, ObjectNode schema) {
        switch (field.getType()) {
            case EMAIL -> schema.put("format", "email");
            case URL -> schema.put("format", "uri");
            case DATE -> {
                schema.put("format", "date");
                schema.put("pattern", "^" + DATE_PATTERN + "$(?![\\s\\S])");
            }
            case TIME -> schema.put("pattern", "^" + TIME_PATTERN + "$(?![\\s\\S])");
            case DATETIME_LOCAL -> schema.put("pattern", "^" + DATE_PATTERN + "T" + TIME_PATTERN + "$(?![\\s\\S])");
            case COLOR -> schema.put("pattern", "^#[0-9a-fA-F]{6}$(?![\\s\\S])");
            default -> { }
        }
    }

    private void validateDefault(FormFieldMetadata field, ObjectNode schema, JsonNode value) {
        if (value.isTextual()) {
            int length = value.textValue().codePointCount(0, value.textValue().length());
            if (schema.has("minLength") && length < schema.get("minLength").intValue()
                    || schema.has("maxLength") && length > schema.get("maxLength").intValue()) {
                throw error(field, "default violates string length constraints");
            }
        }
        if (value.isNumber()) {
            if (schema.has("minimum") && value.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0
                    || schema.has("maximum") && value.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0) {
                throw error(field, "default violates numeric bounds");
            }
        }
        if (schema.has("oneOf")) {
            boolean found = false;
            for (JsonNode alternative : schema.get("oneOf")) {
                if (canonical(alternative.get("const")).equals(canonical(value))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw error(field, "default is not a declared choice");
            }
        }
        if (value.isArray()) {
            Set<Object> unique = new HashSet<>();
            for (JsonNode item : value) {
                validateDefault(field, (ObjectNode) schema.get("items"), item);
                if (!unique.add(canonical(item))) {
                    throw error(field, "default contains duplicate selections");
                }
            }
        }
    }

    private static Object canonical(JsonNode value) {
        return value.isNumber() ? value.decimalValue().stripTrailingZeros() : value;
    }

    private static JsonNode jsonValue(FormDataValue value) {
        return switch (value.type()) {
            case STRING -> JSON.textNode((String) value.value());
            case INTEGER -> JSON.numberNode((BigInteger) value.value());
            case NUMBER -> JSON.numberNode((BigDecimal) value.value());
            case BOOLEAN -> JSON.booleanNode((Boolean) value.value());
            case ARRAY -> {
                ArrayNode array = JSON.arrayNode();
                for (Object item : (List<?>) value.value()) {
                    array.add(jsonValue((FormDataValue) item));
                }
                yield array;
            }
        };
    }

    private static ObjectNode typedSchema(FormDataType type) {
        return JSON.objectNode().put("type", type.name().toLowerCase(Locale.ROOT));
    }

    private static ObjectNode objectSchema() {
        ObjectNode object = JSON.objectNode().put("type", "object");
        object.putObject("properties");
        return object;
    }

    private static void require(ObjectNode object, String name) {
        ArrayNode required = object.has("required") ? (ArrayNode) object.get("required") : object.putArray("required");
        JsonNode value = JSON.textNode(name);
        for (JsonNode item : required) {
            if (item.equals(value)) {
                return;
            }
        }
        required.add(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private static JsonSchemaExportException error(FormFieldMetadata field, String feature) {
        return new JsonSchemaExportException(field.getName(), feature);
    }
}
