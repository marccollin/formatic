package com.formatic.dto;

import com.formatic.form.FormField;
import com.formatic.form.FormFieldType;

import java.time.LocalDate;

public class BaseDto {
    @FormField(type = FormFieldType.TEXT, required = true, order = 1)
    protected String id;

    @FormField(type = FormFieldType.DATE, order = 2, label = "Created Date")
    protected LocalDate createdDate;
}
