package com.formatic.render.pebble;

import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FormFieldType;
import com.formatic.core.form.SelectRadioOption;
import com.formatic.render.FormRenderModel;
import com.formatic.render.FormLayout;
import com.formatic.render.FormTheme;
import com.formatic.render.FormRenderer;
import com.formatic.render.RenderField;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PebbleFormRendererTest {
    private final FormRenderer renderer = new PebbleFormRenderer("formatic/pebble/form.peb");

    @Test
    void loadsAnApplicationTemplateFromTheConfiguredFolder() {
        var renderer = new PebbleFormRenderer("example/forms/", "contact.peb",
                FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
        assertThat(renderer.render(new FormRenderModel("Contact <form>", "/contact", List.of())))
                .contains("<h1>Contact &lt;form&gt;</h1>", "action=\"/contact\"")
                .doesNotContain("Submit");
    }

    @Test
    void missingExplicitTemplateDoesNotFallBack() {
        assertThatThrownBy(() -> new PebbleFormRenderer("example/forms/missing.peb"))
                .hasMessageContaining("example/forms/missing.peb");
    }

    @Test
    void rendersAllControlTypesWithoutRedisplayingSensitiveValues() {
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
                "action=\"/controls\"", "enctype=\"multipart/form-data\"")
                .doesNotContain("name=\"password\" value=", "name=\"file\" value=");
    }

    @Test
    void escapesTextAndAttributesAndLinksErrorsToTheNestedInput() {
        var street = field(FormFieldType.TEXT, "address.street");
        street.setIdStyle(FieldIdStyle.SNAKE_CASE);
        street.setLabel("Street <name>");
        street.setHelpText("Help & tips");
        street.setOuterCssClass("col-md-6");
        street.setMaxLength(80);
        String html = renderer.render(new FormRenderModel("Address <form>", "/address?a=1&b=2",
                List.of(new RenderField(street, List.of("\"Home\""), List.of("Invalid <street>"))),
                List.of("Form <error>"), "Done <message>", Map.of("_csrf", "abc&def")));
        assertThat(html).contains("name=\"address.street\"", "id=\"address_street\"", "for=\"address_street\"",
                "class=\"col-md-6\"", "maxlength=\"80\"", "aria-invalid=\"true\"",
                "aria-describedby=\"address_street-help address_street-errors\"",
                "Address &lt;form&gt;", "Street &lt;name&gt;", "Help &amp; tips", "Invalid &lt;street&gt;",
                "Form &lt;error&gt;", "Done &lt;message&gt;", "name=\"_csrf\" value=\"abc&amp;def\"",
                "action=\"/address?a=1&amp;b=2\"")
                .doesNotContain("Invalid <street>", "multipart/form-data", "value=\"\"Home\"\"");
    }

    @Test
    void overridesDefaultSelectionsAndOnlyEmitsMarkersForEnabledControls() {
        var enabled = field(FormFieldType.CHECKBOX, "enabled");
        var locked = field(FormFieldType.CHECKBOX, "locked");
        locked.setDisabled(true);
        var choices = field(FormFieldType.SELECT, "choices");
        choices.setMultiple(true);
        var first = new SelectRadioOption("first", "First");
        first.setSelected(true);
        var second = new SelectRadioOption("second", "Second");
        choices.setOptions(List.of(first, second));
        String html = renderer.render(new FormRenderModel(null, "", List.of(
                new RenderField(enabled, List.of("false"), List.of()),
                new RenderField(locked, List.of("true"), List.of()),
                new RenderField(choices, List.of("second"), List.of()))));
        assertThat(html).contains("action=\"\"", "name=\"_enabled\"", "name=\"_choices\"")
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
