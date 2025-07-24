package com.formatic.core.form;

import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Builder class responsible for creating form field metadata from a given class.
 * <p>
 * This class scans the declared fields of the target class and applies a list of
 * {@link FormFieldAnnotationHandler} instances to generate corresponding
 * {@link FormFieldMetadata} objects for fields supported by the handlers.
 * <p>
 * Metadata results are cached per class to optimize repeated metadata retrievals.
 */
public class FormFieldMetadataBuilder {

    private final List<FormFieldAnnotationHandler<?>> handlers;
    private final Map<Class<?>, List<FormFieldMetadata>> cache = new ConcurrentHashMap<>();

    public FormFieldMetadataBuilder(List<FormFieldAnnotationHandler<?>> handlers) {
        this.handlers = handlers;
    }

    public List<FormFieldMetadata> buildMetadata(Class<?> clazz) {
        return cache.computeIfAbsent(clazz, this::buildMetadataInternal);
    }

    private List<FormFieldMetadata> buildMetadataInternal(Class<?> clazz) {
        List<FormFieldMetadata> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            handlers.stream()
                    .filter(h -> h.supports(field))
                    .findFirst()
                    .ifPresent(handler -> {
                        FormFieldMetadata m = handler.handle(field);
                        if (m != null) result.add(m);
                    });
        }

        //sort
        return result.stream()
                .sorted(Comparator.comparingInt((FormFieldMetadata m) ->
                        m.getOrder() == 0 ? Integer.MAX_VALUE : m.getOrder()))
                .collect(Collectors.toList());

    }
}
