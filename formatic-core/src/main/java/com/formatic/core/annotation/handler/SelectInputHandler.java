package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.SelectInput;
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
 * Handler for fields annotated with {@link SelectInput}.
 * <p>
 * This class maps {@code @SelectInput}-annotated fields to {@link FormFieldMetadata},
 * configuring select-specific attributes such as static or dynamic options and the
 * multiple-selection flag.
 * - If static options are defined in the annotation, they are parsed and applied.
 * - If a dynamic options provider is specified, options are loaded using
 *   {@link OptionsProviderService}.
 * - The {@code multiple} attribute determines whether the select supports multiple values.
 */
public class SelectInputHandler extends BaseFormFieldHandler<SelectInput> {

    private final OptionsProviderService optionsProviderService;

    public SelectInputHandler(OptionsProviderService optionsProviderService) {
        super(SelectInput.class);
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    protected void processSpecificAttributes(SelectInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.SELECT);

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

        metadata.setMultiple(annotation.multiple());

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
