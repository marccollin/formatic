package com.formatic.core.builder;

import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.core.form.ConstraintValidationException;
import com.formatic.core.form.FormConstraintValidator;
import com.formatic.core.form.FormFieldMetadata;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ReflectionMetadataScanner {
    private ReflectionMetadataScanner() {}

    static List<FormFieldMetadata> scan(Class<?> type, List<FormFieldAnnotationHandler<?>> handlers) {
        List<FormFieldMetadata> result = new ArrayList<>();
        collect(type, "", type.getName(), handlers, new HashSet<>(), result);
        return result;
    }

    private static void collect(Class<?> type, String prefix, String root,
                                List<FormFieldAnnotationHandler<?>> handlers,
                                Set<Class<?>> ancestors, List<FormFieldMetadata> result) {
        if (!ancestors.add(type)) {
            throw new ConstraintValidationException(root + "." + prefix + ": cyclic @NestedForm reference");
        }
        List<FormFieldMetadata> local = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            FormFieldAnnotationHandler<?> handler = handlers.stream()
                    .filter(candidate -> candidate.supports(field)).findFirst().orElse(null);
            if (field.isAnnotationPresent(NestedForm.class)) {
                String path = prefix + field.getName();
                Class<?> nestedType = field.getType();
                if (handler != null || Modifier.isStatic(field.getModifiers())
                        || nestedType.isPrimitive() || nestedType.isArray() || nestedType.isEnum()
                        || nestedType.isInterface() || nestedType.isRecord()
                        || Modifier.isAbstract(nestedType.getModifiers())
                        || nestedType.getPackageName().startsWith("java.")
                        || nestedType.getPackageName().startsWith("javax.")
                        || java.util.Collection.class.isAssignableFrom(nestedType)
                        || java.util.Map.class.isAssignableFrom(nestedType)) {
                    throw new ConstraintValidationException(root + "." + path
                            + ": @NestedForm requires a single concrete DTO without an input annotation");
                }
                collect(nestedType, path + ".", root, handlers, ancestors, local);
            } else if (handler != null) {
                FormFieldMetadata metadata = handler.handle(field);
                if (metadata != null) {
                    metadata.setName(prefix + metadata.getName());
                    FormConstraintValidator.validate(metadata, root + "." + prefix + field.getName());
                    local.add(metadata);
                }
            }
        }
        ancestors.remove(type);
        local.sort(Comparator.comparingInt(metadata ->
                metadata.getOrder() == 0 ? Integer.MAX_VALUE : metadata.getOrder()));
        result.addAll(local);
    }
}
