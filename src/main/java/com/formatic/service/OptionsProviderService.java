package com.formatic.service;

import com.formatic.form.SelectRadioOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OptionsProviderService {

    private static final Logger logger = LoggerFactory.getLogger(OptionsProviderService.class);

    private final ApplicationContext applicationContext;
    private final Map<String, List<SelectRadioOption>> optionsCache = new ConcurrentHashMap<>();

    public OptionsProviderService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<SelectRadioOption> loadOptions(String providerMethodName) {
        return optionsCache.computeIfAbsent(providerMethodName, this::loadOptionsFromProvider);
    }

    private List<SelectRadioOption> loadOptionsFromProvider(String providerMethodName) {
        try {
            Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);

            for (Object bean : beans.values()) {
                Method method = findMethodByName(bean.getClass(), providerMethodName);
                if (method != null) {
                    return invokeOptionsProvider(bean, method);
                }
            }

            logger.warn("Aucun provider trouvé pour la méthode: {}", providerMethodName);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des options dynamiques pour {}: {}",
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
                logger.warn("Type de retour non supporté pour les options: {}",
                        result.getClass().getSimpleName());
                yield Collections.emptyList();
            }
        };
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

    public void clearCache() {
        optionsCache.clear();
    }

    public void clearCache(String providerName) {
        optionsCache.remove(providerName);
    }
}
