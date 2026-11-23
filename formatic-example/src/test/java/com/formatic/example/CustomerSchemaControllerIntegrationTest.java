package com.formatic.example;

import com.formatic.example.controller.CustomerSchemaController;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CustomerSchemaControllerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class CustomerSchemaControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void returnsCombinedNestedDefinitionUnderAContextPath() throws Exception {
        mvc.perform(get("/demo/api/forms/customer").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.schema['$schema']").value("https://json-schema.org/draft/2020-12/schema"))
                .andExpect(jsonPath("$.schema.properties.address.properties.street.type").value("string"))
                .andExpect(jsonPath("$.schema.properties.address.required[0]").value("street"))
                .andExpect(jsonPath("$.ui.version").value(1))
                .andExpect(jsonPath("$.ui.fields[0].path").value("name"))
                .andExpect(jsonPath("$.ui.fields[1].path").value("address.street"))
                .andExpect(jsonPath("$.ui.fields[1].widget").value("text"))
                .andExpect(jsonPath("$.uiSchema").doesNotExist())
                .andExpect(jsonPath("$.action").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void localizesLabelsPerRequestWithoutChangingCachedMetadata() throws Exception {
        mvc.perform(get("/api/forms/customer").header("Accept-Language", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ui.fields[1].label").value("Rue"))
                .andExpect(jsonPath("$.schema.properties.address.properties.street.title").value("Rue"));
        mvc.perform(get("/api/forms/customer").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ui.fields[1].label").value("Street"))
                .andExpect(jsonPath("$.schema.properties.address.properties.street.title").value("Street"));
    }

    @Configuration
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @Import(CustomerSchemaController.class)
    static class TestApplication {
        @Bean
        ReflectionFormFieldMetadataBuilder metadataBuilder() {
            return new ReflectionFormFieldMetadataBuilder(java.util.List.of(new TextInputHandler()));
        }
    }
}
