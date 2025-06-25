package com.formatic.form;

import com.formatic.service.OptionsProviderService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RadioFieldHandler implements FormFieldTypeHandler {

    private final OptionsProviderService optionsProviderService;

    public RadioFieldHandler(OptionsProviderService optionsProviderService) {
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.RADIO;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.RADIO;
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
