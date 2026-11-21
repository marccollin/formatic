package com.formatic.render.thymeleaf;

import com.formatic.render.FormLayout;
import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormTheme;
import com.formatic.render.internal.FieldView;
import com.formatic.render.internal.FormStyle;
import com.formatic.render.internal.FormView;
import com.formatic.render.internal.TemplatePaths;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Objects;

/** Renders an explicitly selected template with the standard Thymeleaf dialect. */
public final class ThymeleafFormRenderer implements FormRenderer {
    private final ITemplateEngine engine;
    private final String templateName;
    private final FormStyle style;

    public ThymeleafFormRenderer(String templateName) {
        this("", templateName, FormTheme.BOOTSTRAP, FormLayout.VERTICAL);
    }

    public ThymeleafFormRenderer(String prefix, String templateName, FormTheme theme, FormLayout layout) {
        this(defaultEngine(), prefix, templateName, theme, layout);
    }

    public ThymeleafFormRenderer(ITemplateEngine engine, String prefix, String templateName,
                                 FormTheme theme, FormLayout layout) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.templateName = TemplatePaths.resolve(prefix, templateName);
        this.style = new FormStyle(theme, layout);
    }

    private static ITemplateEngine defaultEngine() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        var engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public String render(FormRenderModel form) {
        FormView.validateAction(form.action());
        var context = new Context();
        context.setVariable("form", form);
        context.setVariable("style", style);
        context.setVariable("multipart", FormView.multipart(form));
        context.setVariable("fields", form.fields().stream().map(FieldView::new).toList());
        return engine.process(templateName, context);
    }
}
