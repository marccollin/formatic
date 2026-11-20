package com.formatic.spring.thymeleaf;

import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MaxLengthFieldTemplateTest {

    static Stream<Object[]> lengthConstraints() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical")
                .flatMap(layout -> Stream.of("text", "email", "password", "phone", "url", "textarea")
                        .flatMap(type -> Stream.of(Integer.MIN_VALUE, -1, 0, 1, 255, Integer.MAX_VALUE)
                                .map(maxLength -> new Object[]{layout, type, maxLength})));
    }

    @ParameterizedTest
    @MethodSource("lengthConstraints")
    void rendersOnlyNonNegativeMaxLength(String layout, String type, int maxLength) throws Exception {
        String template;
        try (var resource = getClass().getResourceAsStream(
                "/templates/" + layout + "/fields/" + type + "-field.html")) {
            assertThat(resource).isNotNull();
            template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }

        FormFieldMetadata field = new FormFieldMetadata();
        field.setName("text");
        field.setMaxLength(maxLength);
        field.setMinLength(-1);
        Context context = new Context();
        context.setVariable("field", field);
        context.setVariable("formData", new FormData());

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process("<div th:object=\"${formData}\">" + template + "</div>", context);

        var control = Pattern.compile("<(?:input|textarea)\\b[^>]*>").matcher(html);
        assertThat(control.find()).isTrue();
        if (maxLength < 0) {
            assertThat(control.group()).doesNotContain("maxlength=");
        } else {
            assertThat(control.group()).contains("maxlength=\"" + maxLength + "\"");
        }
        assertThat(control.group()).doesNotContain("minlength=");
    }

    public static class FormData {
        public String getText() { return "Existing value"; }
    }
}
