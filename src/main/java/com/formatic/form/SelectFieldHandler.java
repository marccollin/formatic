package com.formatic.form;

import com.formatic.service.OptionsProviderService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SelectFieldHandler implements FormFieldTypeHandler {

    private final OptionsProviderService optionsProviderService;

    public SelectFieldHandler(OptionsProviderService optionsProviderService) {
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.SELECT ||
                (annotation.type() == FormFieldType.AUTO && isSelectableField(field));
    }


    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.SELECT; // Par défaut, peut être surchargé
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {
        if (annotation.options().length > 0) {
            // Options statiques
            List<SelectRadioOption> options = parseStaticOptions(annotation.options());
            metadata.setOptions(options);
        } else if (StringUtils.hasText(annotation.optionsProvider())) {
            // Options dynamiques
            metadata.setOptionsProvider(annotation.optionsProvider());
            List<SelectRadioOption> dynamicOptions = optionsProviderService.loadOptions(annotation.optionsProvider());
            metadata.setOptions(dynamicOptions);
        }
    }

    private boolean isSelectableField(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) ||
                isEntity(field.getType());
    }

    private boolean isEntity(Class<?> clazz) {
        if (clazz.isPrimitive() ||
                clazz.getName().startsWith("java.") ||
                clazz.getName().startsWith("jakarta.") ||
                clazz.isEnum()) {
            return false;
        }

        return Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(field -> "id".equals(field.getName()));
    }

    private List<SelectRadioOption> parseStaticOptions(String[] optionsArray) {
        return Arrays.stream(optionsArray)
                .map(this::parseStaticOption)
                .collect(Collectors.toList());
    }

    private SelectRadioOption parseStaticOption(String optionStr) {
        if (optionStr.contains(":")) {
            String[] parts = optionStr.split(":", 2);
            return new SelectRadioOption(parts[0].trim(), parts[1].trim());
        }
        return new SelectRadioOption(optionStr.trim(), optionStr.trim());
    }
}

