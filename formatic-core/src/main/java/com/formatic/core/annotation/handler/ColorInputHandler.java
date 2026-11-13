package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.ColorInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

public class ColorInputHandler extends BaseFormFieldHandler<ColorInput> {

    public ColorInputHandler() {
        super(ColorInput.class);
    }

    @Override
    protected void processSpecificAttributes(ColorInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.COLOR);
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setPlaceholder("");
        metadata.setAlpha(annotation.alpha());
    }
}
