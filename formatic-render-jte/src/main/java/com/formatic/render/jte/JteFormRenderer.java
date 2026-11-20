package com.formatic.render.jte;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.internal.FormView;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/** Renders the bundled Bootstrap vertical form using precompiled JTE templates. */
public final class JteFormRenderer implements FormRenderer {
    private final TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var output = new StringOutput();
        engine.render("formatic/bootstrap/vertical/form.jte", form, output);
        return output.toString();
    }
}
