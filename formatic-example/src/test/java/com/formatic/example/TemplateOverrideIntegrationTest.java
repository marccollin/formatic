package com.formatic.example;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = TemplateOverrideIntegrationTest.ConsumerApplication.class)
@AutoConfigureMockMvc
class TemplateOverrideIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationTemplateOverridesLibraryTemplateWhileOtherFieldsUseLibraryJar() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String textPath = "templates/bootstrap/forms/vertical/fields/text-field.html";
        String datePath = "templates/bootstrap/forms/vertical/fields/date-field.html";

        var textResources = Collections.list(classLoader.getResources(textPath));
        assertThat(textResources).hasSize(2);
        assertThat(textResources.get(0).getProtocol()).isEqualTo("file");
        assertThat(textResources.get(1).getProtocol()).isEqualTo("jar");
        var dateResource = classLoader.getResource(datePath);
        assertThat(dateResource).isNotNull();
        assertThat(dateResource.getProtocol()).isEqualTo("jar");

        mockMvc.perform(get("/consumer-form"))
                .andExpect(status().isOk())
                .andExpect(view().name("consumer-form"))
                .andExpect(content().string(containsString("class=\"consumer-custom-field\"")))
                .andExpect(content().string(containsString("value=\"Example customer\"")))
                .andExpect(content().string(containsString("type=\"date\"")))
                .andExpect(content().string(containsString("value=\"2026-11-09\"")))
                .andExpect(content().string(containsString("step=\"7\"")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    static class ConsumerApplication {
        @Bean
        ConsumerController consumerController() {
            return new ConsumerController();
        }
    }

    @Controller
    static class ConsumerController {
        @GetMapping("/consumer-form")
        String form(Model model) {
            FormFieldMetadata text = new FormFieldMetadata("name", FormFieldType.TEXT, "Customer name", 0);
            FormFieldMetadata date = new FormFieldMetadata("date", FormFieldType.DATE, "Date", 1);
            date.setDateStep("7");
            model.addAttribute("textField", text);
            model.addAttribute("dateField", date);
            model.addAttribute("formData", Map.of("name", "Example customer", "date", "2026-11-09"));
            return "consumer-form";
        }
    }
}
