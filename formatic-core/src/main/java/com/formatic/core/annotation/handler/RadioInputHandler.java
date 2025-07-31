package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.RadioInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.core.service.OptionsProviderService;
import com.formatic.core.utils.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for fields annotated with {@link RadioInput}.
 * <p>
 * This class is responsible for mapping {@code @RadioInput}-annotated fields to
 * {@link FormFieldMetadata} by handling radio-specific attributes such as static or
 * dynamic options and default values.
 * <p>
 * If static options are provided via the annotation, they are parsed directly.
 * If a dynamic options provider is specified, options are loaded using the
 * {@link OptionsProviderService}.
 */
public class RadioInputHandler extends BaseFormFieldHandler<RadioInput> {

    private final OptionsProviderService optionsProviderService;

    public RadioInputHandler(OptionsProviderService optionsProviderService) {
        super(RadioInput.class);
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    protected void processSpecificAttributes(RadioInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.RADIO);

        if (annotation.options().length > 0) {
            // Static Options
            List<SelectRadioOption> options = parseStaticOptions(annotation.options());
            metadata.setOptions(options);
        } else if (StringUtils.hasText(annotation.optionsProvider())) {
            // Dynamic Options
            metadata.setOptionsProvider(annotation.optionsProvider());
            List<SelectRadioOption> dynamicOptions = optionsProviderService.loadOptions(annotation.optionsProvider());
            metadata.setOptions(dynamicOptions);
        }

        metadata.setDefaultValue(annotation.defaultValue());
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
