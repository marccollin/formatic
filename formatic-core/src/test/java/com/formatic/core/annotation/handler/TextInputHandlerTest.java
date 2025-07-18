package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.TextInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TextInputHandlerTest {

    private TextInputHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TextInputHandler();
    }

    @Test
    void should_support_field_with_text_input_annotation() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("username");

        // When
        boolean supports = handler.supports(field);

        // Then
        assertTrue(supports);
    }

    @Test
    void should_not_support_field_without_text_input_annotation() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("age");

        // When
        boolean supports = handler.supports(field);

        // Then
        assertFalse(supports);
    }

    @Test
    void should_handle_text_input_field_with_default_values() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("username");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertNotNull(metadata);
        assertEquals(FormFieldType.TEXT, metadata.getType());
        assertEquals("username", metadata.getName());
        assertEquals("Username", metadata.getLabel());
        assertEquals("Enter username", metadata.getPlaceholder());
        assertEquals("user-input", metadata.getCssClass());
        assertTrue(metadata.isRequired());
        assertEquals(3, metadata.getMinLength());
        assertEquals(20, metadata.getMaxLength());
    }

    @Test
    void should_handle_text_input_field_with_custom_name_and_label() throws NoSuchFieldException {
        // Given
        Field field = TestEntity.class.getDeclaredField("email");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertEquals("userEmail", metadata.getName());
        assertEquals("Email Address", metadata.getLabel());
        assertEquals("email@example.com", metadata.getPlaceholder());
        assertFalse(metadata.isRequired());
    }

    // Test class to simulate an entity
    private static class TestEntity {
        @TextInput(
                placeholder = "Enter username",
                cssClass = "user-input",
                required = true,
                minLength = 3,
                maxLength = 20
        )
        private String username;

        @TextInput(
                name = "userEmail",
                label = "Email Address",
                placeholder = "email@example.com",
                required = false
        )
        private String email;

        private Integer age; // No annotation used
    }

}
