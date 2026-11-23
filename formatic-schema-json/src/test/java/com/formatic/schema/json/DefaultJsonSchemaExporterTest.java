package com.formatic.schema.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.annotation.NumberInput;
import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.handler.NumberInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FormDataType;
import com.formatic.core.form.FormDataValue;
import com.formatic.core.form.FormDefinition;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultJsonSchemaExporterTest {
    private final DefaultJsonSchemaExporter exporter = new DefaultJsonSchemaExporter();
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaFactory validator = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    @Test
    void exportsAndValidatesACompleteNestedForm() throws Exception {
        var name = field("name", FormFieldType.TEXT, FormDataType.STRING);
        name.setMinLength(2);
        name.setMaxLength(20);
        name.setRequired(true);
        var age = field("age", FormFieldType.NUMBER, FormDataType.INTEGER);
        age.setMin(0);
        age.setMax(120);
        age.setDefaultValue("0");
        var contact = field("contactMethod", FormFieldType.RADIO, FormDataType.STRING);
        contact.setOptions(List.of(new SelectRadioOption("email", "Email"), new SelectRadioOption("phone", "Phone")));
        var interests = field("interests", FormFieldType.SELECT, FormDataType.ARRAY);
        interests.setMultiple(true);
        interests.setItemDataType(FormDataType.STRING);
        interests.setOptions(List.of(new SelectRadioOption("java", "Java"), new SelectRadioOption("web", "Web")));
        var city = field("address.city", FormFieldType.TEXT, FormDataType.STRING);
        city.setMinLength(1);
        city.setRequired(true);
        ObjectNode schema = exporter.export(new FormDefinition("Contact", List.of(name, age, contact, interests, city)));

        assertThat(validator.getSchema(SchemaLocation.of(DefaultJsonSchemaExporter.DIALECT))
                .validate(schema)).isEmpty();
        assertThat(schema.path("$schema").asText()).isEqualTo(DefaultJsonSchemaExporter.DIALECT);
        assertThat(schema.path("title").asText()).isEqualTo("Contact");
        assertThat(schema.path("required")).isEqualTo(mapper.readTree("[\"name\",\"address\"]"));
        assertThat(schema.at("/properties/address/required")).isEqualTo(mapper.readTree("[\"city\"]"));
        assertThat(schema.at("/properties/age/default").bigIntegerValue()).isEqualTo(BigInteger.ZERO);
        assertThat(schema.at("/properties/contactMethod/oneOf/0/title").asText()).isEqualTo("Email");
        assertThat(schema.has("additionalProperties")).isFalse();
        assertValid(schema, """
                {"name":"Jo","age":0,"address":{"city":"Paris"},"unknown":"allowed"}
                """);
        for (String invalid : List.of(
                "{\"name\":\"J\",\"address\":{\"city\":\"Paris\"}}",
                "{\"name\":\"Jo\",\"age\":-1,\"address\":{\"city\":\"Paris\"}}",
                "{\"name\":\"Jo\",\"age\":1.5,\"address\":{\"city\":\"Paris\"}}",
                "{\"name\":\"Jo\"}",
                "{\"name\":\"Jo\",\"address\":{}}",
                "{\"name\":\"Jo\",\"age\":null,\"address\":{\"city\":\"Paris\"}}",
                "{\"name\":\"Jo\",\"interests\":[\"java\",\"java\"],\"address\":{\"city\":\"Paris\"}}",
                "{\"name\":\"Jo\",\"contactMethod\":\"post\",\"address\":{\"city\":\"Paris\"}}")) {
            assertThat(validator.getSchema(schema).validate(mapper.readTree(invalid))).isNotEmpty();
        }
    }

    @Test
    void emitsOnlyPresentConstraintsAndKeepsMetadataAndOutputsIndependent() {
        var field = field("name", FormFieldType.TEXT, FormDataType.STRING);
        var form = new FormDefinition(null, List.of(field));
        ObjectNode first = exporter.export(form);
        assertThat(first.at("/properties/name").has("maxLength")).isFalse();
        assertThat(first.has("title")).isFalse();
        assertThat(field.getTypedDefaultValue()).isNull();
        assertThat(field.getMaxLength()).isEqualTo(-1);
        ((ObjectNode) first.get("properties")).removeAll();
        assertThat(exporter.export(form).at("/properties/name/type").asText()).isEqualTo("string");

        field.setMinLength(0);
        field.setMaxLength(0);
        field.setTypedDefaultValue(new FormDataValue(FormDataType.STRING, ""));
        ObjectNode explicit = exporter.export(form);
        assertThat(explicit.at("/properties/name/minLength").intValue()).isZero();
        assertThat(explicit.at("/properties/name/maxLength").intValue()).isZero();
        assertThat(explicit.at("/properties/name/default").asText()).isEmpty();
    }

    @Test
    void preservesTypedNumericChoicesDefaultsAndDecimalPrecision() throws Exception {
        var field = field("amount", FormFieldType.SELECT, FormDataType.NUMBER);
        field.setOptions(List.of(new SelectRadioOption("0.1234567890123456789", "Precise")));
        field.setDefaultValue("0.12345678901234567890");
        ObjectNode schema = exporter.export(new FormDefinition(null, List.of(field)));
        assertThat(schema.at("/properties/amount/oneOf/0/const").decimalValue())
                .isEqualByComparingTo("0.1234567890123456789");
        assertThat(schema.at("/properties/amount/default").decimalValue())
                .isEqualByComparingTo("0.12345678901234567890");
        assertValid(schema, "{\"amount\":0.1234567890123456789}");
        assertThat(field.getOptions().getFirst().getValue()).isEqualTo("0.1234567890123456789");
    }

    @Test
    void exportsBooleanAndArrayDefaultsAndRejectsDuplicatesOrNonChoices() throws Exception {
        var enabled = field("enabled", FormFieldType.CHECKBOX, FormDataType.BOOLEAN);
        enabled.setDefaultValue("true");
        var choices = field("choices", FormFieldType.CHECKBOX, FormDataType.ARRAY);
        choices.setMultiple(true);
        choices.setItemDataType(FormDataType.NUMBER);
        choices.setOptions(List.of(new SelectRadioOption("1", "One")));
        var one = new FormDataValue(FormDataType.NUMBER, BigDecimal.ONE);
        choices.setTypedDefaultValue(new FormDataValue(FormDataType.ARRAY, List.of(one)));
        var form = new FormDefinition(null, List.of(enabled, choices));
        ObjectNode schema = exporter.export(form);
        assertThat(schema.at("/properties/enabled/default").booleanValue()).isTrue();
        assertValid(schema, "{\"enabled\":true,\"choices\":[1]}");

        choices.setTypedDefaultValue(new FormDataValue(FormDataType.ARRAY, List.of(one, one)));
        assertThatThrownBy(() -> exporter.export(form)).hasMessageContaining("choices: default contains duplicate");
        choices.setTypedDefaultValue(new FormDataValue(FormDataType.ARRAY,
                List.of(new FormDataValue(FormDataType.NUMBER, BigDecimal.TEN))));
        assertThatThrownBy(() -> exporter.export(form)).hasMessageContaining("choices: default is not a declared choice");
    }

    @Test
    void rejectsInvalidDefaultsAndUsesUnicodeCodePointLength() {
        var text = field("name", FormFieldType.TEXT, FormDataType.STRING);
        text.setMinLength(1);
        text.setMaxLength(1);
        text.setDefaultValue("\uD83D\uDE00");
        assertThat(exporter.export(new FormDefinition(null, List.of(text))).at("/properties/name/default").asText())
                .isEqualTo("\uD83D\uDE00");
        text.setDefaultValue("ab");
        assertThatThrownBy(() -> exporter.export(new FormDefinition(null, List.of(text))))
                .hasMessageContaining("name: default violates string length");
        var number = field("amount", FormFieldType.NUMBER, FormDataType.INTEGER);
        number.setMin(0);
        number.setDefaultValue("-1");
        assertThatThrownBy(() -> exporter.export(new FormDefinition(null, List.of(number))))
                .hasMessageContaining("amount: default violates numeric bounds");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a..b", ".a", "a.", ""})
    void rejectsMalformedPaths(String path) {
        assertThatThrownBy(() -> exporter.export(new FormDefinition(null,
                List.of(field(path, FormFieldType.TEXT, FormDataType.STRING)))))
                .isInstanceOf(JsonSchemaExportException.class);
    }

    @Test
    void rejectsBothCollisionOrdersAndDuplicateLeaves() {
        var leaf = field("address", FormFieldType.TEXT, FormDataType.STRING);
        var nested = field("address.city", FormFieldType.TEXT, FormDataType.STRING);
        for (var fields : List.of(List.of(leaf, nested), List.of(nested, leaf), List.of(nested, nested))) {
            assertThatThrownBy(() -> exporter.export(new FormDefinition(null, fields)))
                    .hasMessageContaining("collision");
        }
    }

    @Test
    void optionalNestedLeavesDoNotRequireTheirParents() throws Exception {
        ObjectNode schema = exporter.export(new FormDefinition(null,
                List.of(field("address.city", FormFieldType.TEXT, FormDataType.STRING))));
        assertThat(schema.has("required")).isFalse();
        assertValid(schema, "{}");
        assertValid(schema, "{\"address\":{}}");
    }

    @Test
    void escapesJsonPointerCharactersAsOrdinaryPropertyNames() {
        var field = field("a/b.~value", FormFieldType.TEXT, FormDataType.STRING);
        ObjectNode schema = exporter.export(new FormDefinition(null, List.of(field)));
        assertThat(schema.at("/properties/a~1b/properties/~0value/type").asText()).isEqualTo("string");
    }

    @Test
    void reportsUnsupportedOrInvalidMetadataWithFieldPaths() {
        var field = field("address.amount", FormFieldType.NUMBER, FormDataType.NUMBER);
        field.setStep(0.5);
        assertFailure(field, "step");
        field.setStep(Double.NaN);
        field.setMin(Double.POSITIVE_INFINITY);
        assertFailure(field, "min");
        field.setMin(Double.NaN);
        field.setPattern("[a-z]+");
        assertFailure(field, "pattern");
        field.setPattern("");
        field.setMinDate("2020-01-01");
        assertFailure(field, "temporal");
        field.setMinDate("");
        field.setDataType(null);
        assertFailure(field, "dataType");
        field.setDataType(FormDataType.NUMBER);
        field.setOptionsProvider("prices");
        assertFailure(field, "optionsProvider");
        field.setOptionsProvider("");
        field.setOptions(List.of(new SelectRadioOption("NaN", "Invalid")));
        assertFailure(field, "option");
    }

    @Test
    void reportsArrayTypeAndChoiceErrors() {
        var field = field("choices", FormFieldType.SELECT, FormDataType.ARRAY);
        assertFailure(field, "multiple");
        field.setMultiple(true);
        assertFailure(field, "itemDataType");
        field.setItemDataType(FormDataType.INTEGER);
        field.setOptions(List.of(new SelectRadioOption("1", "One"), new SelectRadioOption("1", "Again")));
        assertFailure(field, "duplicate");
        field.setOptions(List.of());
        field.setMaxLength(10);
        assertFailure(field, "length");
    }

    @Test
    void suppressesPasswordDefaultsAndRejectsUnimplementedControlFormats() {
        var password = field("password", FormFieldType.PASSWORD, FormDataType.STRING);
        password.setDefaultValue("private");
        assertFailure(password, "password defaults");
        for (var type : List.of(FormFieldType.FILE, FormFieldType.LABEL)) {
            assertFailure(field("unsupported", type, FormDataType.STRING), "unsupported control");
        }
    }

    @Test
    void mapsStringControlsAndRequiresFormatAssertionForClients() throws Exception {
        for (var type : List.of(FormFieldType.EMAIL, FormFieldType.URL, FormFieldType.PHONE,
                FormFieldType.COLOR, FormFieldType.DATE, FormFieldType.TIME, FormFieldType.DATETIME_LOCAL)) {
            var field = field("value", type, FormDataType.STRING);
            ObjectNode schema = exporter.export(new FormDefinition(null, List.of(field)));
            assertThat(schema.at("/properties/value/type").asText()).isEqualTo("string");
            assertThat(validator.getSchema(SchemaLocation.of(DefaultJsonSchemaExporter.DIALECT))
                    .validate(schema)).isEmpty();
            field.setDataType(FormDataType.INTEGER);
            assertFailure(field, "string dataType");
        }
        var email = field("value", FormFieldType.EMAIL, FormDataType.STRING);
        var url = field("value", FormFieldType.URL, FormDataType.STRING);
        var phone = field("value", FormFieldType.PHONE, FormDataType.STRING);
        assertThat(exporter.export(new FormDefinition(null, List.of(email))).at("/properties/value/format").asText())
                .isEqualTo("email");
        assertThat(exporter.export(new FormDefinition(null, List.of(url))).at("/properties/value/format").asText())
                .isEqualTo("uri");
        assertThat(exporter.export(new FormDefinition(null, List.of(phone))).at("/properties/value/format").isMissingNode())
                .isTrue();
        phone.setDefaultValue("+1 (555) 123-4567");
        assertValid(exporter.export(new FormDefinition(null, List.of(phone))), "{\"value\":\"+1 (555) 123-4567\"}");
    }

    @ParameterizedTest
    @CsvSource({
            "EMAIL, contact@example.com, not-an-email",
            "URL, https://example.com/path, relative/path",
            "DATE, 2024-02-29, 2023-02-29",
            "DATE, 2000-02-29, 1900-02-29",
            "DATE, 2024-04-30, 2024-04-31",
            "DATE, 9999-12-31, 0000-01-01",
            "TIME, 23:59, 24:00",
            "TIME, 09:30:59.123, 09:30:60",
            "TIME, 00:00:00.1, 00:00Z",
            "DATETIME_LOCAL, 2024-02-29T23:59, 2023-02-29T23:59",
            "DATETIME_LOCAL, 2000-02-29T09:30:00.123, 1900-02-29T09:30",
            "DATETIME_LOCAL, 2024-01-01T00:00, 2024-01-01T00:00Z",
            "COLOR, #aB12fF, #12345678",
            "COLOR, #000000, red"
    })
    void validatesControlValuesAndDefaults(FormFieldType type, String valid, String invalid) {
        var field = field("value", type, FormDataType.STRING);
        field.setDefaultValue(valid);
        ObjectNode schema = exporter.export(new FormDefinition(null, List.of(field)));
        var config = SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();
        assertThat(validator.getSchema(schema, config).validate(mapper.createObjectNode().put("value", valid))).isEmpty();
        assertThat(validator.getSchema(schema, config).validate(mapper.createObjectNode().put("value", invalid)))
                .isNotEmpty();
        field.setDefaultValue(invalid);
        assertFailure(field, "default violates control");
        if (type != FormFieldType.EMAIL && type != FormFieldType.URL) {
            field.setDefaultValue(valid + "\n");
            assertFailure(field, "default violates control");
        }
    }

    @Test
    void keepsExplicitTemporalConstraintsAndAlphaColorsUnsupported() {
        var date = field("birthday", FormFieldType.DATE, FormDataType.STRING);
        date.setMinDate("2020-01-01");
        assertFailure(date, "temporal");
        date.setMinDate("");
        date.setDateStep("any");
        assertFailure(date, "temporal");
        var color = field("color", FormFieldType.COLOR, FormDataType.STRING);
        color.setAlpha(true);
        assertFailure(color, "alpha");
    }

    @Test
    void rejectsChoicesThatConflictWithControlConstraints() {
        var email = field("email", FormFieldType.EMAIL, FormDataType.STRING);
        email.setOptions(List.of(new SelectRadioOption("not-an-email", "Invalid")));
        assertFailure(email, "option violates");
        var number = field("amount", FormFieldType.NUMBER, FormDataType.NUMBER);
        number.setMin(0);
        number.setOptions(List.of(new SelectRadioOption("-1", "Negative")));
        assertFailure(number, "option violates");
    }

    @Test
    void exportsMetadataFromRuntimeBuildersAndRetainsReadOnlyAndDescriptions() {
        var fields = new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler(), new NumberInputHandler()))
                .buildMetadata(Contact.class);
        fields.getFirst().setHelpText("Full name");
        ObjectNode schema = exporter.export(new FormDefinition("Contact", fields));
        assertThat(schema.at("/properties/name/readOnly").booleanValue()).isTrue();
        assertThat(schema.at("/properties/name/description").asText()).isEqualTo("Full name");
        assertThat(schema.at("/properties/age/type").asText()).isEqualTo("integer");
        assertThat(fields.get(1).getStep()).isNaN();
    }

    private void assertValid(ObjectNode schema, String instance) throws Exception {
        JsonNode data = mapper.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(instance);
        assertThat(validator.getSchema(schema).validate(data)).isEmpty();
    }

    private void assertFailure(FormFieldMetadata field, String feature) {
        assertThatThrownBy(() -> exporter.export(new FormDefinition(null, List.of(field))))
                .isInstanceOf(JsonSchemaExportException.class)
                .hasMessageContaining(field.getName()).hasMessageContaining(feature);
    }

    private static FormFieldMetadata field(String name, FormFieldType control, FormDataType dataType) {
        var field = new FormFieldMetadata(name, control, name, 0);
        field.setDataType(dataType);
        return field;
    }

    static class Contact {
        @TextInput(minLength = 2, readonly = true) private String name;
        @NumberInput(min = 0, max = 120) private int age;
    }
}
