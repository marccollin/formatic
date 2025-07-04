package com.formatic.annotation.handler;

import com.formatic.annotation.NumberInput;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class NumberInputHandler extends BaseFormFieldHandler<NumberInput> {

    public NumberInputHandler() {
        super(NumberInput.class);
    }

    @Override
    protected void processSpecificAttributes(NumberInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.NUMBER);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setReadonly(annotation.readonly());
        metadata.setMin(annotation.min());
        metadata.setMax(annotation.max());
        metadata.setStep(annotation.step());
        metadata.setErrorMessage(annotation.errorMessage());
    }

}
