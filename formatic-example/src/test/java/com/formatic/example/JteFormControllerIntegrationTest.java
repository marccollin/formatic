package com.formatic.example;

import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.example.controller.JteFormController;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(classes = JteFormControllerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class JteFormControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void rendersNestedFieldsWithContextRelativeAction() throws Exception {
        mvc.perform(get("/demo/jte/customer").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/demo/jte/customer\"")))
                .andExpect(content().string(containsString("name=\"address.street\"")))
                .andExpect(content().string(containsString("id=\"address_street\"")));
    }

    @Test
    void redisplaysNestedValidationErrorsAndRetainsOtherSubmittedValues() throws Exception {
        mvc.perform(post("/jte/customer")
                        .param("name", "Customer").param("address.street", "").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-describedby=\"address_street-errors\"")))
                .andExpect(content().string(containsString("Street is required.")))
                .andExpect(content().string(containsString("value=\"Quebec\"")))
                .andExpect(content().string(not(containsString("Customer details received"))));
    }

    @Test
    void acceptsValidNestedSubmission() throws Exception {
        mvc.perform(post("/jte/customer")
                        .param("name", "Customer").param("address.street", "Main").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Main\"")))
                .andExpect(content().string(containsString("Customer details received")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @Import(JteFormController.class)
    static class TestApplication {
        @Bean
        ReflectionFormFieldMetadataBuilder metadataBuilder() {
            return new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler()), FieldIdStyle.SNAKE_CASE);
        }
    }
}
