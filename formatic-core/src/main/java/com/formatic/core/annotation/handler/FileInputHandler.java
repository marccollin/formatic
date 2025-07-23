package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.FileInput;
import com.formatic.core.annotation.UrlInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link FileInput}.
 *
 * This class processes metadata for file  input fields in dynamic form generation.
 * It extracts and sets file-specific properties such as input pattern, placeholder,
 * min/max length, default value, CSS class, and error message.
 *
 * Maps the {@code @UrlInput} annotation to a {@link FormFieldMetadata} structure.
 */
public class FileInputHandler extends BaseFormFieldHandler<FileInput> {

    public FileInputHandler() {
        super(FileInput.class);
    }

    @Override
    protected void processSpecificAttributes(FileInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.FILE);
        metadata.setCssClass(annotation.cssClass());
        metadata.setAccept(annotation.accept());
        metadata.setMultiple(annotation.multiple());
        metadata.setErrorMessage(annotation.errorMessage());

    }

}
