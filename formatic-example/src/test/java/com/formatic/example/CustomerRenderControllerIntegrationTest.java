package com.formatic.example;

import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.example.controller.CustomerRenderController;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CustomerRenderControllerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class CustomerRenderControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"jte, JTE", "pebble, Pebble"})
    void rendersNestedFieldsWithContextRelativeAction(String engine, String title) throws Exception {
        String path = "/demo/" + engine + "/customer";
        mvc.perform(get(path).contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h2>" + title + " customer form</h2>")))
                .andExpect(content().string(containsString("action=\"" + path + "\"")))
                .andExpect(content().string(containsString("name=\"address.street\"")))
                .andExpect(content().string(containsString("id=\"address_street\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jte", "pebble"})
    void redisplaysNestedValidationErrorsAndRetainsOtherSubmittedValues(String engine) throws Exception {
        mvc.perform(post("/{engine}/customer", engine)
                        .param("name", "Customer").param("address.street", "").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"address_street-errors\"")))
                .andExpect(content().string(containsString("Street is required.")))
                .andExpect(content().string(containsString("value=\"Quebec\"")))
                .andExpect(content().string(not(containsString("Customer details received"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jte", "pebble"})
    void acceptsValidNestedSubmission(String engine) throws Exception {
        mvc.perform(post("/{engine}/customer", engine)
                        .param("name", "Customer").param("address.street", "Main").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Main\"")))
                .andExpect(content().string(containsString("Customer details received")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @Import(CustomerRenderController.class)
    static class TestApplication {
        @Bean
        ReflectionFormFieldMetadataBuilder metadataBuilder() {
            return new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler()), FieldIdStyle.SNAKE_CASE);
        }
    }
}
