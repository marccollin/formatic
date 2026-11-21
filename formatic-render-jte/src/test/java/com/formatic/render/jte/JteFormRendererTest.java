package com.formatic.render.jte;

import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.render.FormRenderModel;
import gg.jte.TemplateNotFoundException;
import com.formatic.render.RenderField;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JteFormRendererTest {
    private final JteFormRenderer renderer = new JteFormRenderer("formatic/form.jte");

    @Test
    void missingExplicitTemplateDoesNotFallBack() {
        var renderer = new JteFormRenderer("example/forms/missing.jte");
        assertThatThrownBy(() -> renderer.render(new FormRenderModel(null, "", List.of())))
                .isInstanceOf(TemplateNotFoundException.class)
                .hasMessageContaining("example/forms/missing.jte");
    }

    @Test
    void rendersAllControlTypesWithHtmlNames() {
        var fields = Arrays.stream(FormFieldType.values()).map(type -> {
            var field = field(type, type.name().toLowerCase(Locale.ROOT));
            field.setOptions(List.of(new SelectRadioOption("yes", "Yes")));
            return new RenderField(field, List.of("yes"), List.of());
        }).toList();
        String html = renderer.render(new FormRenderModel("Controls", "/controls", fields));
        for (String type : List.of("text", "hidden", "email", "password", "tel", "number", "url",
                "file", "color", "date", "datetime-local", "time", "range", "radio", "checkbox")) {
            assertThat(html).contains("type=\"" + type + "\"");
        }
        assertThat(html).contains("<textarea", "<select", "id=\"label\"", "name=\"auto\"",
                "action=\"/controls\"", "enctype=\"multipart/form-data\"");
        assertThat(html).doesNotContain("name=\"password\" value=", "name=\"file\" value=");
    }

    @Test
    void rendersDottedNamesAccessibleErrorsAndEscapedText() {
        var field = field(FormFieldType.TEXT, "address.street");
        field.setIdStyle(FieldIdStyle.SNAKE_CASE);
        field.setLabel("Street <name>");
        field.setHelpText("Help & tips");
        field.setOuterCssClass("col-md-6");
        field.setMaxLength(80);
        String html = renderer.render(new FormRenderModel("Address", "/address",
                List.of(new RenderField(field, List.of("\"Home\""), List.of("Invalid <street>"))),
                List.of("Form <error>"), null, Map.of("_csrf", "abc&def")));
        assertThat(html).contains("name=\"address.street\"", "id=\"address_street\"", "for=\"address_street\"",
                "class=\"col-md-6\"", "maxlength=\"80\"", "aria-invalid=\"true\"",
                "aria-describedby=\"address_street-help address_street-errors\"",
                "Street &lt;name&gt;", "Help &amp; tips", "Invalid &lt;street&gt;", "Form &lt;error&gt;",
                "name=\"_csrf\" value=\"abc&amp;def\"")
                .doesNotContain("Invalid <street>", "multipart/form-data", "value=\"\"Home\"\"");
    }

    @Test
    void usesBoundSelectionsAndOnlyEmitsMarkersForEnabledControls() {
        var checkbox = field(FormFieldType.CHECKBOX, "enabled");
        var disabled = field(FormFieldType.CHECKBOX, "locked");
        disabled.setDisabled(true);
        var choices = field(FormFieldType.SELECT, "choices");
        choices.setMultiple(true);
        var first = new SelectRadioOption("first", "First");
        first.setSelected(true);
        var second = new SelectRadioOption("second", "Second");
        choices.setOptions(List.of(first, second));
        String html = renderer.render(new FormRenderModel(null, "/choices", List.of(
                new RenderField(checkbox, List.of("false"), List.of()),
                new RenderField(disabled, List.of("true"), List.of()),
                new RenderField(choices, List.of("second"), List.of()))));
        assertThat(html).contains("name=\"_enabled\"", "name=\"_choices\"")
                .doesNotContain("name=\"_locked\"")
                .containsPattern("value=\"second\"\\s+selected")
                .doesNotContainPattern("value=\"first\"\\s+selected");
    }

    @Test
    void rejectsActionsOutsideTheApplication() {
        for (String action : List.of("https://example.com", "//example.com", "javascript:alert(1)")) {
            assertThatIllegalArgumentException().isThrownBy(() ->
                    renderer.render(new FormRenderModel(null, action, List.of())));
        }
    }

    private static FormFieldMetadata field(FormFieldType type, String name) {
        return new FormFieldMetadata(name, type, name, 0);
    }
}
