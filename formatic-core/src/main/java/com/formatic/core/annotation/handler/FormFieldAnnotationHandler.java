package com.formatic.core.annotation.handler;

import com.formatic.core.form.FormFieldMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Interface for handling custom form field annotations.
 *
 * Implementations of this interface are responsible for:
 * <ul>
 *   <li>Determining whether a given field is supported via {@link #supports(Field)}</li>
 *   <li>Processing the field's annotation to generate corresponding {@link FormFieldMetadata}</li>
 * </ul>
 *
 * This abstraction allows dynamic form generation by mapping annotated fields
 * to their metadata representations in a flexible and extensible manner.
 *
 * @param <T> The annotation type this handler supports
 */
public interface FormFieldAnnotationHandler<T extends Annotation> {
    boolean supports(Field field);

    FormFieldMetadata handle(Field field);
}
