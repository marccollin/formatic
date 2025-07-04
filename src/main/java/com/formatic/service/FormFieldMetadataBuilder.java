package com.formatic.service;

import com.formatic.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.form.FormFieldMetadata;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@Service
public class FormFieldMetadataBuilder {

    private final List<FormFieldAnnotationHandler<?>> handlers;

    public FormFieldMetadataBuilder(List<FormFieldAnnotationHandler<?>> handlers) {
        this.handlers = handlers;
    }

    public List<FormFieldMetadata> buildMetadata(Class<?> clazz) {
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
        return result;
    }

}
