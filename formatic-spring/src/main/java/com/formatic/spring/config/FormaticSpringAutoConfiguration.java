package com.formatic.spring.config;

import com.formatic.core.annotation.handler.*;
import com.formatic.core.form.FormFieldMetadataBuilder;
import com.formatic.core.service.OptionsProviderRegistry;
import com.formatic.core.service.OptionsProviderService;
import com.formatic.spring.registry.SpringOptionsProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * Spring auto-configuration for the Formatic library.
 *
 * This class registers default beans (form field input handlers, services, and registries)
 * only if no other user-defined beans of the same type are found in the Spring context.
 * It also scans the base package to discover other Formatic components.
 *
 * The configuration is conditionally activated only if the class
 * `com.formatic.core.form.FormFieldMetadataBuilder` is present on the classpath.
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.formatic")
@ConditionalOnClass(name = "com.formatic.core.form.FormFieldMetadataBuilder")
public class FormaticSpringAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(FormaticSpringAutoConfiguration.class);

    public FormaticSpringAutoConfiguration() {
        logger.info("🚀 FormaticSpringAutoConfiguration is being loaded!");
    }

    @Bean
    @ConditionalOnMissingBean
    public TextInputHandler textInputHandler() {
        return new TextInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public TextareaInputHandler textareaInputHandler() {
        return new TextareaInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public DateInputHandler dateInputHandler() {
        return new DateInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public NumberInputHandler numberInputHandler() {
        return new NumberInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public UrlInputHandler urlInputHandler() {
        return new UrlInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public PhoneInputHandler phoneInputHandler() {
        return new PhoneInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public RadioInputHandler radioInputHandler(OptionsProviderService optionsProviderService) {
        return new RadioInputHandler(optionsProviderService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SelectInputHandler selectInputHandler(OptionsProviderService optionsProviderService) {
        return new SelectInputHandler(optionsProviderService);
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailInputHandler emailInputHandler() {
        return new EmailInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public FileInputHandler fileInputHandler() {
        return new FileInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordInputHandler passwordInputHandler() {
        return new PasswordInputHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public HiddenInputHandler hiddenInputHandler() {
        return new HiddenInputHandler();
    }


    @Bean
    public OptionsProviderRegistry optionsProviderRegistry(ApplicationContext applicationContext) {
        // Spring will automatically inject the ApplicationContext
        return new SpringOptionsProviderRegistry(applicationContext);
    }

    @Bean
    public OptionsProviderService optionsProviderService(OptionsProviderRegistry registry) {
        // Spring will inject the OptionsProviderRegistry bean we just defined (or another if present)
        return new OptionsProviderService(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public CheckBoxHandler checkBoxHandler(OptionsProviderService optionsProviderService) {
        return new CheckBoxHandler(optionsProviderService);
    }


    @Bean
    @ConditionalOnMissingBean
    public FormFieldMetadataBuilder formFieldMetadataBuilder(List<FormFieldAnnotationHandler<?>> handlers) {
        logger.info("🔧 Creating FormFieldMetadataBuilder with {} handlers", handlers.size());

        return new FormFieldMetadataBuilder(handlers);
    }
}
