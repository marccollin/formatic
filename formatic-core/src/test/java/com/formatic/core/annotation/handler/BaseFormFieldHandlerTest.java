package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.CommonFieldAttributes;
import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class BaseFormFieldHandlerTest {

    @Test
    void should_use_field_name_when_annotation_name_is_empty() throws NoSuchFieldException {
        // Given
        TestHandler handler = new TestHandler();
        Field field = TestEntity.class.getDeclaredField("fieldName");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertEquals("fieldName", metadata.getName());

        // If label is not specified in the dto, name is used and first letter is capitalized
        assertEquals("FieldName", metadata.getLabel());
        assertFalse(metadata.isRequired());
    }

    @Test
    void should_use_annotation_values_when_provided() throws NoSuchFieldException {
        // Given
        TestHandler handler = new TestHandler();
        Field field = TestEntity.class.getDeclaredField("customField");

        // When
        FormFieldMetadata metadata = handler.handle(field);

        // Then
        assertEquals("custom_name", metadata.getName());
        assertEquals("Custom Label", metadata.getLabel());
        assertTrue(metadata.isRequired());
    }

    // Test annotation
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
        String name() default "";
        String label() default "";
        boolean required() default false;
        boolean disabled() default false;
        String[] htmlAttributes() default {};
    }

    // Test Handler
    private static class TestHandler extends BaseFormFieldHandler<TestAnnotation> {
        public TestHandler() {
            super(TestAnnotation.class);
        }

        @Override
        protected void processSpecificAttributes(TestAnnotation annotation, FormFieldMetadata metadata, Field field) {
            // No special handling for this test
        }
    }

    // Test entity
    private static class TestEntity {
        @TestAnnotation
        private String fieldName;

        @TestAnnotation(name = "custom_name", label = "Custom Label", required = true)
        private String customField;
    }
}
