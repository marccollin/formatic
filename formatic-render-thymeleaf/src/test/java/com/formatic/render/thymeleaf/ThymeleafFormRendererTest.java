package com.formatic.render.thymeleaf;

import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.render.FormLayout;
import com.formatic.render.FormRenderModel;
import com.formatic.render.FormTheme;
import com.formatic.render.RenderField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ThymeleafFormRendererTest {
    private static final String PREFIX = "templates/formatic/standalone/";

    @Test
    void rendersAllTypesAndEscapesValues() {
        var fields = Arrays.stream(FormFieldType.values()).map(type -> {
            var field = new FormFieldMetadata(type.name().toLowerCase(Locale.ROOT), type, "Test <label>", 0);
            field.setOptions(List.of(new SelectRadioOption("yes", "Yes")));
            return new RenderField(field, List.of("yes"), List.of());
        }).toList();
        String html = new ThymeleafFormRenderer(PREFIX + "form.html").render(
                new FormRenderModel("Title <test>", "/submit", fields));
        for (String type : List.of("text", "hidden", "email", "password", "tel", "number", "url",
                "file", "color", "date", "datetime-local", "time", "range", "radio", "checkbox")) {
            assertThat(html).contains("type=\"" + type + "\"");
        }
        assertThat(html).contains("Title &lt;test&gt;", "Test &lt;label&gt;", "<textarea", "<select",
                "enctype=\"multipart/form-data\"").doesNotContain("name=\"password\" value=", "name=\"file\" value=");
    }

    @ParameterizedTest
    @EnumSource(FormTheme.class)
    void rendersEachLayoutWithValuesAndErrors(FormTheme theme) {
        for (FormLayout layout : FormLayout.values()) {
            var field = new FormFieldMetadata("address.street", FormFieldType.TEXT, "Street", 0);
            field.setIdStyle(FieldIdStyle.SNAKE_CASE);
            field.setHelpText("Help & tips");
            String html = new ThymeleafFormRenderer(PREFIX, "form.html", theme, layout).render(
                    new FormRenderModel("Customer", "/customer", List.of(
                            new RenderField(field, List.of("<value>"), List.of("<error>"))),
                            List.of("Global <error>"), "Done", Map.of("_csrf", "abc&def")));
            assertThat(html).contains("name=\"address.street\"", "id=\"address_street\"", "&lt;value&gt;",
                    "&lt;error&gt;", "Global &lt;error&gt;", "Help &amp; tips", "abc&amp;def",
                    "aria-describedby=\"address_street-help address_street-errors\"");
            String expected = switch (theme) {
                case BOOTSTRAP -> layout == FormLayout.VERTICAL ? "row g-3" : "col-sm-2 col-form-label";
                case TAILWIND -> layout == FormLayout.VERTICAL ? "space-y-6" : "grid grid-cols-1 gap-6 md:grid-cols-2";
                case BULMA -> layout == FormLayout.VERTICAL ? "field" : "columns is-multiline";
            };
            assertThat(html).contains("class=\"" + expected + "\"");
        }
    }

    @Test
    void rendersBoundSelectionsAndOnlyEmitsMarkersForEnabledControls() {
        var choices = new FormFieldMetadata("choices", FormFieldType.SELECT, "Choices", 0);
        choices.setMultiple(true);
        var first = new SelectRadioOption("first", "First");
        first.setSelected(true);
        var second = new SelectRadioOption("second", "Second");
        choices.setOptions(List.of(first, second));
        var enabled = new FormFieldMetadata("enabled", FormFieldType.CHECKBOX, "Enabled", 0);
        var locked = new FormFieldMetadata("locked", FormFieldType.CHECKBOX, "Locked", 0);
        locked.setDisabled(true);
        String html = new ThymeleafFormRenderer(PREFIX + "form.html").render(new FormRenderModel(null, "", List.of(
                new RenderField(choices, List.of("second"), List.of()),
                new RenderField(enabled, List.of("false"), List.of()),
                new RenderField(locked, List.of("true"), List.of()))));
        assertThat(html).contains("name=\"_choices\"", "name=\"_enabled\"")
                .doesNotContain("name=\"_locked\"")
                .containsPattern("value=\"second\"\\s+selected")
                .doesNotContainPattern("value=\"first\"\\s+selected");
    }

    @Test
    void rendersAnExplicitCustomTemplate() {
        String html = new ThymeleafFormRenderer("custom/", "contact.html",
                FormTheme.BULMA, FormLayout.HORIZONTAL).render(new FormRenderModel("Custom <title>", "", List.of()));
        assertThat(html).contains("<h1>Custom &lt;title&gt;</h1>").doesNotContain("Submit");
    }

    @Test
    void requiresAnExistingExplicitTemplateAndRejectsExternalActions() {
        assertThatThrownBy(() -> new ThymeleafFormRenderer("missing.html")
                .render(new FormRenderModel(null, "", List.of()))).hasMessageContaining("missing.html");
        assertThatIllegalArgumentException().isThrownBy(() -> new ThymeleafFormRenderer(PREFIX + "form.html")
                .render(new FormRenderModel(null, "//example.com", List.of())));
    }
}
