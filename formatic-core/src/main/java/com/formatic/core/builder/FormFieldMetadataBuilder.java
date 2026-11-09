package com.formatic.core.builder;

import com.formatic.core.form.FormFieldMetadata;

import java.util.List;

/**
 * Interface defining the contract for all construction strategies of form field metadata
 */
public interface FormFieldMetadataBuilder {

    /**
     * Constructs and returns the metadata list for the fields of the specified class
     * * @param clazz The form class to analyze.
     * @return The sorted list of metadata for its fields.
     */
    List<FormFieldMetadata> buildMetadata(Class<?> clazz);
}
