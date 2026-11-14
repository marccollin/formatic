package com.formatic.spring.config;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.util.List;

public final class FormaticPropertiesTest$CustomerFormMetadata {
    public static List<FormFieldMetadata> getMetadata() {
        return List.of(new FormFieldMetadata("address.street", FormFieldType.TEXT, "Street", 1));
    }
}
