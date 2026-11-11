package com.formatic.core.processor;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import java.util.List;

public class ConstraintValidationTest$InvalidGeneratedFormFormMetadata {
    public static List<FormFieldMetadata> getMetadata() {
        FormFieldMetadata metadata = new FormFieldMetadata("amount", FormFieldType.NUMBER, "Amount", 0);
        metadata.setMin(Double.NaN);
        metadata.setMax(Double.NaN);
        metadata.setStep(-1);
        return List.of(metadata);
    }
}
