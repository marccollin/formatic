package com.formatic.core.annotation.handler;

import com.formatic.core.annotation.DateInput;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;

import java.lang.reflect.Field;

/**
 * Form field handler for fields annotated with {@link DateInput}.
 * <p>
 * This class extends {@link BaseFormFieldHandler} to process metadata specific to
 * date input fields. It maps the annotation properties to the corresponding
 * fields in {@link FormFieldMetadata}, such as:
 * <ul>
 *   <li>CSS class for styling</li>
 *   <li>Placeholder text</li>
 *   <li>Default value</li>
 *   <li>Readonly status</li>
 *   <li>Minimum and maximum allowed dates</li>
 * </ul>
 *
 * It sets the field type to {@link FormFieldType#DATE} to be used
 * in dynamic form rendering.
 */
public class DateInputHandler extends BaseFormFieldHandler<DateInput> {

    public DateInputHandler() {
        super(DateInput.class);
    }

    @Override
    protected void processSpecificAttributes(DateInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.DATE);
        metadata.setCssClass(annotation.cssClass());
        metadata.setPlaceholder(annotation.placeholder());
        metadata.setDefaultValue(annotation.defaultValue());
        metadata.setReadonly(annotation.readonly());
        metadata.setMinDate(annotation.min());
        metadata.setMaxDate(annotation.max());
    }

}
