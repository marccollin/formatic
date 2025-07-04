package com.formatic.annotation.handler;

import com.formatic.annotation.CommonFieldAttributes;
import com.formatic.annotation.SelectInput;
import com.formatic.annotation.TextInput;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import com.formatic.form.SelectRadioOption;
import com.formatic.service.OptionsProviderService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SelectInputHandler  extends BaseFormFieldHandler<SelectInput> {

    private final OptionsProviderService optionsProviderService;

    public SelectInputHandler(OptionsProviderService optionsProviderService) {
        super(SelectInput.class);
        this.optionsProviderService = optionsProviderService;
    }


    @Override
    protected void processSpecificAttributes(SelectInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.SELECT);

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

        metadata.setMultiple(annotation.multiple());

    }

    private List<SelectRadioOption> parseStaticOptions(String[] optionsArray){
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
