package com.formatic.core.builder;

import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormMetadataNaming;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder class responsible for creating form field metadata from a given class.
 * <p>
 * This class scans the declared fields of the target class and applies a list of
 * {@link FormFieldAnnotationHandler} instances to generate corresponding
 * {@link FormFieldMetadata} objects for fields supported by the handlers.
 * <p>
 * Metadata results are cached per class to optimize repeated metadata retrievals.
 */
public class ReflectionFormFieldMetadataBuilder  implements FormFieldMetadataBuilder{
    private final List<FormFieldAnnotationHandler<?>> handlers;
    private final FieldIdStyle idStyle;
    private final Map<Class<?>, List<FormFieldMetadata>> cache = new ConcurrentHashMap<>();

    public ReflectionFormFieldMetadataBuilder(List<FormFieldAnnotationHandler<?>> handlers) {
        this(handlers, FieldIdStyle.DOT);
    }

    public ReflectionFormFieldMetadataBuilder(List<FormFieldAnnotationHandler<?>> handlers, FieldIdStyle idStyle) {
        this.handlers = handlers;
        this.idStyle = java.util.Objects.requireNonNull(idStyle, "idStyle");
    }

    public List<FormFieldMetadata> buildMetadata(Class<?> clazz) {
        return cache.computeIfAbsent(clazz, this::buildMetadataInternal);
    }

    private List<FormFieldMetadata> buildMetadataInternal(Class<?> clazz) {
        return FormMetadataNaming.apply(ReflectionMetadataScanner.scan(clazz, handlers), idStyle, clazz.getName());
    }
}
