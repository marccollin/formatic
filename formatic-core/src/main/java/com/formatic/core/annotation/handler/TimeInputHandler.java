package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.TimeInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

public class TimeInputHandler extends BaseFormFieldHandler<TimeInput> {

    public TimeInputHandler() {
        super(TimeInput.class);
    }

    @Override
    protected void processSpecificAttributes(TimeInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.TIME);
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinTime(annotation.min());
        metadata.setMaxTime(annotation.max());
        metadata.setTimeStep(annotation.step());
    }
}
