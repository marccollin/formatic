package com.formatic.core.annotation.handler;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.annotation.CheckboxInput;

import com.formatic.core.form.SelectRadioOption;
import com.formatic.core.service.OptionsProviderService;
import com.formatic.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler for fields annotated with {@code @CheckboxInput}.
 *
 * This class determines how to interpret and configure a field based on its Java type:
 *
 * <ul>
 *   <li><b>boolean / Boolean:</b> Generates a simple checkbox (true/false). Any defined options are ignored.</li>
 *   <li><b>Collection (List, Set, etc.):</b> Generates a group of multi-select checkboxes.
 *       Options (static or via provider) are required.</li>
 *   <li><b>String:</b> Treated as a simple checkbox with "true"/"false" values if no options are defined.
 *       If options are provided, an exception is thrown.</li>
 * </ul>
 *
 * Options can be specified in two ways:
 * <ul>
 *   <li>Statically via the {@code options} attribute of the annotation</li>
 *   <li>Dynamically via a registered {@code optionsProvider}</li>
 * </ul>
 *
 * This handler uses {@link OptionsProviderService} to load dynamic options and populates
 * the {@link FormFieldMetadata} with the appropriate configuration to render the field in the UI.
 */
public class CheckBoxHandler extends BaseFormFieldHandler<CheckboxInput> {

    private static final Logger logger = LoggerFactory.getLogger(CheckBoxHandler.class);

    private final OptionsProviderService optionsProviderService;

    public CheckBoxHandler(OptionsProviderService optionsProviderService) {
        super(CheckboxInput.class);
        this.optionsProviderService = optionsProviderService;
    }


    @Override
    protected void processSpecificAttributes(CheckboxInput annotation, FormFieldMetadata metadata, Field field) {
        metadata.setType(FormFieldType.CHECKBOX);

        // Determine the checkbox type based on the field type
        if (isBooleanField(field)) {
            configureSimpleCheckbox(metadata, field, annotation);
        } else if (isCollectionField(field)) {
            configureMultiSelectCheckbox(metadata, field, annotation);
        } else if (isStringField(field)) {
            configureStringBasedCheckbox(metadata, field, annotation);
        }

    }

    private boolean isBooleanField(Field field) {
        return field.getType() == boolean.class || field.getType() == Boolean.class;
    }

    private void configureSimpleCheckbox(FormFieldMetadata metadata, Field field, CheckboxInput annotation) {
        // For a simple boolean, no options are needed
        metadata.setMultiple(false);

        // "If options are defined for a boolean, they can be ignored or an exception can be thrown
        if (annotation.options().length > 0 || StringUtils.hasText(annotation.optionsProvider())) {
            // Log warning
            logger.warn("Warning: Options définies pour un champ boolean simple, ignorées");
        }
    }

    private boolean isCollectionField(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) ||
                Set.class.isAssignableFrom(field.getType()) ||
                List.class.isAssignableFrom(field.getType());
    }


    private void configureMultiSelectCheckbox(FormFieldMetadata metadata, Field field, CheckboxInput annotation) {
        metadata.setMultiple(true);

        // Options are mandatory for multi-select checkboxes
        if (annotation.options().length > 0) {
            // Static options
            List<SelectRadioOption> options = parseStaticOptions(annotation.options());
            metadata.setOptions(options);
        } else if (StringUtils.hasText(annotation.optionsProvider())) {
            // Dynamic option
            metadata.setOptionsProvider(annotation.optionsProvider());
            List<SelectRadioOption> dynamicOptions = optionsProviderService.loadOptions(annotation.optionsProvider());
            metadata.setOptions(dynamicOptions);
        } else {
            throw new IllegalArgumentException(
                    "Multi-select checkboxes require options (either static or via optionsProvider)"
            );
        }
    }

    private boolean isStringField(Field field) {
        return field.getType() == String.class;
    }


    private void configureStringBasedCheckbox(FormFieldMetadata metadata, Field field, CheckboxInput annotation) {
        if (annotation.options().length > 0 || StringUtils.hasText(annotation.optionsProvider())) {
            throw new IllegalArgumentException(
                    String.format("The field '%s' of type String cannot be used with multiple options. " +
                            "Use List<String> or Set<String> for multiple checkboxes.", field.getName())
            );
        } else {
            // String without options = simple checkbox with 'true'/'false' value
            metadata.setMultiple(false);
        }
    }

    private List<SelectRadioOption> parseStaticOptions(String[] optionsArray) {
        return Arrays.stream(optionsArray)
                .map(this::parseStaticOption)
                .collect(Collectors.toList());
    }

    private SelectRadioOption parseStaticOption(String optionStr) {
        if (optionStr.contains(":")) {
            String[] parts = optionStr.split(":", 2);
            return new SelectRadioOption(parts[0].trim(), parts[1].trim());
        }
        return new SelectRadioOption(optionStr.trim(), optionStr.trim());
    }

}
