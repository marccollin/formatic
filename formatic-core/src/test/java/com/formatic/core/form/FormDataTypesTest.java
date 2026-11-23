package com.formatic.core.form;

import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FormDataTypesTest {
    @Test
    void infersValueKindsIndependentlyOfWidgetsAcrossRuntimeBuilders() {
        var handler = new TextInputHandler();
        var reflection = new ReflectionFormFieldMetadataBuilder(List.of(handler)).buildMetadata(Values.class);
        var bytecode = new BytecodeFormFieldMetadataBuilder(List.of(handler)).buildMetadata(Values.class);

        assertThat(bytecode).usingRecursiveComparison()
                .withComparatorForType(Double::compare, Double.class).isEqualTo(reflection);
        assertThat(reflection).extracting(FormFieldMetadata::getDataType).containsExactly(
                FormDataType.STRING, FormDataType.INTEGER, FormDataType.NUMBER, FormDataType.BOOLEAN,
                FormDataType.STRING, FormDataType.ARRAY, FormDataType.ARRAY, FormDataType.ARRAY, null);
        assertThat(reflection).extracting(FormFieldMetadata::getItemDataType).containsExactly(
                null, null, null, null, null, FormDataType.INTEGER, FormDataType.STRING, null, null);
        assertThat(reflection).extracting(FormFieldMetadata::getType).containsOnly(FormFieldType.TEXT);
    }

    @Test
    void supportsDirectHandlerUseAndPreservesCopies() throws Exception {
        var field = new TextInputHandler().handle(Values.class.getDeclaredField("numbers"));
        var copy = new FormFieldMetadata(field);

        assertThat(copy.getDataType()).isEqualTo(FormDataType.ARRAY);
        assertThat(copy.getItemDataType()).isEqualTo(FormDataType.INTEGER);
        assertThat(copy.getType()).isEqualTo(FormFieldType.TEXT);
    }

    @Test
    void preservesAnExplicitHandlerMapping() throws Exception {
        var handler = new TextInputHandler() {
            @Override
            protected void processSpecificAttributes(TextInput annotation, FormFieldMetadata metadata,
                                                     java.lang.reflect.Field field) {
                super.processSpecificAttributes(annotation, metadata, field);
                metadata.setDataType(FormDataType.STRING);
            }
        };

        var result = new ReflectionFormFieldMetadataBuilder(List.of(handler)).buildMetadata(Values.class);

        assertThat(result).extracting(FormFieldMetadata::getDataType).containsOnly(FormDataType.STRING);
        assertThat(result).extracting(FormFieldMetadata::getItemDataType).containsOnlyNulls();
    }

    @Test
    void unknownTypesRequireAnExplicitMapping() {
        assertThat(FormDataTypes.scalar("java.lang.Object", false)).isEmpty();
        assertThat(FormDataTypes.scalar("java.lang.Number", false)).isEmpty();
        assertThat(FormDataTypes.scalar("java.math.BigInteger", false)).contains(FormDataType.INTEGER);
        assertThat(FormDataTypes.scalar("java.time.LocalDate", false)).contains(FormDataType.STRING);
        assertThat(new FormFieldMetadata().getDataType()).isNull();
    }

    public static class Values {
        @TextInput private String text;
        @TextInput private int count;
        @TextInput private BigDecimal amount;
        @TextInput private boolean checked;
        @TextInput private Choice choice;
        @TextInput private List<Integer> numbers;
        @TextInput private String[] names;
        @TextInput private List<?> unknownItems;
        @TextInput private Map<String, String> unsupported;
    }

    enum Choice { FIRST, SECOND }
}
