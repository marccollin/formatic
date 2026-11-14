package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.RangeInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

public class RangeInputHandler extends BaseFormFieldHandler<RangeInput> {

    public RangeInputHandler() {
        super(RangeInput.class);
    }

    @Override
    protected void processSpecificAttributes(RangeInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.RANGE);
        metadata.setPlaceholder("");
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setMin(annotation.min());
        metadata.setMax(annotation.max());
        metadata.setStep(annotation.step());
    }
}
