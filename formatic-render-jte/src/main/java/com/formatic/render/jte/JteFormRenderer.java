package com.formatic.render.jte;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormLayout;
import com.formatic.render.FormTheme;
import com.formatic.render.internal.FormStyle;
import com.formatic.render.internal.FormView;
import com.formatic.render.internal.TemplatePaths;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import java.util.Map;
import java.util.Objects;

/** Renders forms using precompiled JTE templates. */
public final class JteFormRenderer implements FormRenderer {
    private final TemplateEngine engine;
    private final String templateName;
    private final FormStyle style;

    public JteFormRenderer(String templateName) {
        this("", templateName, FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
    }

    public JteFormRenderer(String templatePrefix, String templateName, FormTheme theme, FormLayout layout) {
        this(TemplateEngine.createPrecompiled(ContentType.Html), templatePrefix, templateName, theme, layout);
    }

    public JteFormRenderer(TemplateEngine engine, String templatePrefix, String templateName,
                           FormTheme theme, FormLayout layout) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.templateName = TemplatePaths.resolve(templatePrefix, templateName);
        style = new FormStyle(theme, layout);
    }

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var output = new StringOutput();
        engine.render(templateName, Map.of("form", form, "style", style), output);
        return output.toString();
    }
}
