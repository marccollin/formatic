package com.formatic.example;

import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.example.controller.CustomerRenderController;
import com.formatic.spring.config.FormaticProperties;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = CustomerRenderControllerIntegrationTest.TestApplication.class, properties = {
        "formatic.jte.template-prefix=formatic/",
        "formatic.pebble.template-prefix=formatic/pebble/",
        "formatic.thymeleaf.template-prefix=templates/formatic/standalone/"
})
@AutoConfigureMockMvc
class CustomerRenderControllerIntegrationTest {
    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"jte, JTE", "pebble, Pebble", "thymeleaf, Thymeleaf"})
    void rendersNestedFieldsWithContextRelativeAction(String engine, String title) throws Exception {
        String path = "/demo/" + engine + "/customer";
        mvc.perform(get(path).contextPath("/demo"))
                .andExpect(status().isOk())
                .andExpect(view().name("formatic-" + engine + "-BOOTSTRAP-VERTICAL"))
                .andExpect(content().string(containsString("<h2>" + title + " customer form</h2>")))
                .andExpect(content().string(containsString("action=\"" + path + "\"")))
                .andExpect(content().string(containsString("name=\"address.street\"")))
                .andExpect(content().string(containsString("id=\"address_street\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jte", "pebble", "thymeleaf"})
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
    @ValueSource(strings = {"jte", "pebble", "thymeleaf"})
    void acceptsValidNestedSubmission(String engine) throws Exception {
        mvc.perform(post("/{engine}/customer", engine)
                        .param("name", "Customer").param("address.street", "Main").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Main\"")))
                .andExpect(content().string(containsString("Customer details received")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jte", "pebble", "thymeleaf"})
    void localizesLabelForEachRequestOnGetAndPost(String engine) throws Exception {
        mvc.perform(get("/{engine}/customer", engine).locale(Locale.FRENCH))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Rue</")))
                .andExpect(content().string(not(containsString("#{customer.street}"))));
        mvc.perform(post("/{engine}/customer", engine).locale(Locale.FRENCH)
                        .param("name", "Customer").param("address.street", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Rue</")))
                .andExpect(content().string(containsString("Street is required.")));
        mvc.perform(get("/{engine}/customer", engine).locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Street</")));
    }

    @ParameterizedTest
    @MethodSource("styles")
    void rendersAndPreservesEachStyleOnValidationFailure(String engine, String theme, String layout,
                                                         String asset, String layoutClass, String errorClass)
            throws Exception {
        String path = "/" + engine + "/customer";
        String action = theme.equals("BOOTSTRAP") && layout.equals("VERTICAL")
                ? path : path + "?theme=" + theme + "&amp;layout=" + layout;
        mvc.perform(get(path).param("theme", theme).param("layout", layout))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(asset)))
                .andExpect(content().string(containsString("class=\"" + layoutClass + "\"")))
                .andExpect(content().string(containsString("action=\"" + action + "\"")));
        mvc.perform(post(path).param("theme", theme).param("layout", layout)
                        .param("name", "Customer").param("address.street", "").param("address.city", "Quebec"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"" + layoutClass + "\"")))
                .andExpect(content().string(containsString("class=\"" + errorClass + "\"")))
                .andExpect(content().string(containsString("aria-describedby=\"address_street-errors\"")))
                .andExpect(content().string(containsString("Street is required.")))
                .andExpect(content().string(containsString("value=\"Quebec\"")));
    }

    static Stream<Arguments> styles() {
        return Stream.of("jte", "pebble", "thymeleaf").flatMap(engine -> Stream.of(
                Arguments.of(engine, "BOOTSTRAP", "VERTICAL", "bootstrap.min.css",
                        "row g-3", "invalid-feedback d-block"),
                Arguments.of(engine, "BOOTSTRAP", "HORIZONTAL", "bootstrap.min.css",
                        "col-sm-2 col-form-label", "invalid-feedback d-block"),
                Arguments.of(engine, "TAILWIND", "VERTICAL", "@tailwindcss/browser@4",
                        "space-y-6", "mt-1 text-sm text-red-600"),
                Arguments.of(engine, "TAILWIND", "HORIZONTAL", "@tailwindcss/browser@4",
                        "grid grid-cols-1 gap-6 md:grid-cols-2", "mt-1 text-sm text-red-600"),
                Arguments.of(engine, "BULMA", "VERTICAL", "bulma.min.css",
                        "field", "help is-danger"),
                Arguments.of(engine, "BULMA", "HORIZONTAL", "bulma.min.css",
                        "columns is-multiline", "help is-danger")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @EnableConfigurationProperties(FormaticProperties.class)
    @Import(CustomerRenderController.class)
    static class TestApplication {
        @Bean
        ReflectionFormFieldMetadataBuilder metadataBuilder() {
            return new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler()), FieldIdStyle.SNAKE_CASE);
        }
    }
}
