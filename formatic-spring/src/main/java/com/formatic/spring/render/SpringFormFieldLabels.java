package com.formatic.spring.render;

import com.formatic.core.form.FormFieldLabels;
import com.formatic.core.form.FormFieldMetadata;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Resolves Formatic label keys through the application's Spring MessageSource. */
public final class SpringFormFieldLabels {
    private SpringFormFieldLabels() {
    }

    public static List<FormFieldMetadata> resolve(List<FormFieldMetadata> fields, MessageSource messages, Locale locale) {
        Objects.requireNonNull(messages, "messages");
        return FormFieldLabels.resolve(fields, locale,
                (key, requestedLocale) -> Optional.ofNullable(messages.getMessage(key, null, null, requestedLocale)));
    }
}
