package com.formatic.core.builder;

import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.form.ConstraintValidationException;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NestedFormTest {
    @ParameterizedTest
    @EnumSource(FieldIdStyle.class)
    void flattensRepeatedAndDeepDtosWithoutChangingBindingNames(FieldIdStyle style) {
        var handlers = List.of(new TextInputHandler());
        var reflection = new ReflectionFormFieldMetadataBuilder(List.copyOf(handlers), style);
        var bytecode = new BytecodeFormFieldMetadataBuilder(List.copyOf(handlers), style);
        for (FormFieldMetadataBuilder builder : List.of(reflection, bytecode)) {
            List<FormFieldMetadata> fields = builder.buildMetadata(Customer.class);
            assertThat(fields).extracting(FormFieldMetadata::getName)
                    .containsExactly("name", "billing.street", "billing.location.city",
                            "shipping.street", "shipping.location.city");
            assertThat(fields).extracting(FormFieldMetadata::getId)
                    .containsExactly("name", style.format("billing.street"), style.format("billing.location.city"),
                            style.format("shipping.street"), style.format("shipping.location.city"));
            assertThat(builder.buildMetadata(Address.class)).extracting(FormFieldMetadata::getName)
                    .containsExactly("street", "location.city");
            assertThat(builder.buildMetadata(Customer.class)).isSameAs(fields);
        }
    }

    @Test
    void rejectsCyclesUnsupportedContainersAndConflictingAnnotations() {
        for (Class<?> type : List.of(Cycle.class, CollectionForm.class, ConflictingForm.class)) {
            for (FormFieldMetadataBuilder builder : List.of(
                    new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler())),
                    new BytecodeFormFieldMetadataBuilder(List.of(new TextInputHandler())))) {
                assertThatThrownBy(() -> builder.buildMetadata(type))
                        .isInstanceOf(ConstraintValidationException.class).hasMessageContaining("@NestedForm");
            }
        }
    }

    @Test
    void rejectsNamingCollisionsInsteadOfRenderingDuplicateIds() {
        var builder = new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler()), FieldIdStyle.SNAKE_CASE);
        assertThatThrownBy(() -> builder.buildMetadata(Collision.class))
                .isInstanceOf(ConstraintValidationException.class).hasMessageContaining("duplicate HTML ID billing_street");
    }

    public static class Customer {
        @TextInput private String name;
        @NestedForm private Address billing;
        @NestedForm private Address shipping;
        private Address ignored;
    }

    public static class Address {
        @TextInput private String street;
        @NestedForm private Location location;
    }

    public static class Location {
        @TextInput private String city;
    }

    static class Cycle {
        @NestedForm private Cycle child;
    }

    static class CollectionForm {
        @NestedForm private List<Address> addresses;
    }

    static class ConflictingForm {
        @NestedForm @TextInput private Address address;
    }

    static class Collision {
        @NestedForm private Address billing;
        @TextInput private String billing_street;
    }
}
