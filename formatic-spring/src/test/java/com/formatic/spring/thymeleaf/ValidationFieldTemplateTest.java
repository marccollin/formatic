package com.formatic.spring.thymeleaf;

import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ValidationFieldTemplateTest.TestApplication.class)
@AutoConfigureMockMvc
class ValidationFieldTemplateTest {
    @Autowired
    private MockMvc mockMvc;

    static Stream<Object[]> fields() {
        return layouts()
                .flatMap(layout -> Stream.of("text", "email", "password", "phone", "url", "textarea",
                                "number", "date", "file", "select", "checkbox", "radio", "checkbox-multiple", "select-multiple")
                        .map(kind -> new Object[]{layout, kind}));
    }

    static Stream<String> layouts() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical");
    }

    @ParameterizedTest
    @MethodSource("fields")
    void rendersAllEscapedErrorsAndAccessibleLinks(String layout, String kind) throws Exception {
        String html = render(layout, kind, "field", true);
        assertThat(html).contains("id=\"value-help\"", "id=\"value-errors\"", "role=\"alert\"",
                        "Invalid &lt;value&gt; &amp; input", "Second error", "Helpful &lt;hint&gt;")
                .doesNotContain("Annotation error", "<value>", "style=\"display: none;\"");
        String invalidClass = layout.startsWith("bootstrap") ? "is-invalid" : "!border-red-500";
        for (String control : controls(html)) {
            assertThat(control).contains("aria-invalid=\"true\"",
                    "aria-describedby=\"value-help value-errors\"", invalidClass, "custom-control");
        }
        assertThat(count(html, "id=\"value-help\"")).isEqualTo(1);
        assertThat(count(html, "id=\"value-errors\"")).isEqualTo(1);
        if (kind.equals("radio") || kind.equals("checkbox-multiple")) {
            assertThat(html).contains("<fieldset", "<legend");
        }
    }

    @ParameterizedTest
    @MethodSource("fields")
    void errorsWithoutHelpHaveNoDanglingHelpReference(String layout, String kind) throws Exception {
        String html = render(layout, kind, "field", false);
        assertThat(html).doesNotContain("value-help");
        for (String control : controls(html)) {
            assertThat(control).contains("aria-describedby=\"value-errors\"", "aria-invalid=\"true\"");
        }
    }

    @ParameterizedTest
    @MethodSource("fields")
    void helpWithoutErrorsDoesNotMarkControlsInvalid(String layout, String kind) throws Exception {
        String html = render(layout, kind, "none", true);
        assertThat(html).contains("id=\"value-help\"");
        assertValid(html);
        for (String control : controls(html)) {
            assertThat(control).contains("aria-describedby=\"value-help\"");
        }
    }

    @ParameterizedTest
    @MethodSource("fields")
    void unrelatedAndGlobalErrorsDoNotMarkThisFieldInvalid(String layout, String kind) throws Exception {
        String html = render(layout, kind, "other", false);
        assertValid(html);
        assertThat(html).doesNotContain("aria-describedby=", "value-help", "Other error", "Global error");
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void postValidationRendersFullPageOrHtmxFragment(String layout) throws Exception {
        for (boolean htmx : List.of(false, true)) {
            String html = mockMvc.perform(post("/validation-test").param("layout", layout).param("value", "")
                            .header("HX-Request", Boolean.toString(htmx)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("A value is required", "aria-invalid=\"true\"",
                    "aria-describedby=\"value-help value-errors\"");
            assertThat(html.contains("<!DOCTYPE html>")).isEqualTo(!htmx);
        }
    }

    @ParameterizedTest
    @MethodSource("layouts")
    void hiddenFieldsDoNotRenderVisibleFeedback(String layout) throws Exception {
        String html = render(layout, "hidden", "field", true);
        assertThat(html).contains("type=\"hidden\"");
        assertThat(html).doesNotContain("aria-invalid=", "aria-describedby=", "value-help", "value-errors",
                "Invalid &lt;value&gt;");
    }

    private void assertValid(String html) {
        assertThat(html).doesNotContain("aria-invalid=", "value-errors", "is-invalid", "!border-red-500",
                "Annotation error", "Invalid &lt;value&gt;");
    }

    private List<String> controls(String html) {
        var matcher = Pattern.compile("<(?:input|select|textarea)\\b[^>]*\\bname=\"value\"[^>]*>").matcher(html);
        List<String> controls = new ArrayList<>();
        while (matcher.find()) {
            controls.add(matcher.group());
        }
        assertThat(controls).isNotEmpty();
        return controls;
    }

    private int count(String html, String literal) {
        return (int) Pattern.compile(Pattern.quote(literal)).matcher(html).results().count();
    }

    private String render(String layout, String kind, String errors, boolean help) throws Exception {
        return mockMvc.perform(get("/validation-test").param("layout", layout).param("kind", kind)
                        .param("errors", errors).param("help", Boolean.toString(help)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    static class TestApplication {
        @Bean
        ValidationController validationController() { return new ValidationController(); }
    }

    @Controller
    static class ValidationController {
        @GetMapping("/validation-test")
        String form(@RequestParam String layout, @RequestParam String kind, @RequestParam String errors,
                    @RequestParam boolean help, Model model) {
            FormData data = new FormData();
            model.addAttribute("formData", data);
            if (!errors.equals("none")) {
                BeanPropertyBindingResult result = new BeanPropertyBindingResult(data, "formData");
                if (errors.equals("field")) {
                    result.rejectValue("value", "invalid", "Invalid <value> & input");
                    result.rejectValue("value", "second", "Second error");
                } else {
                    result.rejectValue("other", "other", "Other error");
                    result.reject("global", "Global error");
                }
                model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "formData", result);
            }
            addField(layout, kind, help, model);
            return "validation-test-form";
        }

        @PostMapping("/validation-test")
        String submit(@Valid @ModelAttribute("formData") FormData data, BindingResult result,
                      @RequestParam String layout, @RequestHeader("HX-Request") boolean htmx, Model model) {
            addField(layout, "text", true, model);
            return htmx ? "validation-test-form :: validated-form" : "validation-test-form";
        }

        private void addField(String layout, String kind, boolean help, Model model) {
            FormFieldType type = FormFieldType.valueOf(kind.split("-")[0].toUpperCase(java.util.Locale.ROOT));
            FormFieldMetadata field = new FormFieldMetadata("value", type, "Value", 0);
            field.setMultiple(kind.endsWith("-multiple"));
            field.setHelpText(help ? "Helpful <hint>" : "");
            field.setErrorMessage("Annotation error");
            field.setCssClass("custom-control");
            field.setOptions(List.of(new SelectRadioOption("true", "Yes"), new SelectRadioOption("false", "No")));
            model.addAttribute("field", field);
            model.addAttribute("layout", layout);
            model.addAttribute("fragment", kind.split("-")[0] + "-field");
        }
    }

    public static class FormData {
        @NotBlank(message = "A value is required")
        private String value = "true";

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getOther() { return ""; }
    }
}
