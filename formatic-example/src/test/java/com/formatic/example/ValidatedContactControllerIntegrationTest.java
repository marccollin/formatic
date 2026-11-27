package com.formatic.example;

import com.formatic.core.annotation.handler.EmailInputHandler;
import com.formatic.core.annotation.handler.PhoneInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.example.controller.ValidatedContactController;
import com.formatic.spring.config.FormaticProperties;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ValidatedContactControllerIntegrationTest.TestApplication.class,
        properties = "formatic.thymeleaf.template-prefix=templates/formatic/standalone/")
@AutoConfigureMockMvc
class ValidatedContactControllerIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void showsTheFormWithAContextRelativeAction() throws Exception {
        mvc.perform(get("/demo/validated-contact").contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/demo/validated-contact\"")))
                .andExpect(content().string(containsString("name=\"name\"")))
                .andExpect(content().string(containsString("name=\"email\"")))
                .andExpect(content().string(containsString("name=\"phone\"")));
    }

    @Test
    void invalidHtmlRendersFieldErrorsAndRetainsSubmittedValues() throws Exception {
        mvc.perform(post("/validated-contact").param("name", "A").param("email", "invalid")
                        .param("phone", "+33 1 23 45 67 89"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The value is shorter than the minimum length.")))
                .andExpect(content().string(containsString("Must be a valid email address.")))
                .andExpect(content().string(containsString("value=\"+33 1 23 45 67 89\"")))
                .andExpect(content().string(not(containsString("Contact received"))));
        mvc.perform(post("/validated-contact").param("name", "").param("email", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A value is required.")));
    }

    @Test
    void validHtmlDisplaysSuccess() throws Exception {
        mvc.perform(post("/validated-contact").param("name", "Alice").param("email", "alice@example.com")
                        .param("phone", "+33 1 23 45 67 89"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contact received (not saved).")));
    }

    @Test
    void jsonReturnsFieldErrorsAndAcceptsValidValues() throws Exception {
        mvc.perform(post("/api/validated-contact").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A","email":"invalid","phone":"+33 1 23 45 67 89"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message")
                        .value(org.hamcrest.Matchers.contains("The value is shorter than the minimum length.")))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')].message")
                        .value(org.hamcrest.Matchers.contains("Must be a valid email address.")));
        mvc.perform(post("/api/validated-contact").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","email":"alice@example.com","phone":"+33 1 23 45 67 89"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @EnableConfigurationProperties(FormaticProperties.class)
    @Import(ValidatedContactController.class)
    static class TestApplication {
        @Bean
        ReflectionFormFieldMetadataBuilder metadataBuilder() {
            return new ReflectionFormFieldMetadataBuilder(
                    List.of(new TextInputHandler(), new EmailInputHandler(), new PhoneInputHandler()),
                    FieldIdStyle.SNAKE_CASE);
        }
    }
}
