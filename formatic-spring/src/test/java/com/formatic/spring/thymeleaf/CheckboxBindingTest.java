package com.formatic.spring.thymeleaf;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CheckboxBindingTest.TestApplication.class)
@AutoConfigureMockMvc
class CheckboxBindingTest {

    @Autowired
    private MockMvc mockMvc;

    static Stream<Object[]> simpleCheckboxes() {
        return layouts().flatMap(layout -> Stream.of("enabled", "optional", "stringValue")
                .flatMap(field -> Stream.of("true", "false", "null")
                        .map(initial -> new Object[]{layout, field, initial})));
    }

    static Stream<String> layouts() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical");
    }

    @ParameterizedTest
    @MethodSource("simpleCheckboxes")
    void rendersAndBindsSimpleCheckbox(String layout, String field, String initial) throws Exception {
        String html = render(layout, field, initial, false);
        String checkbox = input(html, field);
        assertThat(checkbox).contains("type=\"checkbox\"", "value=\"true\"", "id=\"" + field + "\"");
        assertThat(checkbox.contains("checked=\"checked\"")).isEqualTo(initial.equals("true"));
        assertThat(input(html, "_" + field)).contains("type=\"hidden\"");
        assertThat(html).contains("for=\"" + field + "\"");

        mockMvc.perform(post("/checkbox-test")
                        .param("initial", initial)
                        .param("_" + field, "on")
                        .param(field, "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + field).value(field.equals("stringValue") ? "true" : true));

        mockMvc.perform(post("/checkbox-test")
                        .param("initial", initial)
                        .param("_" + field, "on"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$." + field).value(field.equals("stringValue") ? null : false));
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void disabledCheckboxDoesNotEmitResetMarker(String layout) throws Exception {
        String html = render(layout, "enabled", "true", true);
        assertThat(input(html, "enabled")).contains("disabled=\"disabled\"");
        assertThat(html).doesNotContain("name=\"_enabled\"");

        mockMvc.perform(post("/checkbox-test").param("initial", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void collectionCheckboxesKeepTheirOptionValues(String layout) throws Exception {
        String html = render(layout, "interests", "false", false);
        assertThat(input(html, "interests")).contains("value=\"sport\"", "checked=\"checked\"");
        assertThat(html).contains("value=\"music\"");

        mockMvc.perform(post("/checkbox-test")
                        .param("_interests", "on")
                        .param("interests", "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interests[0]").value("music"));
    }

    private String render(String layout, String field, String initial, boolean disabled) throws Exception {
        return mockMvc.perform(get("/checkbox-test")
                        .param("layout", layout)
                        .param("fieldName", field)
                        .param("initial", initial)
                        .param("disabled", Boolean.toString(disabled)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String input(String html, String name) {
        var matcher = Pattern.compile("<input\\b[^>]*\\bname=\"" + Pattern.quote(name) + "\"[^>]*>")
                .matcher(html);
        assertThat(matcher.find()).as("Input %s in %s", name, html).isTrue();
        return matcher.group();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    static class TestApplication {
        @Bean
        CheckboxController checkboxController() {
            return new CheckboxController();
        }
    }

    @Controller
    static class CheckboxController {
        @ModelAttribute("formData")
        FormData formData(@RequestParam(defaultValue = "false") String initial) {
            FormData data = new FormData();
            data.setEnabled(Boolean.parseBoolean(initial));
            data.setOptional(initial.equals("null") ? null : Boolean.valueOf(initial));
            data.setStringValue(initial.equals("null") ? null : initial);
            data.setInterests(new ArrayList<>(List.of("sport")));
            return data;
        }

        @GetMapping("/checkbox-test")
        String form(@RequestParam String layout, @RequestParam String fieldName,
                    @RequestParam boolean disabled, Model model) {
            FormFieldMetadata field = new FormFieldMetadata(fieldName, FormFieldType.CHECKBOX, "Label", 0);
            field.setDisabled(disabled);
            field.setMultiple(fieldName.equals("interests"));
            field.setOptions(List.of(new SelectRadioOption("sport", "Sport"), new SelectRadioOption("music", "Music")));
            model.addAttribute("field", field);
            model.addAttribute("layout", layout);
            return "checkbox-test-form";
        }

        @PostMapping("/checkbox-test")
        @ResponseBody
        Map<String, Object> submit(@ModelAttribute("formData") FormData data) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("enabled", data.isEnabled());
            result.put("optional", data.getOptional());
            result.put("stringValue", data.getStringValue());
            result.put("interests", data.getInterests());
            return result;
        }
    }

    public static class FormData {
        private boolean enabled;
        private Boolean optional;
        private String stringValue;
        private List<String> interests;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Boolean getOptional() { return optional; }
        public void setOptional(Boolean optional) { this.optional = optional; }
        public String getStringValue() { return stringValue; }
        public void setStringValue(String stringValue) { this.stringValue = stringValue; }
        public List<String> getInterests() { return interests; }
        public void setInterests(List<String> interests) { this.interests = interests; }
    }
}
