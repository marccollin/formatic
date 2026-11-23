package com.formatic.schema.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formatic.core.form.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultJsonFormExporterTest {
    private final DefaultJsonFormExporter exporter = new DefaultJsonFormExporter();

    @Test
    void exportsVersionedNeutralPresentationInPreparedOrder() throws Exception {
        var street = field("address.street", FormFieldType.TEXT, FormDataType.STRING);
        street.setPlaceholder("Street");
        street.setHelpText("Your street");
        street.setReadonly(true);
        street.setRequired(true);
        street.setCssClass("private-css");
        var name = field("ui:name", FormFieldType.TEXTAREA, FormDataType.STRING);
        name.setRows(4);
        name.setDisabled(true);
        var form = new FormDefinition("Customer", List.of(street, name));
        var result = exporter.export(form);
        assertThat(result.ui()).isEqualTo(new ObjectMapper().readTree("""
                {"version":1,"fields":[
                  {"path":"address.street","widget":"text","label":"address.street",
                   "placeholder":"Street","helpText":"Your street","disabled":false,"readOnly":true},
                  {"path":"ui:name","widget":"textarea","label":"ui:name",
                   "disabled":true,"readOnly":false,"rows":4}
                ]}
                """));
        var json = new ObjectMapper().valueToTree(result);
        assertThat(json.size()).isEqualTo(2);
        assertThat(json.has("schema")).isTrue();
        assertThat(json.has("ui")).isTrue();
        assertThat(json.toString()).doesNotContain("private-css", "ui:widget", "uiSchema");
        result.ui().removeAll();
        assertThat(exporter.export(form).ui().path("fields").size()).isEqualTo(2);
        assertThat(street.getName()).isEqualTo("address.street");
    }

    @ParameterizedTest
    @CsvSource({
            "TEXT, STRING, text", "TEXTAREA, STRING, textarea", "NUMBER, INTEGER, number",
            "EMAIL, STRING, email", "URL, STRING, url", "PHONE, STRING, phone",
            "COLOR, STRING, color", "DATE, STRING, date", "TIME, STRING, time",
            "DATETIME_LOCAL, STRING, datetime-local", "PASSWORD, STRING, password",
            "HIDDEN, INTEGER, hidden", "CHECKBOX, BOOLEAN, checkbox",
            "AUTO, BOOLEAN, checkbox", "AUTO, NUMBER, number", "AUTO, STRING, text"
    })
    void mapsNeutralWidgetIntent(FormFieldType control, FormDataType kind, String widget) {
        var result = exporter.export(new FormDefinition(null, List.of(field("value", control, kind))));
        assertThat(result.ui().at("/fields/0/widget").asText()).isEqualTo(widget);
    }

    @Test
    void mapsSingleAndMultipleChoices() {
        var select = field("choice", FormFieldType.SELECT, FormDataType.STRING);
        select.setOptions(List.of(new SelectRadioOption("a", "A")));
        assertWidget(select, "select");
        select.setType(FormFieldType.RADIO);
        assertWidget(select, "radio");
        select.setType(FormFieldType.SELECT);
        select.setDataType(FormDataType.ARRAY);
        select.setItemDataType(FormDataType.STRING);
        select.setMultiple(true);
        assertWidget(select, "multiselect");
        select.setType(FormFieldType.CHECKBOX);
        assertWidget(select, "checkboxes");
    }

    @Test
    void rejectsUnsupportedPresentationRatherThanDiscardingIt() {
        var field = field("name", FormFieldType.TEXT, FormDataType.STRING);
        field.setGroup("Contact");
        assertFailure(field, "group");
        field.setGroup("");
        field.setDisplayCondition("enabled");
        assertFailure(field, "displayCondition");
        field.setDisplayCondition("");
        field.setWrap("hard");
        assertFailure(field, "wrap");
        field.setWrap("");
        field.setRows(3);
        assertFailure(field, "rows");
        field.setRows(0);
        field.setCols(5);
        assertFailure(field, "cols");
    }

    @Test
    void rejectsAmbiguousChoicesAndWidgetTypes() {
        var choice = field("choice", FormFieldType.SELECT, FormDataType.STRING);
        assertFailure(choice, "prepared options");
        var option = new SelectRadioOption("a", "A");
        choice.setOptions(List.of(option));
        option.setSelected(true);
        assertFailure(choice, "selected");
        option.setSelected(false);
        option.setDisabled(true);
        assertFailure(choice, "disabled");
        option.setDisabled(false);
        option.setGroup("Group");
        assertFailure(choice, "grouped");
        assertFailure(field("checkbox", FormFieldType.CHECKBOX, FormDataType.STRING), "boolean");
        assertFailure(field("text", FormFieldType.TEXT, FormDataType.INTEGER), "string");
        assertFailure(field("range", FormFieldType.RANGE, FormDataType.NUMBER), "min and max");
    }

    @Test
    void emptyFormsHaveAnExplicitVersionAndFieldList() {
        var result = exporter.export(new FormDefinition(null, List.of()));
        assertThat(result.ui().path("version").intValue()).isEqualTo(1);
        assertThat(result.ui().path("fields").isArray()).isTrue();
        assertThat(result.ui().path("fields").isEmpty()).isTrue();
    }

    private void assertWidget(FormFieldMetadata field, String widget) {
        assertThat(exporter.export(new FormDefinition(null, List.of(field))).ui().at("/fields/0/widget").asText())
                .isEqualTo(widget);
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
