package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.SelectInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.core.service.OptionsProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelectInputHandlerTest {

    @Mock
    private OptionsProviderService optionsProviderService;

    private SelectInputHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SelectInputHandler(optionsProviderService);
    }

    @Test
    void should_handle_select_with_static_options() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("country");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertEquals(FormFieldType.SELECT, metadata.getType());
        assertEquals("country", metadata.getName());
        assertEquals("Country", metadata.getLabel());
        assertFalse(metadata.isMultiple());

        List<SelectRadioOption> options = metadata.getOptions();
        assertEquals(3, options.size());
        assertEquals("fr", options.get(0).getValue());
        assertEquals("France", options.get(0).getLabel());
        assertEquals("us", options.get(1).getValue());
        assertEquals("United States", options.get(1).getLabel());
        assertEquals("ca", options.get(2).getValue());
        assertEquals("Canada", options.get(2).getLabel());
    }

    @Test
    void should_handle_select_with_dynamic_options() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("department");
        List<SelectRadioOption> dynamicOptions = Arrays.asList(
                new SelectRadioOption("it", "IT Department"),
                new SelectRadioOption("hr", "HR Department")
        );
        when(optionsProviderService.loadOptions("departmentProvider"))
                .thenReturn(dynamicOptions);

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertEquals(FormFieldType.SELECT, metadata.getType());
        assertEquals("departmentProvider", metadata.getOptionsProvider());
        assertEquals(2, metadata.getOptions().size());
        verify(optionsProviderService).loadOptions("departmentProvider");
    }

    @Test
    void should_handle_multiple_select() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("skills");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertTrue(metadata.isMultiple());
        assertEquals(2, metadata.getOptions().size());
    }

    @Test
    void should_parse_option_without_colon() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("simpleSelect");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        List<SelectRadioOption> options = metadata.getOptions();
        assertEquals(2, options.size());
        assertEquals("option1", options.get(0).getValue());
        assertEquals("option1", options.get(0).getLabel());
        assertEquals("option2", options.get(1).getValue());
        assertEquals("option2", options.get(1).getLabel());
    }

    private static class TestEntity {
        @SelectInput(options = {
                "fr:France",
                "us:United States",
                "ca:Canada"
        })
        private String country;

        @SelectInput(optionsProvider = "departmentProvider")
        private String department;

        @SelectInput(
                options = {"java:Java", "python:Python"},
                multiple = true
        )
        private String[] skills;

        @SelectInput(options = {"option1", "option2"})
        private String simpleSelect;
    }
}
