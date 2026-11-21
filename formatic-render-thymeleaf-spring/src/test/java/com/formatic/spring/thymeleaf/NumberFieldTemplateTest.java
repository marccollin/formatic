package com.formatic.spring.thymeleaf;

import com.formatic.core.annotation.NumberInput;
import com.formatic.core.annotation.handler.NumberInputHandler;
import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NumberFieldTemplateTest {
    static Stream<Object[]> constraints() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical")
                .flatMap(layout -> Stream.of(
                        new double[]{Double.NaN, Double.NaN, Double.NaN},
                        new double[]{-10.5, 100, 0.01},
                        new double[]{0, 0, 1},
                        new double[]{-Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE},
                        new double[]{Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY})
                        .map(values -> new Object[]{layout, values[0], values[1], values[2]}));
    }

    @ParameterizedTest
    @MethodSource("constraints")
    void rendersOnlyFiniteNumericConstraints(String layout, double min, double max, double step) throws Exception {
        String template;
        try (var resource = getClass().getResourceAsStream("/templates/" + layout + "/fields/number-field.html")) {
            assertThat(resource).isNotNull();
            template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
        FormFieldMetadata field = new NumberInputHandler().handle(NumberForm.class.getDeclaredField("amount"));
        assertThat(field.getMin()).isNaN();
        assertThat(field.getMax()).isNaN();
        assertThat(field.getStep()).isNaN();
        field.setMin(min);
        field.setMax(max);
        field.setStep(step);

        Context context = new Context();
        context.setVariable("field", field);
        context.setVariable("formData", new NumberForm());
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process("<div th:object=\"${formData}\">" + template + "</div>", context);

        assertConstraint(html, "min", min);
        assertConstraint(html, "max", max);
        assertConstraint(html, "step", step);
        assertThat(html).contains("type=\"number\"", "value=\"1.25\"")
                .doesNotContain("NaN", "Infinity");
    }

    private void assertConstraint(String html, String attribute, double value) {
        if (Double.isFinite(value)) {
            assertThat(html).contains(" " + attribute + "=\"" + value + "\"");
        } else {
            assertThat(html).doesNotContain(" " + attribute + "=");
        }
    }

    public static class NumberForm {
        @NumberInput
        private double amount = 1.25;

        public double getAmount() {
            return amount;
        }
    }
}
