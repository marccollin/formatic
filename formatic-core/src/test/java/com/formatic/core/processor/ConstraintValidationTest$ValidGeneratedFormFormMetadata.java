package com.formatic.core.processor;

import com.formatic.core.annotation.handler.DateInputHandler;
import com.formatic.core.annotation.handler.NumberInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.form.FormFieldMetadata;
import java.util.List;

public class ConstraintValidationTest$ValidGeneratedFormFormMetadata {
    public static List<FormFieldMetadata> getMetadata() throws NoSuchFieldException {
        Class<?> form = ConstraintValidationTest.ValidGeneratedForm.class;
        return List.of(
                new TextInputHandler().handle(form.getDeclaredField("name")),
                new NumberInputHandler().handle(form.getDeclaredField("amount")),
                new DateInputHandler().handle(form.getDeclaredField("date")));
    }
}
