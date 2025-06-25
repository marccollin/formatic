package com.formatic.service;

import com.formatic.form.FormField;
import com.formatic.form.FormFieldMetadata;
import com.formatic.form.FormFieldType;
import com.formatic.form.FormFieldTypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class FormMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(FormMetadataService.class);

    private final List<FormFieldTypeHandler> typeHandlers;
    private final MetadataCacheService cacheService;

    public FormMetadataService(List<FormFieldTypeHandler> typeHandlers,
                               MetadataCacheService cacheService) {
        this.typeHandlers = typeHandlers;
        this.cacheService = cacheService;
    }

    public List<FormFieldMetadata> getMetadataForClass(Class<?> clazz) {
        if (cacheService.containsClass(clazz)) {
            return cacheService.getMetadataForClass(clazz);
        }

        List<FormFieldMetadata> metadata = buildMetadata(clazz);
        cacheService.putMetadataForClass(clazz, metadata);
        return metadata;
    }

    private List<FormFieldMetadata> buildMetadata(Class<?> clazz) {
        List<FormFieldMetadata> metadataList = new ArrayList<>();
        processFieldsRecursively(clazz, metadataList);

        //defaut value for order it's 0. Theses values is put after the other who have a value
        return metadataList.stream()
                .sorted(Comparator.comparingInt((FormFieldMetadata m) ->
                        m.getOrder() == 0 ? Integer.MAX_VALUE : m.getOrder()))
                .collect(Collectors.toList());
    }

    private void processFieldsRecursively(Class<?> clazz, List<FormFieldMetadata> metadataList) {
        if (clazz == null || clazz == Object.class) {
            return;
        }

        processFieldsRecursively(clazz.getSuperclass(), metadataList);

        Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(FormField.class))
                .forEach(field -> {
                    try {
                        FormFieldMetadata metadata = buildFieldMetadata(field);
                        metadataList.add(metadata);
                    } catch (Exception e) {
                        logger.error("Erreur lors de la construction des métadonnées pour le champ {}: {}",
                                field.getName(), e.getMessage());
                    }
                });
    }

    private FormFieldMetadata buildFieldMetadata(Field field) {
        FormField formField = field.getAnnotation(FormField.class);
        FormFieldMetadata metadata = new FormFieldMetadata();

        // Configuration de base
        configureBasicProperties(metadata, field, formField);

        // Configuration spécifique selon le type via Strategy Pattern
        FormFieldTypeHandler handler = findHandler(field, formField);
        if (handler != null) {
            metadata.setType(handler.getFieldType());
            handler.configureMetadata(metadata, field, formField);
        } else {
            metadata.setType(FormFieldType.TEXT); // Fallback
        }

        // Configuration des validations communes
        configureValidations(metadata, formField);

        return metadata;
    }

    private FormFieldTypeHandler findHandler(Field field, FormField annotation) {
        return typeHandlers.stream()
                .filter(handler -> handler.canHandle(field, annotation))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No handler found for field " + field.getName()));
    }

    private void configureBasicProperties(FormFieldMetadata metadata, Field field, FormField formField) {
        metadata.setName(StringUtils.hasText(formField.name()) ?
                formField.name() : field.getName());

        metadata.setLabel(StringUtils.hasText(formField.label()) ?
                formField.label() : capitalizeFirstLetter(field.getName()));

        metadata.setCssClass(formField.cssClass());
        metadata.setRequired(formField.required());
        metadata.setPlaceholder(formField.placeholder());
        metadata.setOrder(formField.order());

        metadata.setHtmlAttributesString(formField.htmlAttributes());

    }

    private void configureValidations(FormFieldMetadata metadata, FormField formField) {
        if (formField.minLength() > 0 && formField.minLength() < Integer.MAX_VALUE) {
            metadata.setMinLength(formField.minLength());
        }
        if (formField.maxLength() > 0 && formField.maxLength() < Integer.MAX_VALUE) {
            metadata.setMaxLength(formField.maxLength());
        }

        if(formField.min()>0 && formField.min()< Double.MAX_VALUE) {
            metadata.setMin(formField.min());
        }

        if(formField.max()>0 && formField.max()< Double.MAX_VALUE) {
            metadata.setMax(formField.max());
        }
    }

    private String capitalizeFirstLetter(String str) {
        return StringUtils.hasText(str) ?
                str.substring(0, 1).toUpperCase() + str.substring(1) : str;
    }

    // Méthodes de gestion du cache
    public void clearCache() {
        cacheService.clearCache();
    }

    public void clearCacheForClass(Class<?> clazz) {
        cacheService.clearCacheForClass(clazz);
    }

}
