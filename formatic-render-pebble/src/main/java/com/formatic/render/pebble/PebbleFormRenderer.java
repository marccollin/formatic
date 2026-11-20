package com.formatic.render.pebble;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.internal.FieldView;
import com.formatic.render.internal.FormView;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;

/** Renders the bundled Bootstrap vertical form using Pebble templates. */
public final class PebbleFormRenderer implements FormRenderer {
    private final PebbleTemplate template = new PebbleEngine.Builder()
            .loader(new ClasspathLoader(PebbleFormRenderer.class.getClassLoader()))
            .autoEscaping(true)
            .build()
            .getTemplate("formatic/pebble/bootstrap/vertical/form.peb");

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var output = new StringWriter();
        try {
            template.evaluate(output, Map.of(
                    "form", form,
                    "multipart", FormView.multipart(form),
                    "fields", form.fields().stream().map(FieldView::new).toList()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the Pebble form", e);
        }
        return output.toString();
    }
}
