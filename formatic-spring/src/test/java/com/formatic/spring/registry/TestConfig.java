package com.formatic.spring.registry;

import com.formatic.core.form.SelectRadioOption;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootConfiguration
@Configuration
public class TestConfig {

    @Bean
    public SpringOptionsProviderRegistry springOptionsProviderRegistry(ApplicationContext applicationContext) {
        return new SpringOptionsProviderRegistry(applicationContext);
    }

    @Bean
    public CountryOptionsProvider countryOptionsProvider() {
        return new CountryOptionsProvider();
    }

    @Bean
    public LanguageOptionsProvider languageOptionsProvider() {
        return new LanguageOptionsProvider();
    }

    @Component
    public static class CountryOptionsProvider {

        public List<SelectRadioOption> getCountries() {
            return Arrays.asList(
                    new SelectRadioOption("FR", "France"),
                    new SelectRadioOption("US", "United States"),
                    new SelectRadioOption("CA", "Canada")
            );
        }
    }

    @Component
    public static class LanguageOptionsProvider {

        public Map<String, String> getLanguages() {
            return Map.of("fr", "French", "en", "English");
        }
    }
}
