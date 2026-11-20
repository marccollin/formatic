package com.formatic.render.jte;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import java.net.URI;

/** Renders the bundled Bootstrap vertical form using precompiled JTE templates. */
public final class JteFormRenderer implements FormRenderer {
    private final TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);

    @Override
    public String render(FormRenderModel form) {
        String action = form.action();
        if (!action.isEmpty()) {
            URI uri = URI.create(action);
            if (!action.startsWith("/") || action.startsWith("//") || uri.getRawAuthority() != null) {
                throw new IllegalArgumentException("Form action must be empty or a root-relative application URL");
            }
        }
        var output = new StringOutput();
        engine.render("formatic/bootstrap/vertical/form.jte", form, output);
        return output.toString();
    }
}
