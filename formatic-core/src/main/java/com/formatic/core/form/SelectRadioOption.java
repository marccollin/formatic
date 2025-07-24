package com.formatic.core.form;

import java.util.Objects;

/**
 * Represents an option item for select or radio input elements.
 * <p>
 * Each option has a value and a label displayed to the user.
 * Additional attributes control selection, disabled state, and grouping.
 */
public class SelectRadioOption {
    private String value;
    private String label;
    private boolean selected;
    private boolean disabled;
    private String group;

    public SelectRadioOption(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectRadioOption that = (SelectRadioOption) o;
        return Objects.equals(value, that.value) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, label);
    }

    @Override
    public String toString() {
        return "SelectRadioOption{" +
                "value='" + value + '\'' +
                ", label='" + label + '\'' +
                '}';
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
