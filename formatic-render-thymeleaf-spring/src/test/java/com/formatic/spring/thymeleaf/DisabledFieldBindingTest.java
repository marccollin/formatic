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

@SpringBootTest(classes = DisabledFieldBindingTest.TestApplication.class)
@AutoConfigureMockMvc
class DisabledFieldBindingTest {
    @Autowired
    private MockMvc mockMvc;

    static Stream<Object[]> fields() {
        return layouts()
                .flatMap(layout -> Stream.of("checkbox", "radio", "select", "multiple-select")
                        .map(kind -> new Object[]{layout, kind}));
    }

    static Stream<String> layouts() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical");
    }

    @ParameterizedTest
    @MethodSource("fields")
    void disabledFieldsHaveNoResetMarkersAndPreserveExistingValues(String layout, String kind) throws Exception {
        String html = render(layout, kind, true, false);
        assertThat(html).doesNotContain("name=\"_choices\"", "name=\"_choice\"");
        var controls = Pattern.compile("<(?:input|select)\\b[^>]*\\bname=\"(?:choices|choice)\"[^>]*>")
                .matcher(html);
        int count = 0;
        while (controls.find()) {
            assertThat(controls.group()).contains("disabled=\"disabled\"");
            count++;
        }
        assertThat(count).isPositive();
        mockMvc.perform(post("/disabled-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choice").value("sport"))
                .andExpect(jsonPath("$.choices[0]").value("sport"));
    }

    @ParameterizedTest
    @MethodSource("fields")
    void enabledFieldsRetainOptionLevelDisabling(String layout, String kind) throws Exception {
        String html = render(layout, kind, false, true);
        String tag = kind.contains("select") ? "option" : "input";
        assertThat(elementWithValue(html, tag, "sport")).doesNotContain("disabled=");
        assertThat(elementWithValue(html, tag, "music")).contains("disabled=\"disabled\"");
        if (kind.contains("select")) {
            var select = Pattern.compile("<select\\b[^>]*>").matcher(html);
            assertThat(select.find()).isTrue();
            assertThat(select.group()).doesNotContain("disabled=");
        }
        String property = kind.equals("checkbox") || kind.equals("multiple-select") ? "choices" : "choice";
        mockMvc.perform(post("/disabled-test").param(property, "sport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(property.equals("choices") ? "$.choices[0]" : "$.choice").value("sport"));
    }

    @ParameterizedTest
    @MethodSource("fields")
    void enabledFieldsStillUpdateAndCollectionMarkersClearSelections(String layout, String kind) throws Exception {
        String html = render(layout, kind, false, false);
        String tag = kind.contains("select") ? "option" : "input";
        assertThat(elementWithValue(html, tag, "sport")).doesNotContain("disabled=");
        assertThat(elementWithValue(html, tag, "music")).doesNotContain("disabled=");
        boolean multiple = kind.equals("checkbox") || kind.equals("multiple-select");
        String property = multiple ? "choices" : "choice";
        mockMvc.perform(post("/disabled-test").param(property, "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(multiple ? "$.choices[0]" : "$.choice").value("music"));
        if (multiple) {
            assertThat(html).contains("name=\"_choices\"");
            mockMvc.perform(post("/disabled-test").param("_choices", "on"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.choices").isEmpty());
        }
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void allDisabledCheckboxOptionsHaveNoResetMarkers(String layout) throws Exception {
        String html = render(layout, "checkbox", false, true, true);
        assertThat(elementWithValue(html, "input", "sport")).contains("disabled=\"disabled\"");
        assertThat(elementWithValue(html, "input", "music")).contains("disabled=\"disabled\"");
        assertThat(html).doesNotContain("name=\"_choices\"");
        mockMvc.perform(post("/disabled-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choices[0]").value("sport"));
    }

    private String render(String layout, String kind, boolean disabled, boolean optionDisabled) throws Exception {
        return render(layout, kind, disabled, optionDisabled, false);
    }

    private String render(String layout, String kind, boolean disabled, boolean optionDisabled,
                          boolean allOptionsDisabled) throws Exception {
        return mockMvc.perform(get("/disabled-test").param("layout", layout).param("kind", kind)
                        .param("disabled", Boolean.toString(disabled))
                        .param("optionDisabled", Boolean.toString(optionDisabled))
                        .param("allOptionsDisabled", Boolean.toString(allOptionsDisabled)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private String elementWithValue(String html, String tag, String value) {
        var matcher = Pattern.compile("<" + tag + "\\b[^>]*\\bvalue=\"" + value + "\"[^>]*>").matcher(html);
        assertThat(matcher.find()).as("%s with value %s", tag, value).isTrue();
        return matcher.group();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    static class TestApplication {
        @Bean
        DisabledController disabledController() { return new DisabledController(); }
    }

    @Controller
    static class DisabledController {
        @ModelAttribute("formData")
        FormData data() { return new FormData(); }

        @GetMapping("/disabled-test")
        String form(@RequestParam String layout, @RequestParam String kind, @RequestParam boolean disabled,
                    @RequestParam boolean optionDisabled, @RequestParam boolean allOptionsDisabled, Model model) {
            boolean multiple = kind.equals("checkbox") || kind.equals("multiple-select");
            FormFieldType type = kind.contains("select") ? FormFieldType.SELECT
                    : kind.equals("radio") ? FormFieldType.RADIO : FormFieldType.CHECKBOX;
            FormFieldMetadata field = new FormFieldMetadata(multiple ? "choices" : "choice", type, "Choices", 0);
            field.setMultiple(multiple);
            field.setDisabled(disabled);
            SelectRadioOption sport = new SelectRadioOption("sport", "Sport");
            sport.setDisabled(allOptionsDisabled);
            SelectRadioOption music = new SelectRadioOption("music", "Music");
            music.setDisabled(optionDisabled);
            field.setOptions(List.of(sport, music));
            model.addAttribute("field", field);
            model.addAttribute("layout", layout);
            model.addAttribute("fragment", type.name().toLowerCase() + "-field");
            return "disabled-test-form";
        }

        @PostMapping("/disabled-test")
        @ResponseBody
        Map<String, Object> submit(@ModelAttribute("formData") FormData data) {
            return Map.of("choice", data.getChoice(), "choices", data.getChoices());
        }
    }

    public static class FormData {
        private String choice = "sport";
        private List<String> choices = new ArrayList<>(List.of("sport"));
        public String getChoice() { return choice; }
        public void setChoice(String choice) { this.choice = choice; }
        public List<String> getChoices() { return choices; }
        public void setChoices(List<String> choices) { this.choices = choices; }
    }
}
