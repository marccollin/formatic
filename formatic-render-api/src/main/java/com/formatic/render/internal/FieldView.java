package com.formatic.render.internal;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.render.RenderField;

import java.util.List;

/** Shared presentation rules for the bundled renderers, not part of the public rendering contract. */
public record FieldView(RenderField field) {
    public FormFieldMetadata metadata() {
        return field.metadata();
    }

    public FormFieldType type() {
        return metadata().getType();
    }

    public String inputType() {
        return switch (type()) {
            case EMAIL -> "email";
            case PASSWORD -> "password";
            case PHONE -> "tel";
            case NUMBER -> "number";
            case URL -> "url";
            case FILE -> "file";
            case COLOR -> "color";
            case DATE -> "date";
            case DATETIME_LOCAL -> "datetime-local";
            case TIME -> "time";
            case RANGE -> "range";
            default -> "text";
        };
    }

    public String inputClass() {
        return controlClass(switch (type()) {
            case RANGE -> "form-range";
            case COLOR -> "form-control form-control-color";
            default -> "form-control";
        });
    }

    public String value() {
        if (type() == FormFieldType.FILE || type() == FormFieldType.PASSWORD) return null;
        if (field.values() == null) return metadata().getDefaultValue();
        return field.values().isEmpty() ? "" : field.values().getFirst();
    }

    public boolean selected(SelectRadioOption option) {
        if (field.values() != null) return field.values().contains(option.getValue());
        return metadata().getDefaultValue() != null && !metadata().getDefaultValue().isEmpty()
                ? metadata().getDefaultValue().equals(option.getValue()) : option.isSelected();
    }

    public boolean checked() {
        String value = value();
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value) || "1".equals(value);
    }

    public List<SelectRadioOption> options() {
        return metadata().getOptions() == null ? List.of() : metadata().getOptions();
    }

    public boolean needsMarker() {
        if (metadata().isDisabled()) return false;
        if (type() == FormFieldType.CHECKBOX && !metadata().isMultiple()) return true;
        return (type() == FormFieldType.CHECKBOX || type() == FormFieldType.SELECT)
                && metadata().isMultiple() && options().stream().anyMatch(option -> !option.isDisabled());
    }

    public boolean hasHelp() {
        return metadata().getHelpText() != null && !metadata().getHelpText().isBlank();
    }

    public boolean invalid() {
        return !field.errors().isEmpty();
    }

    public String describedBy() {
        String id = metadata().getId();
        if (hasHelp() && invalid()) return id + "-help " + id + "-errors";
        if (hasHelp()) return id + "-help";
        return invalid() ? id + "-errors" : null;
    }

    public String controlClass(String base) {
        String custom = metadata().getCssClass();
        return base + (invalid() ? " is-invalid" : "") + (custom == null || custom.isBlank() ? "" : " " + custom);
    }

    public boolean supportsLength() {
        return switch (type()) {
            case AUTO, TEXT, TEXTAREA, EMAIL, PASSWORD, PHONE, URL -> true;
            default -> false;
        };
    }

    public String min() {
        return switch (type()) {
            case DATE -> nonEmpty(metadata().getMinDate());
            case TIME -> nonEmpty(metadata().getMinTime());
            case DATETIME_LOCAL -> nonEmpty(metadata().getMinDateTime());
            case NUMBER, RANGE -> finite(metadata().getMin());
            default -> null;
        };
    }

    public String max() {
        return switch (type()) {
            case DATE -> nonEmpty(metadata().getMaxDate());
            case TIME -> nonEmpty(metadata().getMaxTime());
            case DATETIME_LOCAL -> nonEmpty(metadata().getMaxDateTime());
            case NUMBER, RANGE -> finite(metadata().getMax());
            default -> null;
        };
    }

    public String step() {
        return switch (type()) {
            case DATE -> nonEmpty(metadata().getDateStep());
            case TIME -> nonEmpty(metadata().getTimeStep());
            case DATETIME_LOCAL -> nonEmpty(metadata().getDateTimeStep());
            case NUMBER, RANGE -> finite(metadata().getStep());
            default -> null;
        };
    }

    private static String finite(double value) {
        return Double.isFinite(value) ? Double.toString(value) : null;
    }

    public static String nonEmpty(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
