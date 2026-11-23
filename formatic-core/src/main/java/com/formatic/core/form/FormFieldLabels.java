package com.formatic.core.form;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;

/** Resolves explicitly marked labels without modifying cached metadata. */
public final class FormFieldLabels {
    private static final System.Logger LOG = System.getLogger(FormFieldLabels.class.getName());

    private FormFieldLabels() {
    }

    /**
     * Resolve labels of the form {@code #{key}} using an application-provided lookup.
     * Missing messages use the capitalized final segment of the submitted field name.
     */
    public static List<FormFieldMetadata> resolve(List<FormFieldMetadata> fields, Locale locale,
            BiFunction<String, Locale, Optional<String>> messages) {
        Objects.requireNonNull(fields, "fields");
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(messages, "messages");
        return fields.stream().map(field -> {
            var copy = new FormFieldMetadata(field);
            String label = field.getLabel();
            if (label != null && label.startsWith("#{") && label.endsWith("}") && label.length() > 3) {
                String key = label.substring(2, label.length() - 1);
                Optional<String> value = Objects.requireNonNull(messages.apply(key, locale), "message result");
                if (value.isPresent()) {
                    copy.setLabel(value.get());
                } else {
                    LOG.log(System.Logger.Level.WARNING, "Missing form label message ''{0}'' for locale {1}; using field name", key, locale);
                    String name = field.getName();
                    String last = name.substring(name.lastIndexOf('.') + 1);
                    copy.setLabel(last.isEmpty() ? name : last.substring(0, 1).toUpperCase(Locale.ROOT) + last.substring(1));
                }
            }
            return copy;
        }).toList();
    }

    /** Resolve labels from the application's standard Java resource bundle. */
    public static List<FormFieldMetadata> resolveBundle(List<FormFieldMetadata> fields, Locale locale, String baseName) {
        Objects.requireNonNull(baseName, "baseName");
        return resolve(fields, locale, (key, requestedLocale) -> {
            try {
                return Optional.of(ResourceBundle.getBundle(baseName, requestedLocale,
                        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_DEFAULT)).getString(key));
            } catch (MissingResourceException e) {
                return Optional.empty();
            }
        });
    }
}
