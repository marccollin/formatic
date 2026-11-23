package com.formatic.core.form;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormFieldValuesTest {
    @Test
    void convertsScalarStringsWithoutGuessingTypesOrLosingPrecision() {
        assertThat(FormDataValue.fromText(FormDataType.STRING, "001", "name").value()).isEqualTo("001");
        assertThat(FormDataValue.fromText(FormDataType.INTEGER, "9007199254740993", "count").value())
                .isEqualTo(new BigInteger("9007199254740993"));
        assertThat(FormDataValue.fromText(FormDataType.NUMBER, "0.1234567890123456789", "amount").value())
                .isEqualTo(new BigDecimal("0.1234567890123456789"));
        assertThat(FormDataValue.fromText(FormDataType.BOOLEAN, "false", "checked").value()).isEqualTo(false);
    }

    @Test
    void rejectsInvalidScalarValuesWithTheirLocation() {
        for (String invalid : List.of("1.5", "01", "+1", " 1", "NaN", "Infinity")) {
            assertThatThrownBy(() -> FormDataValue.fromText(FormDataType.INTEGER, invalid, "address.count"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("address.count");
        }
        assertThatThrownBy(() -> FormDataValue.fromText(FormDataType.BOOLEAN, "yes", "checked"))
                .hasMessageContaining("checked");
        assertThatThrownBy(() -> FormDataValue.fromText(FormDataType.NUMBER, "NaN", "amount"))
                .hasMessageContaining("amount");
    }

    @Test
    void distinguishesAbsentAndExplicitEmptyDefaultsWithoutChangingHtml() {
        var field = field(FormDataType.STRING);
        field.setDefaultValue("");
        assertThat(FormFieldValues.defaultValue(field)).isEmpty();

        var empty = new FormDataValue(FormDataType.STRING, "");
        field.setTypedDefaultValue(empty);

        assertThat(FormFieldValues.defaultValue(field)).contains(empty);
        assertThat(new FormFieldMetadata(field).getTypedDefaultValue()).isEqualTo(empty);
        assertThat(field.getDefaultValue()).isEmpty();
        field.setTypedDefaultValue(null);
        assertThat(FormFieldValues.defaultValue(field)).isEmpty();
    }

    @Test
    void convertsLegacyDefaultsAndPreservesHtmlStrings() {
        var field = field(FormDataType.INTEGER);
        field.setDefaultValue("0");

        assertThat(FormFieldValues.defaultValue(field))
                .contains(new FormDataValue(FormDataType.INTEGER, BigInteger.ZERO));
        assertThat(field.getDefaultValue()).isEqualTo("0");
        assertThat(field.getTypedDefaultValue()).isNull();
    }

    @Test
    void copiesArrayDefaultsAndChecksItemTypes() {
        var field = field(FormDataType.ARRAY);
        field.setItemDataType(FormDataType.STRING);
        var items = new ArrayList<>(List.of(new FormDataValue(FormDataType.STRING, "java")));
        var defaultValue = new FormDataValue(FormDataType.ARRAY, items);
        field.setTypedDefaultValue(defaultValue);
        items.clear();

        assertThat(FormFieldValues.defaultValue(field)).contains(defaultValue);
        assertThat(defaultValue.value()).isEqualTo(List.of(new FormDataValue(FormDataType.STRING, "java")));
        field.setItemDataType(FormDataType.INTEGER);
        assertThatThrownBy(() -> FormFieldValues.defaultValue(field))
                .hasMessageContaining("name: default item");
        field.setItemDataType(null);
        field.setTypedDefaultValue(new FormDataValue(FormDataType.ARRAY, List.of()));
        assertThatThrownBy(() -> FormFieldValues.defaultValue(field))
                .hasMessageContaining("unknown data type");
    }

    @Test
    void rejectsMismatchedAndAmbiguousDefaults() {
        var field = field(FormDataType.INTEGER);
        field.setTypedDefaultValue(new FormDataValue(FormDataType.STRING, "0"));
        assertThatThrownBy(() -> FormFieldValues.defaultValue(field))
                .hasMessageContaining("name: default");
        field.setTypedDefaultValue(null);
        field.setDataType(FormDataType.ARRAY);
        field.setDefaultValue("java,web");
        assertThatThrownBy(() -> FormFieldValues.defaultValue(field))
                .hasMessageContaining("name: default");
        assertThatThrownBy(() -> new FormDataValue(FormDataType.INTEGER, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FormDataValue(FormDataType.ARRAY, List.of("java")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsChoiceItemsWithoutMutatingOriginalOptions() {
        var field = field(FormDataType.ARRAY);
        field.setItemDataType(FormDataType.INTEGER);
        var option = new SelectRadioOption("0", "Zero");
        option.setSelected(true);
        field.setOptions(List.of(option));

        assertThat(FormFieldValues.optionValues(field))
                .containsExactly(new FormDataValue(FormDataType.INTEGER, BigInteger.ZERO));
        assertThat(option.getValue()).isEqualTo("0");
        assertThat(option.getLabel()).isEqualTo("Zero");
        assertThat(option.isSelected()).isTrue();
        assertThat(field.getTypedDefaultValue()).isNull();
    }

    @Test
    void detectsNumericallyEquivalentChoicesAndUnknownKinds() {
        var field = field(FormDataType.NUMBER);
        field.setOptions(List.of(new SelectRadioOption("1.0", "One"), new SelectRadioOption("1.00", "Again")));
        assertThatThrownBy(() -> FormFieldValues.optionValues(field))
                .hasMessageContaining("name: duplicate typed option");
        field.setDataType(null);
        assertThatThrownBy(() -> FormFieldValues.optionValues(field))
                .hasMessageContaining("name: unknown data type");
    }

    private static FormFieldMetadata field(FormDataType dataType) {
        var field = new FormFieldMetadata("name", FormFieldType.TEXT, "Name", 1);
        field.setDataType(dataType);
        return field;
    }
}
