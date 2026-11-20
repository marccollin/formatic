package com.formatic.render.pebble;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormLayout;
import com.formatic.render.FormTheme;
import com.formatic.render.internal.FieldView;
import com.formatic.render.internal.FormStyle;
import com.formatic.render.internal.FormView;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;

/** Renders forms using bundled Pebble templates. */
public final class PebbleFormRenderer implements FormRenderer {
    private final PebbleTemplate template = new PebbleEngine.Builder()
            .loader(new ClasspathLoader(PebbleFormRenderer.class.getClassLoader()))
            .autoEscaping(true)
            .build()
            .getTemplate("formatic/pebble/form.peb");
    private final FormStyle style;

    public PebbleFormRenderer() {
        this(FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
    }

    public PebbleFormRenderer(FormTheme theme, FormLayout layout) {
        style = new FormStyle(theme, layout);
    }

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var output = new StringWriter();
        try {
            template.evaluate(output, Map.of(
                    "form", form,
                    "style", style,
                    "multipart", FormView.multipart(form),
                    "fields", form.fields().stream().map(FieldView::new).toList()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the Pebble form", e);
        }
        return output.toString();
    }
}
