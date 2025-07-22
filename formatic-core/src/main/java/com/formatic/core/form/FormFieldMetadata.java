package com.formatic.core.form;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents metadata for a form field used to generate HTML inputs dynamically.
 *
 * This class stores common HTML input attributes as well as additional properties
 * specific to different input types (e.g., text, select, date).
 */
public class FormFieldMetadata {

    private String name;
    private FormFieldType type;
    private String label;
    private boolean readonly;
    private boolean required;
    private String placeholder;
    private List<SelectRadioOption> options;
    private String optionsProvider;
    private int order;
    private String cssClass;
    private Map<String, String> htmlAttributes = new HashMap<>();
    private String group;
    private String displayCondition;
    private String pattern;
    private String errorMessage;
    private String defaultValue;

    private String title;

    private String minDate;
    private String maxDate;

    private boolean readOnly;
    private double min;
    private double max;
    private double step;
    private int minLength;
    private int maxLength;
    private int rows;
    private int cols;
    private boolean multiple;
    private String helpText;

    Map<String, Object> extraProperties;

    private String htmlAttributesString;

    public Map<String, Object> getExtraProperties() {

        if(extraProperties==null){
            extraProperties=new HashMap<>();
        }

        return extraProperties;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getExtraAttributesAsHtml() {
        if (extraProperties == null || extraProperties.isEmpty()) return "";
        return extraProperties.entrySet().stream()
                .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(" "));
    }


    public void setExtraProperties(Map<String, Object> extraProperties) {

        if(extraProperties==null){
            extraProperties=new HashMap<>();
        }

        this.extraProperties = extraProperties;
    }

    public String getHtmlAttributesString() {
        return this.htmlAttributesString;
    }

    public void setHtmlAttributesString(String htmlAttributesString) {
        this.htmlAttributesString = htmlAttributesString;
    }

    public void setHtmlAttributesString(String[] htmlAttributes) {

        this.htmlAttributesString = (htmlAttributes == null || htmlAttributes.length == 0) ? null :
                Arrays.stream(htmlAttributes)
                        .map(attr -> attr.split(":", 2))
                        .filter(parts -> parts.length == 2)
                        .map(parts -> parts[0].trim() + "=\"" + parts[1].trim() + "\"")
                        .collect(Collectors.joining(" "));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FormFieldType getType() {
        return type;
    }

    public void setType(FormFieldType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public List<SelectRadioOption> getOptions() {
        return options;
    }

    public void setOptions(List<SelectRadioOption> options) {
        this.options = options;
    }

    public String getOptionsProvider() {
        return optionsProvider;
    }

    public void setOptionsProvider(String optionsProvider) {
        this.optionsProvider = optionsProvider;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public Map<String, String> getHtmlAttributes() {
        Map<String, String> attrs = new HashMap<>();
        if (htmlAttributesString != null) {
            Arrays.stream(htmlAttributesString.split(","))
                    .map(attr -> attr.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .forEach(parts ->
                            attrs.put(parts[0].trim(), parts[1].trim().replaceAll("['\"]", "")));
        }
        return attrs;
    }

    public String getHtmlAttributesAsString() {
        if (htmlAttributes == null || htmlAttributes.isEmpty()) {
            return "";
        }
        return htmlAttributes.entrySet().stream()
                .map(e -> "data-" + e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(","));
    }

    public void setHtmlAttributes(Map<String, String> htmlAttributes) {
        this.htmlAttributes = htmlAttributes;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDisplayCondition() {
        return displayCondition;
    }

    public void setDisplayCondition(String displayCondition) {
        this.displayCondition = displayCondition;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getStep() {
        return step;
    }

    public void setStep(double step) {
        this.step = step;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public String getMinDate() {
        return minDate;
    }

    public void setMinDate(String minDate) {
        this.minDate = minDate;
    }

    public String getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(String maxDate) {
        this.maxDate = maxDate;
    }

}
