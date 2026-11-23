package com.formatic.schema.rjsf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formatic.core.form.FormDataType;
import com.formatic.core.form.FormDefinition;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.schema.json.JsonSchemaExportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RjsfFormExporterTest {
    private final RjsfFormExporter exporter = new RjsfFormExporter();

    @Test
    void exportsNestedOrderAndPresentationWithoutMutatingMetadata() throws Exception {
        var city = field("address.city", FormFieldType.TEXT, FormDataType.STRING);
        city.setRequired(true);
        city.setPlaceholder("City");
        city.setHelpText("Your city");
        city.setReadonly(true);
        var name = field("name", FormFieldType.TEXT, FormDataType.STRING);
        name.setDisabled(true);
        var street = field("address.street", FormFieldType.TEXT, FormDataType.STRING);
        var definition = new FormDefinition("Contact", List.of(city, name, street));
        var result = exporter.export(definition);

        assertThat(result.uiSchema()).isEqualTo(new ObjectMapper().readTree("""
                {
                  "ui:order":["address","name"],
                  "address":{
                    "ui:order":["city","street"],
                    "city":{"ui:widget":"text","ui:placeholder":"City","ui:help":"Your city","ui:readonly":true},
                    "street":{"ui:widget":"text"}
                  },
                  "name":{"ui:widget":"text","ui:disabled":true}
                }
                """));
        assertThat(result.schema().at("/properties/address/required").get(0).asText()).isEqualTo("city");
        assertThat(result.schema().has("ui:order")).isFalse();
        assertThat(city.getName()).isEqualTo("address.city");
        result.uiSchema().removeAll();
        ((ObjectNode) result.schema().get("properties")).removeAll();
        assertThat(exporter.export(definition).uiSchema().size()).isEqualTo(3);
        assertThat(exporter.export(definition).schema().at("/properties/name/type").asText()).isEqualTo("string");
    }

    @ParameterizedTest
    @CsvSource({
            "TEXT, STRING, text", "TEXTAREA, STRING, textarea",
            "NUMBER, INTEGER, updown", "NUMBER, NUMBER, updown",
            "EMAIL, STRING, email", "URL, STRING, uri", "PHONE, STRING, text",
            "COLOR, STRING, color", "DATE, STRING, date", "TIME, STRING, text",
            "DATETIME_LOCAL, STRING, text", "PASSWORD, STRING, password",
            "HIDDEN, INTEGER, hidden", "CHECKBOX, BOOLEAN, checkbox",
            "AUTO, BOOLEAN, checkbox", "AUTO, INTEGER, updown", "AUTO, STRING, text"
    })
    void mapsWidgetsWithCompatibleDataKinds(FormFieldType control, FormDataType kind, String widget) {
        var field = field("value", control, kind);
        var result = exporter.export(new FormDefinition(null, List.of(field)));
        assertThat(result.uiSchema().at("/value/ui:widget").asText()).isEqualTo(widget);
        if (control == FormFieldType.DATETIME_LOCAL) {
            assertThat(result.schema().at("/properties/value/format").isMissingNode()).isTrue();
        }
    }

    @Test
    void mapsChoicesTextareaRowsAndExplicitRangeBounds() {
        var select = field("select", FormFieldType.SELECT, FormDataType.ARRAY);
        select.setMultiple(true);
        select.setItemDataType(FormDataType.STRING);
        select.setOptions(List.of(new SelectRadioOption("java", "Java")));
        var checkboxes = new FormFieldMetadata(select);
        checkboxes.setName("checkboxes");
        checkboxes.setType(FormFieldType.CHECKBOX);
        var radio = field("radio", FormFieldType.RADIO, FormDataType.INTEGER);
        radio.setOptions(List.of(new SelectRadioOption("1", "One")));
        var textarea = field("notes", FormFieldType.TEXTAREA, FormDataType.STRING);
        textarea.setRows(4);
        var range = field("range", FormFieldType.RANGE, FormDataType.NUMBER);
        range.setMin(0);
        range.setMax(100);
        var result = exporter.export(new FormDefinition(null, List.of(select, checkboxes, radio, textarea, range)));
        assertThat(result.uiSchema().at("/select/ui:widget").asText()).isEqualTo("select");
        assertThat(result.uiSchema().at("/checkboxes/ui:widget").asText()).isEqualTo("checkboxes");
        assertThat(result.uiSchema().at("/radio/ui:widget").asText()).isEqualTo("radio");
        assertThat(result.uiSchema().at("/notes/ui:options/rows").intValue()).isEqualTo(4);
        assertThat(result.uiSchema().at("/range/ui:widget").asText()).isEqualTo("range");
    }

    @Test
    void rejectsUnsupportedPresentationAndReservedBindingPaths() {
        var field = field("name", FormFieldType.TEXT, FormDataType.STRING);
        field.setGroup("Contact");
        assertFailure(field, "group");
        field.setGroup("");
        field.setDisplayCondition("enabled == true");
        assertFailure(field, "displayCondition");
        field.setDisplayCondition("");
        field.setName("address.ui:order");
        assertFailure(field, "reserved");
        field.setName("name");
        field.setCols(20);
        assertFailure(field, "cols/wrap");
        field.setCols(0);
        field.setRows(4);
        assertFailure(field, "rows");
    }

    @Test
    void rejectsOptionsWithUnmappedStateAndInvalidWidgetCombinations() {
        var field = field("choice", FormFieldType.SELECT, FormDataType.STRING);
        assertFailure(field, "prepared options");
        var option = new SelectRadioOption("first", "First");
        field.setOptions(List.of(option));
        option.setDisabled(true);
        assertFailure(field, "options");
        option.setDisabled(false);
        option.setGroup("Group");
        assertFailure(field, "options");
        option.setGroup("");
        option.setSelected(true);
        assertFailure(field, "explicit defaults");
        assertFailure(field("enabled", FormFieldType.CHECKBOX, FormDataType.STRING), "boolean");
        assertFailure(field("count", FormFieldType.TEXT, FormDataType.INTEGER), "string");
        assertFailure(field("range", FormFieldType.RANGE, FormDataType.NUMBER), "explicit min and max");
    }

    @Test
    void serializesCombinedDefinitionAndDoesNotExposeHtmlAttributes() throws Exception {
        var field = field("name", FormFieldType.TEXT, FormDataType.STRING);
        field.setCssClass("form-control");
        field.setHtmlAttributesString("data-private=\"value\"");
        field.setExtraProperties(java.util.Map.of("private", "value"));
        var result = exporter.export(new FormDefinition(null, List.of(field)));
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));
        assertThat(json.size()).isEqualTo(2);
        assertThat(json.has("schema")).isTrue();
        assertThat(json.has("uiSchema")).isTrue();
        assertThat(json.toString()).doesNotContain("private", "form-control");
        var empty = exporter.export(new FormDefinition(null, List.of()));
        assertThat(empty.uiSchema().isEmpty()).isTrue();
    }

    private void assertFailure(FormFieldMetadata field, String feature) {
        assertThatThrownBy(() -> exporter.export(new FormDefinition(null, List.of(field))))
                .isInstanceOf(JsonSchemaExportException.class)
                .hasMessageContaining(field.getName()).hasMessageContaining(feature);
    }

    private static FormFieldMetadata field(String name, FormFieldType control, FormDataType kind) {
        var field = new FormFieldMetadata(name, control, name, 0);
        field.setDataType(kind);
        return field;
    }
}
