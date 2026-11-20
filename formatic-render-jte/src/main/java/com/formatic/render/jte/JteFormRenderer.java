package com.formatic.render.jte;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormLayout;
import com.formatic.render.FormTheme;
import com.formatic.render.internal.FormStyle;
import com.formatic.render.internal.FormView;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import java.util.Map;

/** Renders forms using precompiled JTE templates. */
public final class JteFormRenderer implements FormRenderer {
    private final TemplateEngine engine = TemplateEngine.createPrecompiled(ContentType.Html);
    private final FormStyle style;

    public JteFormRenderer() {
        this(FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
    }

    public JteFormRenderer(FormTheme theme, FormLayout layout) {
        style = new FormStyle(theme, layout);
    }

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var output = new StringOutput();
        engine.render("formatic/form.jte", Map.of("form", form, "style", style), output);
        return output.toString();
    }
}
