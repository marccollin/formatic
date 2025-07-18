package com.formatic.spring.registry;
import com.formatic.core.service.OptionsProvider;
import com.formatic.core.form.SelectRadioOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestConfig.class)
class SpringOptionsProviderRegistryIntegrationTest {

    @Autowired
    private SpringOptionsProviderRegistry registry;

    @Test
    void integrationTest_WithRealSpringBeans() {
        // When
        Optional<OptionsProvider> provider = registry.getProvider("getCountries");

        // Then
        assertTrue(provider.isPresent());

        Object options = provider.get().getOptions();
        assertNotNull(options);
        assertInstanceOf(List.class, options);

        @SuppressWarnings("unchecked")
        List<SelectRadioOption> optionsList = (List<SelectRadioOption>) options;
        assertEquals(3, optionsList.size());

        // Verify specific options
        assertTrue(optionsList.stream().anyMatch(o ->
                "FR".equals(o.getValue()) && "France".equals(o.getLabel())));
        assertTrue(optionsList.stream().anyMatch(o ->
                "US".equals(o.getValue()) && "United States".equals(o.getLabel())));
        assertTrue(optionsList.stream().anyMatch(o ->
                "CA".equals(o.getValue()) && "Canada".equals(o.getLabel())));
    }

    @Test
    void integrationTest_WithMapProvider() {
        // When
        Optional<OptionsProvider> provider = registry.getProvider("getLanguages");

        // Then
        assertTrue(provider.isPresent());

        Object options = provider.get().getOptions();
        assertNotNull(options);
        assertInstanceOf(Map.class, options);

        @SuppressWarnings("unchecked")
        Map<String, String> optionsMap = (Map<String, String>) options;
        assertEquals(2, optionsMap.size());
        assertEquals("French", optionsMap.get("fr"));
        assertEquals("English", optionsMap.get("en"));
    }
}