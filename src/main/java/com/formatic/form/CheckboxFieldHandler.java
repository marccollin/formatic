package com.formatic.form;

import com.formatic.service.OptionsProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CheckboxFieldHandler implements FormFieldTypeHandler {

    private static final Logger logger = LoggerFactory.getLogger(CheckboxFieldHandler.class);

    private final OptionsProviderService optionsProviderService;

    public CheckboxFieldHandler(OptionsProviderService optionsProviderService) {
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    public boolean canHandle(Field field, FormField annotation) {
        return annotation.type() == FormFieldType.CHECKBOX ||
                (annotation.type() == FormFieldType.AUTO && isCheckboxCompatibleField(field));
    }

    private boolean isCheckboxCompatibleField(Field field) {
        return isBooleanField(field) || isCollectionField(field);
    }

    private boolean isBooleanField(Field field) {
        return field.getType() == boolean.class || field.getType() == Boolean.class;
    }

    private boolean isCollectionField(Field field) {
        return Collection.class.isAssignableFrom(field.getType()) ||
                Set.class.isAssignableFrom(field.getType()) ||
                List.class.isAssignableFrom(field.getType());
    }

    private boolean isStringField(Field field) {
        return field.getType() == String.class;
    }

    @Override
    public FormFieldType getFieldType() {
        return FormFieldType.CHECKBOX;
    }

    @Override
    public void configureMetadata(FormFieldMetadata metadata, Field field, FormField annotation) {

        // Déterminer le type de checkbox basé sur le type du champ
        if (isBooleanField(field)) {
            configureSimpleCheckbox(metadata, field, annotation);
        } else if (isCollectionField(field)) {
            configureMultiSelectCheckbox(metadata, field, annotation);
        }else if (isStringField(field)) {
            configureStringBasedCheckbox(metadata, field, annotation);
        }
    }

    private void configureSimpleCheckbox(FormFieldMetadata metadata, Field field, FormField annotation) {
        // Pour un boolean simple, pas besoin d'options
        metadata.setMultiple(false);

        // Si des options sont définies pour un boolean, on peut les ignorer ou lever une exception
        if (annotation.options().length > 0 || StringUtils.hasText(annotation.optionsProvider())) {
            // Log warning ou exception selon votre stratégie
            logger.warn("Warning: Options définies pour un champ boolean simple, ignorées");
        }
    }

    private void configureMultiSelectCheckbox(FormFieldMetadata metadata, Field field, FormField annotation) {
        metadata.setMultiple(true);

        // Les options sont obligatoires pour les checkbox multi-select
        if (annotation.options().length > 0) {
            // Options statiques
            List<SelectRadioOption> options = parseStaticOptions(annotation.options());
            metadata.setOptions(options);
        } else if (StringUtils.hasText(annotation.optionsProvider())) {
            // Options dynamiques
            metadata.setOptionsProvider(annotation.optionsProvider());
            List<SelectRadioOption> dynamicOptions = optionsProviderService.loadOptions(annotation.optionsProvider());
            metadata.setOptions(dynamicOptions);
        } else {
            throw new IllegalArgumentException(
                    "Les checkbox multi-select nécessitent des options (statiques ou via optionsProvider)"
            );
        }
    }

    private void configureStringBasedCheckbox(FormFieldMetadata metadata, Field field, FormField annotation) {
        if (annotation.options().length > 0 || StringUtils.hasText(annotation.optionsProvider())) {
            throw new IllegalArgumentException(
                    String.format("Le champ '%s' de type String ne peut pas être utilisé avec des options multiples. " +
                            "Utilisez List<String> ou Set<String> pour les checkbox multiples.", field.getName())
            );
        } else {
            // String sans options = checkbox simple avec valeur "true"/"false"
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
