package com.formatic.spring.thymeleaf;

import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DateFieldTemplateTest {

    static Stream<Object[]> dateSteps() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical")
                .flatMap(layout -> Stream.of(null, "", "7", "0.5", "any")
                        .map(step -> new Object[]{layout, step}));
    }

    @ParameterizedTest
    @MethodSource("dateSteps")
    void rendersDateStepWithoutChangingNumericStep(String layout, String step) throws Exception {
        String template;
        try (var resource = getClass().getResourceAsStream("/templates/" + layout + "/fields/date-field.html")) {
            assertThat(resource).isNotNull();
            template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }

        FormFieldMetadata field = new FormFieldMetadata();
        field.setName("date");
        field.setDateStep(step);
        field.setStep(0.25);
        Context context = new Context();
        context.setVariable("field", field);
        context.setVariable("formData", Map.of("date", "2026-11-09"));

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process("<div th:object=\"${formData}\">" + template + "</div>", context);

        if (step == null || step.isEmpty()) {
            assertThat(html).doesNotContain("step=");
        } else {
            assertThat(html).contains("step=\"" + step + "\"");
        }
        assertThat(html).contains("type=\"date\"").doesNotContain("step=\"0.25\"");
        assertThat(field.getStep()).isEqualTo(0.25);
    }
}
