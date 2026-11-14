package com.formatic.spring.config;

import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.service.OptionsProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormaticPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void defaultsToDottedIdsAndRejectsInvalidConfiguration() {
        runner.run(context -> assertThat(context.getBean(FormaticProperties.class).getNested().getIdStyle())
                .isEqualTo(FieldIdStyle.DOT));
        runner.withPropertyValues("formatic.nested.id-style=INVALID")
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @EnumSource(FieldIdStyle.class)
    void allAutoConfiguredBuilderFactoriesUseTheConfiguredStyle(FieldIdStyle style) {
        runner.withPropertyValues("formatic.nested.id-style=" + style).run(context -> {
            FormaticProperties properties = context.getBean(FormaticProperties.class);
            FormaticSpringAutoConfiguration configuration = new FormaticSpringAutoConfiguration();
            var reflection = configuration.formFieldMetadataBuilder(List.of(new TextInputHandler()), properties);
            var bytecode = configuration.bytecodeFormFieldMetadataBuilder(List.of(new TextInputHandler()), properties);
            var generated = configuration.compileTimeFormFieldMetadataBuilder(
                    org.mockito.Mockito.mock(OptionsProviderService.class), reflection, properties);
            for (var builder : List.of(reflection, bytecode, generated)) {
                assertThat(builder.buildMetadata(Customer.class)).extracting(FormFieldMetadata::getName)
                        .containsExactly("address.street");
                assertThat(builder.buildMetadata(Customer.class)).extracting(FormFieldMetadata::getId)
                        .containsExactly(style.format("address.street"));
            }
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FormaticProperties.class)
    static class PropertiesConfiguration {}

    public static class Customer {
        @NestedForm private Address address;
    }

    public static class Address {
        @TextInput private String street;
    }
}
