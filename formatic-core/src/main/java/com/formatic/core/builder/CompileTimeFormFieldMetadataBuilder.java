package com.formatic.core.builder;

import com.formatic.core.form.SelectRadioOption;
import com.formatic.core.service.OptionsProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.formatic.core.form.FormFieldMetadata;

public class CompileTimeFormFieldMetadataBuilder implements FormFieldMetadataBuilder {

    private static final Logger log = LoggerFactory.getLogger(CompileTimeFormFieldMetadataBuilder.class);

    //Expected suffix of the generated class (e.g., UserFormFormMetadata)
    private static final String ACCESSOR_SUFFIX = "FormMetadata";

    //Cache to store the List<FormFieldMetadata> once loaded
    private final java.util.Map<Class<?>, List<FormFieldMetadata>> cache = new ConcurrentHashMap<>();

    private final OptionsProviderService optionsProviderService;

    public CompileTimeFormFieldMetadataBuilder(OptionsProviderService optionsProviderService) {
        this.optionsProviderService = optionsProviderService;
    }

    @Override
    public List<FormFieldMetadata> buildMetadata(Class<?> clazz) {
        //La mise en cache garantit que la réflexion n'est utilisée qu'une seule fois par Class<?>
        return cache.computeIfAbsent(clazz, this::loadFromGeneratedClass);
    }

    @SuppressWarnings("unchecked")
    private List<FormFieldMetadata> loadFromGeneratedClass(Class<?> clazz) {
        //Constructs the expected class name: <Package>.<Class>FormMetadata
        String accessorClassName = clazz.getName() + ACCESSOR_SUFFIX;

        try {
            //Try to load class
            Class<?> accessorClass = Class.forName(accessorClassName);

            //Get static method
            java.lang.reflect.Method getMetadataMethod = accessorClass.getMethod("getMetadata");

            //Class static method to get metadata
            List<FormFieldMetadata> metadata = (List<FormFieldMetadata>) getMetadataMethod.invoke(null);

            //Resolve dynamically option value (radio button, select... before caching
            List<FormFieldMetadata> resolvedMetadata = resolveDynamicOptions(metadata, clazz);

            log.info("Loaded metadata for {}... Fields: {}", clazz.getSimpleName(), resolvedMetadata.size());

            //sort and put in a immuable list
            return resolvedMetadata.stream()
                    .sorted(java.util.Comparator.comparingInt(FormFieldMetadata::getOrder))
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));


        } catch (ClassNotFoundException e) {
            log.warn("Generated metadata class {} not found for {}. Please ensure Annotation Processor is running.",
                    accessorClassName, clazz.getSimpleName());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to load or invoke getMetadata() on generated class {} for {}",
                    accessorClassName, clazz.getSimpleName(), e);
            return Collections.emptyList();
        }
    }

    private List<FormFieldMetadata> resolveDynamicOptions(List<FormFieldMetadata> metadata, Class<?> originalClass) {
        List<FormFieldMetadata> resolvedMetadata = new ArrayList<>(metadata.size());

        for (FormFieldMetadata fieldMeta : metadata) {
            String providerName = fieldMeta.getOptionsProvider();

            if (providerName != null && !providerName.isEmpty()) {

                try {
                    List<SelectRadioOption> dynamicOptions = optionsProviderService.loadOptions(providerName);

                    //update options
                    fieldMeta.setOptions(dynamicOptions);

                    resolvedMetadata.add(fieldMeta);

                } catch (Exception e) {
                    log.error("Failed to invoke provider method '{}' for field {}", providerName, fieldMeta.getName(), e);
                    resolvedMetadata.add(fieldMeta);
                }
            } else {
                resolvedMetadata.add(fieldMeta);
            }
        }
        return resolvedMetadata;
    }

}