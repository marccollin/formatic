package com.formatic.core.service;

import com.formatic.core.form.SelectRadioOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OptionsProviderServiceTest {

    @Mock
    private OptionsProviderRegistry registry;

    @Mock
    private OptionsProvider optionsProvider;

    private OptionsProviderService service;

    @BeforeEach
    void setUp() {
        service = new OptionsProviderService(registry);
    }

    @Test
    void loadOptions_WithValidProvider_ReturnsOptions() {
        // Given
        String providerMethodName = "testProvider";
        List<SelectRadioOption> expectedOptions = Arrays.asList(
                new SelectRadioOption("1", "Option 1"),
                new SelectRadioOption("2", "Option 2")
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(expectedOptions);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(expectedOptions, result);
        verify(registry).getProvider(providerMethodName);
        verify(optionsProvider).getOptions();
    }

    @Test
    void loadOptions_WithProviderNotFound_ReturnsEmptyList() {
        // Given
        String providerMethodName = "nonExistentProvider";
        when(registry.getProvider(providerMethodName)).thenReturn(Optional.empty());

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertTrue(result.isEmpty());
        verify(registry).getProvider(providerMethodName);
        verifyNoInteractions(optionsProvider);
    }

    @Test
    void loadOptions_WithProviderThrowingException_ReturnsEmptyList() {
        // Given
        String providerMethodName = "errorProvider";
        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenThrow(new RuntimeException("Test exception"));

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertTrue(result.isEmpty());
        verify(registry).getProvider(providerMethodName);
        verify(optionsProvider).getOptions();
    }

    @Test
    void loadOptions_WithListOfSelectRadioOptions_ReturnsCorrectOptions() {
        // Given
        String providerMethodName = "listProvider";
        List<SelectRadioOption> originalOptions = Arrays.asList(
                new SelectRadioOption("val1", "Label 1"),
                new SelectRadioOption("val2", "Label 2")
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(originalOptions);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(2, result.size());
        assertEquals("val1", result.get(0).getValue());
        assertEquals("Label 1", result.get(0).getLabel());
        assertEquals("val2", result.get(1).getValue());
        assertEquals("Label 2", result.get(1).getLabel());
    }

    @Test
    void loadOptions_WithListOfMaps_ReturnsCorrectOptions() {
        // Given
        String providerMethodName = "mapListProvider";
        List<Map<String, Object>> mapList = Arrays.asList(
                Map.of("value", "key1", "label", "Value 1"),
                Map.of("value", "key2", "label", "Value 2")
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(mapList);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(2, result.size());
        assertEquals("key1", result.get(0).getValue());
        assertEquals("Value 1", result.get(0).getLabel());
        assertEquals("key2", result.get(1).getValue());
        assertEquals("Value 2", result.get(1).getLabel());
    }

    @Test
    void loadOptions_WithListOfStrings_ReturnsCorrectOptions() {
        // Given
        String providerMethodName = "stringListProvider";
        List<String> stringList = Arrays.asList("Option A", "Option B", "Option C");

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(stringList);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(3, result.size());
        assertEquals("Option A", result.get(0).getValue());
        assertEquals("Option A", result.get(0).getLabel());
        assertEquals("Option B", result.get(1).getValue());
        assertEquals("Option B", result.get(1).getLabel());
        assertEquals("Option C", result.get(2).getValue());
        assertEquals("Option C", result.get(2).getLabel());
    }

    @Test
    void loadOptions_WithMap_ReturnsCorrectOptions() {
        // Given
        String providerMethodName = "mapProvider";
        Map<String, String> optionsMap = Map.of(
                "key1", "Label 1",
                "key2", "Label 2"
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(optionsMap);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(option ->
                "key1".equals(option.getValue()) && "Label 1".equals(option.getLabel())));
        assertTrue(result.stream().anyMatch(option ->
                "key2".equals(option.getValue()) && "Label 2".equals(option.getLabel())));
    }

    @Test
    void loadOptions_WithNullResult_ReturnsEmptyList() {
        // Given
        String providerMethodName = "nullProvider";
        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(null);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void loadOptions_WithUnsupportedType_ReturnsEmptyList() {
        // Given
        String providerMethodName = "unsupportedProvider";
        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(42); // Integer not supported

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void loadOptions_CachesResults() {
        // Given
        String providerMethodName = "cachedProvider";
        List<SelectRadioOption> expectedOptions = List.of(
                new SelectRadioOption("1", "Option 1")
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(expectedOptions);

        // When
        List<SelectRadioOption> result1 = service.loadOptions(providerMethodName);
        List<SelectRadioOption> result2 = service.loadOptions(providerMethodName);

        // Then
        assertEquals(expectedOptions, result1);
        assertEquals(expectedOptions, result2);
        assertSame(result1, result2); // Same instance from the cache
        verify(registry, times(1)).getProvider(providerMethodName); // Call a single time
        verify(optionsProvider, times(1)).getOptions(); // Call a single time
    }

    @Test
    void clearCache_RemovesAllCachedEntries() {
        // Given
        String providerMethodName = "testProvider";
        List<SelectRadioOption> expectedOptions = List.of(
                new SelectRadioOption("1", "Option 1")
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(expectedOptions);

        // Populate cache
        service.loadOptions(providerMethodName);
        verify(registry, times(1)).getProvider(providerMethodName);

        // When
        service.clearCache();
        service.loadOptions(providerMethodName);

        // Then
        verify(registry, times(2)).getProvider(providerMethodName); // Called again after clear
    }

    @Test
    void clearCache_WithSpecificProvider_RemovesOnlyThatEntry() {
        // Given
        String provider1 = "provider1";
        String provider2 = "provider2";
        List<SelectRadioOption> options1 = List.of(new SelectRadioOption("1", "Option 1"));
        List<SelectRadioOption> options2 = List.of(new SelectRadioOption("2", "Option 2"));

        OptionsProvider mockProvider2 = mock(OptionsProvider.class);

        when(registry.getProvider(provider1)).thenReturn(Optional.of(optionsProvider));
        when(registry.getProvider(provider2)).thenReturn(Optional.of(mockProvider2));
        when(optionsProvider.getOptions()).thenReturn(options1);
        when(mockProvider2.getOptions()).thenReturn(options2);

        // Populate cache
        service.loadOptions(provider1);
        service.loadOptions(provider2);

        // When
        service.clearCache(provider1);
        service.loadOptions(provider1); // Should reload
        service.loadOptions(provider2); // Should use cache

        // Then
        verify(registry, times(2)).getProvider(provider1); // Called again after clear
        verify(registry, times(1)).getProvider(provider2); // Still in cache
    }

    @Test
    void convertItemToSelectOption_WithMixedTypes() {
        // Given
        String providerMethodName = "mixedProvider";
        List<Object> mixedList = Arrays.asList(
                new SelectRadioOption("existing", "Existing Option"),
                Map.of("value", "mapValue", "label", "Map Label"),
                "Simple String",
                123 // Number
        );

        when(registry.getProvider(providerMethodName)).thenReturn(Optional.of(optionsProvider));
        when(optionsProvider.getOptions()).thenReturn(mixedList);

        // When
        List<SelectRadioOption> result = service.loadOptions(providerMethodName);

        // Then
        assertEquals(4, result.size());

        // Existing SelectRadioOption
        assertEquals("existing", result.get(0).getValue());
        assertEquals("Existing Option", result.get(0).getLabel());

        // Map
        assertEquals("mapValue", result.get(1).getValue());
        assertEquals("Map Label", result.get(1).getLabel());

        // String
        assertEquals("Simple String", result.get(2).getValue());
        assertEquals("Simple String", result.get(2).getLabel());

        // Number converted to string
        assertEquals("123", result.get(3).getValue());
        assertEquals("123", result.get(3).getLabel());
    }
}