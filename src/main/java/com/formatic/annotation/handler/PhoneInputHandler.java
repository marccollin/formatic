package com.formatic.annotation.handler;

import com.formatic.annotation.PhoneInput;
import com.formatic.annotation.TextInput;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class PhoneInputHandler extends BaseFormFieldHandler<PhoneInput> {

    protected PhoneInputHandler() {
        super(PhoneInput.class);
    }

    @Override
    protected void processSpecificAttributes(PhoneInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.PHONE);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setPattern(annotation.pattern());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinLength(annotation.minLength());
        metadata.setMaxLength(annotation.maxLength());
        metadata.setPattern(annotation.pattern());
        metadata.setErrorMessage(annotation.errorMessage());
    }
}
