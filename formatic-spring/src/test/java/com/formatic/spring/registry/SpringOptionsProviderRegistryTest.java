package com.formatic.spring.registry;

import com.formatic.core.service.OptionsProvider;
import com.formatic.core.service.OptionsProviderRegistry;
import com.formatic.core.form.SelectRadioOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SpringOptionsProviderRegistryTest {

    @Mock
    private ApplicationContext applicationContext;

    private SpringOptionsProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SpringOptionsProviderRegistry(applicationContext);
    }

    @Test
    void registerProvider_AndRetrieve_Success() {
        // Given
        String methodName = "testMethod";
        OptionsProvider provider = mock(OptionsProvider.class);

        // When
        registry.registerProvider(methodName, provider);
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertTrue(result.isPresent());
        assertEquals(provider, result.get());
        verifyNoInteractions(applicationContext);
    }

    @Test
    void getProvider_WithDiscoveredBean_Success() {
        // Given
        String methodName = "getTestOptions";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertTrue(result.isPresent());

        // Verify the provider works
        Object options = result.get().getOptions();
        assertNotNull(options);
        assertInstanceOf(List.class, options);

        @SuppressWarnings("unchecked")
        List<SelectRadioOption> optionsList = (List<SelectRadioOption>) options;
        assertEquals(2, optionsList.size());

        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithDiscoveredBeanMap_Success() {
        // Given
        String methodName = "getMapOptions";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertTrue(result.isPresent());

        // Verify the provider works
        Object options = result.get().getOptions();
        assertNotNull(options);
        assertInstanceOf(Map.class, options);

        @SuppressWarnings("unchecked")
        Map<String, String> optionsMap = (Map<String, String>) options;
        assertEquals(2, optionsMap.size());

        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithMultipleBeans_FindsCorrectMethod() {
        // Given
        String methodName = "getSpecificOptions";
        TestOptionsBean testBean1 = new TestOptionsBean();
        AnotherOptionsBean testBean2 = new AnotherOptionsBean();

        Map<String, Object> beans = Map.of(
                "testBean1", testBean1,
                "testBean2", testBean2
        );

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertTrue(result.isPresent());

        // Verify it found the correct method
        Object options = result.get().getOptions();
        assertNotNull(options);
        assertInstanceOf(List.class, options);

        @SuppressWarnings("unchecked")
        List<String> optionsList = (List<String>) options;
        assertEquals(1, optionsList.size());
        assertEquals("specific", optionsList.getFirst());

        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithMethodNotFound_ReturnsEmpty() {
        // Given
        String methodName = "nonExistentMethod";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertFalse(result.isPresent());
        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithEmptyBeans_ReturnsEmpty() {
        // Given
        String methodName = "getTestOptions";
        Map<String, Object> beans = Collections.emptyMap();

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertFalse(result.isPresent());
        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithMethodHavingParameters_IgnoresMethod() {
        // Given
        String methodName = "getOptionsWithParams";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertFalse(result.isPresent());
        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_WithMethodHavingWrongReturnType_IgnoresMethod() {
        // Given
        String methodName = "getStringOptions";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertFalse(result.isPresent());
        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void getProvider_CachesResult() {
        // Given
        String methodName = "getTestOptions";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result1 = registry.getProvider(methodName);
        Optional<OptionsProvider> result2 = registry.getProvider(methodName);

        // Then
        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertSame(result1.get(), result2.get()); // Same instance from cache

        verify(applicationContext, times(1)).getBeansOfType(Object.class); // Called only once
    }

    @Test
    void getProvider_WithProviderThrowingException_ThrowsRuntimeException() {
        // Given
        String methodName = "getErrorOptions";
        TestOptionsBean testBean = new TestOptionsBean();
        Map<String, Object> beans = Map.of("testBean", testBean);

        when(applicationContext.getBeansOfType(Object.class)).thenReturn(beans);

        // When
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertTrue(result.isPresent());

        // Verify that invoking the provider throws RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            result.get().getOptions();
        });

        assertTrue(exception.getMessage().contains("Error while invoking  " + methodName));
        assertNotNull(exception.getCause());

        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void removeProvider_RemovesFromCache() {
        // Given
        String methodName = "testMethod";
        OptionsProvider provider = mock(OptionsProvider.class);
        registry.registerProvider(methodName, provider);

        // When
        registry.removeProvider(methodName);
        Optional<OptionsProvider> result = registry.getProvider(methodName);

        // Then
        assertFalse(result.isPresent()); // Should rediscover since removed from cache
        verify(applicationContext).getBeansOfType(Object.class);
    }

    @Test
    void clear_RemovesAllProviders() {
        // Given
        OptionsProvider provider1 = mock(OptionsProvider.class);
        OptionsProvider provider2 = mock(OptionsProvider.class);
        registry.registerProvider("method1", provider1);
        registry.registerProvider("method2", provider2);

        // When
        registry.clear();

        // Then
        assertFalse(registry.getProvider("method1").isPresent());
        assertFalse(registry.getProvider("method2").isPresent());
        verify(applicationContext, times(2)).getBeansOfType(Object.class);
    }

    @Test
    void registryImplementsInterface() {
        // Then
        assertInstanceOf(OptionsProviderRegistry.class, registry);
    }

    // Classes de test pour simuler des beans Spring
    static class TestOptionsBean {

        public List<SelectRadioOption> getTestOptions() {
            return Arrays.asList(
                    new SelectRadioOption("1", "Option 1"),
                    new SelectRadioOption("2", "Option 2")
            );
        }

        public Map<String, String> getMapOptions() {
            return Map.of("key1", "value1", "key2", "value2");
        }

        public List<String> getOptionsWithParams(String param) {
            return Arrays.asList("param1", "param2");
        }

        public String getStringOptions() {
            return "not a list or map";
        }

        public List<String> getErrorOptions() {
            throw new IllegalStateException("Simulated error");
        }
    }

    static class AnotherOptionsBean {

        public List<String> getSpecificOptions() {
            return List.of("specific");
        }
    }

}
