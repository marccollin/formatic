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

class TextareaFieldTemplateTest {

    static Stream<Object[]> wrappingModes() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal", "tailwind/forms/vertical")
                .flatMap(layout -> Stream.of(null, "soft", "hard", "off")
                        .map(wrap -> new Object[]{layout, wrap}));
    }

    @ParameterizedTest
    @MethodSource("wrappingModes")
    void rendersWrappingMode(String layout, String wrap) throws Exception {
        String template;
        try (var resource = getClass().getResourceAsStream("/templates/" + layout + "/fields/textarea-field.html")) {
            assertThat(resource).isNotNull();
            template = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }

        FormFieldMetadata field = new FormFieldMetadata();
        field.setName("comment");
        field.setCols(40);
        field.setRows(4);
        field.setWrap(wrap);
        Context context = new Context();
        context.setVariable("field", field);
        context.setVariable("formData", Map.of("comment", "Text"));

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        String html = engine.process("<div th:object=\"${formData}\">" + template + "</div>", context);

        if (wrap == null) {
            assertThat(html).doesNotContain("wrap=");
        } else {
            assertThat(html).contains("wrap=\"" + wrap + "\"");
        }
        assertThat(html).contains("<textarea", "cols=\"40\"", "rows=\"4\"");
    }
}
