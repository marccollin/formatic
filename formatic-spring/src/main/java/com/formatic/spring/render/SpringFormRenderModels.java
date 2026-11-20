package com.formatic.spring.render;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.render.FormRenderModel;
import com.formatic.render.RenderField;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Bridges Spring MVC binding state to an engine-neutral render model. */
public final class SpringFormRenderModels {
    private SpringFormRenderModels() {}

    public static FormRenderModel fromBinding(String title, String action, List<FormFieldMetadata> fields,
                                               BindingResult binding, MessageSource messages, Locale locale,
                                               String successMessage, Map<String, String> hiddenFields) {
        var renderedFields = fields.stream().map(field -> {
            var errors = binding.getFieldErrors(field.getName());
            return new RenderField(field, values(field, binding, errors), errors.stream()
                    .map(error -> messages.getMessage(error, locale)).toList());
        }).toList();
        var globalErrors = binding.getGlobalErrors().stream().map(error -> messages.getMessage(error, locale)).toList();
        return new FormRenderModel(title, action, renderedFields, globalErrors, successMessage, hiddenFields);
    }

    private static List<String> values(FormFieldMetadata field, BindingResult binding, List<FieldError> errors) {
        if (field.getType() == FormFieldType.PASSWORD || field.getType() == FormFieldType.FILE
                || field.getType() == FormFieldType.LABEL) return null;
        Object value;
        var rejected = errors.stream().filter(FieldError::isBindingFailure).findFirst();
        if (rejected.isPresent()) {
            value = rejected.get().getRejectedValue();
        } else {
            try {
                value = binding.getRawFieldValue(field.getName());
            } catch (NullValueInNestedPathException e) {
                value = null;
            }
        }
        if (value == null) return rejected.isPresent() ? List.of() : null;
        if (value instanceof Iterable<?> items) {
            List<String> values = new ArrayList<>();
            for (Object item : items) {
                if (item != null) values.add(format(item));
            }
            return values;
        }
        if (value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                Object item = Array.get(value, i);
                if (item != null) values.add(format(item));
            }
            return values;
        }
        return List.of(format(value));
    }

    private static String format(Object value) {
        return switch (value) {
            case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.format(date);
            case LocalDateTime dateTime -> dateTime.toString();
            case LocalTime time -> time.toString();
            case Enum<?> option -> option.name();
            default -> Objects.toString(value);
        };
    }
}
