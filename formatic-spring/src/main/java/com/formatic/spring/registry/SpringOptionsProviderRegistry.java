package com.formatic.spring.registry;

import com.formatic.core.service.OptionsProvider;
import com.formatic.core.service.OptionsProviderRegistry;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring implementation of the registry that automatically discovers providers
 */
public class SpringOptionsProviderRegistry implements OptionsProviderRegistry {

    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, OptionsProvider> providers = new ConcurrentHashMap<>();

    public SpringOptionsProviderRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void registerProvider(String methodName, OptionsProvider provider) {
        providers.put(methodName, provider);
    }

    @Override
    public Optional<OptionsProvider> getProvider(String methodName) {
        return Optional.ofNullable(providers.computeIfAbsent(methodName, this::discoverProvider));
    }

    private OptionsProvider discoverProvider(String methodName) {
        Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);

        for (Object bean : beans.values()) {
            Method method = findMethodByName(bean.getClass(), methodName);
            if (method != null) {
                return () -> {
                    try {
                        return method.invoke(bean);
                    } catch (Exception e) {
                        throw new RuntimeException("Error while invoking  " + methodName, e);
                    }
                };
            }
        }

        return null;
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

    @Override
    public void removeProvider(String methodName) {
        providers.remove(methodName);
    }

    @Override
    public void clear() {
        providers.clear();
    }
}
