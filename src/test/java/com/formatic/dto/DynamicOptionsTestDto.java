package com.formatic.dto;

import com.formatic.form.FormField;
import com.formatic.form.FormFieldType;

public class DynamicOptionsTestDto {
    @FormField(type = FormFieldType.SELECT,
            optionsProvider = "getCities",
            order = 1)
    private String city;

    @FormField(type = FormFieldType.SELECT,
            optionsProvider = "getCategories",
            order = 2)
    private String category;
}
