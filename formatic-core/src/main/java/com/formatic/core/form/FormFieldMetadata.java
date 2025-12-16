package com.formatic.core.form;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents metadata for a form field used to generate HTML inputs dynamically.
 * <p>
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
    private String outerCssClass;
    private Map<String, String> htmlAttributes = new HashMap<>();
    private String group;
    private String displayCondition;
    private String pattern;
    private String errorMessage;
    private String defaultValue;

    private String title;

    private String accept;

    private String minDate;
    private String maxDate;

    private boolean disabled;

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

    public FormFieldMetadata(String name, FormFieldType type, String label, int order) {
        this.name = name;
        this.type = type;
        this.label = label;
        this.order = order;
    }

    // Ajoutez un constructeur par défaut si nécessaire pour la désérialisation
    public FormFieldMetadata() {
    }

    public FormFieldMetadata(FormFieldMetadata source) {
// Copie des primitives et des String (qui sont immuables)
        this.name = source.name;
        this.type = source.type;
        this.label = source.label;
        this.readonly = source.readonly;
        this.required = source.required;
        this.placeholder = source.placeholder;
        this.optionsProvider = source.optionsProvider;
        this.order = source.order;
        this.cssClass = source.cssClass;
        this.outerCssClass = source.outerCssClass;
        this.group = source.group;
        this.displayCondition = source.displayCondition;
        this.pattern = source.pattern;
        this.errorMessage = source.errorMessage;
        this.defaultValue = source.defaultValue;
        this.title = source.title;
        this.accept = source.accept;
        this.minDate = source.minDate;
        this.maxDate = source.maxDate;
        this.disabled = source.disabled;
        this.min = source.min;
        this.max = source.max;
        this.step = source.step;
        this.minLength = source.minLength;
        this.maxLength = source.maxLength;
        this.rows = source.rows;
        this.cols = source.cols;
        this.multiple = source.multiple;
        this.helpText = source.helpText;
        this.htmlAttributesString = source.htmlAttributesString;

        // Copie des Collections (Deep Copy pour les collections)

        // 1. options (List<SelectRadioOption>)
        if (source.options != null) {
            // Créer une nouvelle liste et ajouter les éléments de l'ancienne liste.
            // Si SelectRadioOption est mutable, il faudrait aussi faire une deep copy de ses éléments.
            // On suppose ici que SelectRadioOption est immuable ou que la shallow copy est suffisante.
            this.options = new ArrayList<>(source.options);
        } else {
            this.options = null;
        }

        // 2. htmlAttributes (Map<String, String>)
        if (source.htmlAttributes != null) {
            // Créer une nouvelle HashMap contenant toutes les paires clé-valeur de l'originale.
            this.htmlAttributes = new HashMap<>(source.htmlAttributes);
        } else {
            this.htmlAttributes = new HashMap<>(); // S'assurer qu'elle n'est jamais nulle
        }

        // 3. extraProperties (Map<String, Object>)
        if (source.extraProperties != null) {
            this.extraProperties = new HashMap<>(source.extraProperties);
        } else {
            this.extraProperties = null;
        }
    }


    public Map<String, Object> getExtraProperties() {

        if(extraProperties==null){
            extraProperties=new HashMap<>();
        }

        return extraProperties;
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

    public String getOuterCssClass() {
        return outerCssClass;
    }

    public void setOuterCssClass(String outerCssClass) {
        this.outerCssClass = outerCssClass;
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

    public String getAccept() {
        return accept;
    }

    public void setAccept(String accept) {
        this.accept = accept;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
