package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.NumberInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Handler for fields annotated with {@link NumberInput}.
 * <p>
 * This class is responsible for extracting metadata from fields annotated
 * with {@code @NumberInput} and mapping it to a {@link FormFieldMetadata}
 * instance. It sets properties specific to number inputs, such as min, max,
 * step, default value, placeholder, and CSS class.
 * <p>
 * Used in dynamic form generation to support number-type fields.
 */
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
