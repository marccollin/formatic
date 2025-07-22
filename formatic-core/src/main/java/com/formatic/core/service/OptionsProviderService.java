package com.formatic.core.service;

import com.formatic.core.form.SelectRadioOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for loading and caching dynamic options
 * provided by registered OptionsProvider implementations.
 *
 * Options can be returned as lists or maps and are converted into
 * a unified List of SelectRadioOption objects for use in form fields.
 *
 * Results are cached for performance, with methods to clear the cache selectively.
 */
public class OptionsProviderService {

    private static final Logger logger = LoggerFactory.getLogger(OptionsProviderService.class);

    private final OptionsProviderRegistry registry;

    private final Map<String, List<SelectRadioOption>> optionsCache = new ConcurrentHashMap<>();

    public OptionsProviderService(OptionsProviderRegistry registry) {
        this.registry = registry;
    }

    /**
     * Loads options from cache or from the registered provider if not cached.
     *
     * @param providerMethodName the name of the provider method
     * @return a list of SelectRadioOption objects
     */
    public List<SelectRadioOption> loadOptions(String providerMethodName) {
        return optionsCache.computeIfAbsent(providerMethodName, this::loadOptionsFromProvider);
    }

    /**
     * Loads options by invoking the registered OptionsProvider.
     * Returns empty list if provider not found or on error.
     */
    private List<SelectRadioOption> loadOptionsFromProvider(String providerMethodName) {
        try {
            Optional<OptionsProvider> provider = registry.getProvider(providerMethodName);

            if (provider.isPresent()) {
                Object result = provider.get().getOptions();
                return convertToSelectOptions(result);
            }

            logger.warn("No provider found for method: {}", providerMethodName);
        } catch (Exception e) {
            logger.error("Error loading dynamic options for {}: {}",
                    providerMethodName, e.getMessage());
        }

        return Collections.emptyList();
    }

    private Method findMethodByName(Class<?> clazz, String methodName) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> method.getParameterCount() == 0)
                .filter(method -> List.class.isAssignableFrom(method.getReturnType()) ||
                        Map.class.isAssignableFrom(method.getReturnType()))
                .findFirst()
                .orElse(null);
    }

    private List<SelectRadioOption> invokeOptionsProvider(Object bean, Method method)
            throws ReflectiveOperationException {
        Object result = method.invoke(bean);

        return switch (result) {
            case List<?> list -> convertToSelectOptions(list);
            case Map<?, ?> map -> convertMapToSelectOptions((Map<String, String>) map);
            case null -> Collections.emptyList();
            default -> {
                logger.warn("Unsupported return type for options: {}", result.getClass().getSimpleName());
                yield Collections.emptyList();
            }
        };
    }

    private List<SelectRadioOption> convertToSelectOptions(Object result) {
        return switch (result) {
            case List<?> list -> convertListToSelectOptions(list);
            case Map<?, ?> map -> convertMapToSelectOptions((Map<String, String>) map);
            case null -> Collections.emptyList();
            default -> {
                logger.warn("Unsupported return type for options: {}", result.getClass().getSimpleName());
                yield Collections.emptyList();
            }
        };
    }

    private List<SelectRadioOption> convertListToSelectOptions(List<?> list) {
        return list.stream()
                .map(this::convertItemToSelectOption)
                .collect(Collectors.toList());
    }


    @SuppressWarnings("unchecked")
    private List<SelectRadioOption> convertToSelectOptions(List<?> list) {
        return list.stream()
                .map(this::convertItemToSelectOption)
                .collect(Collectors.toList());
    }

    private SelectRadioOption convertItemToSelectOption(Object item) {
        return switch (item) {
            case SelectRadioOption option -> option;
            case Map<?, ?> map -> {
                Map<String, Object> objMap = (Map<String, Object>) map;
                String value = String.valueOf(objMap.get("value"));
                String label = String.valueOf(objMap.get("label"));
                yield new SelectRadioOption(value, label);
            }
            default -> {
                String str = item.toString();
                yield new SelectRadioOption(str, str);
            }
        };
    }

    private List<SelectRadioOption> convertMapToSelectOptions(Map<String, String> map) {
        return map.entrySet().stream()
                .map(entry -> new SelectRadioOption(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Clears the entire options cache.
     */
    public void clearCache() {
        optionsCache.clear();
    }

    /**
     * Clears the cached options for a specific provider method.
     *
     * @param providerName the provider method name
     */
    public void clearCache(String providerName) {
        optionsCache.remove(providerName);
    }
}
