package com.formatic.spring.render;

import com.formatic.render.FormRenderModel;
import com.formatic.render.FormRenderer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/** Exposes an application's explicitly registered form renderers as MVC views. */
public final class FormRendererViewResolver implements ViewResolver, Ordered {
    public static final String FORM_MODEL_ATTRIBUTE = "form";
    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";
    private final Map<String, FormRenderer> renderers;

    public FormRendererViewResolver(Map<String, FormRenderer> renderers) {
        this.renderers = Map.copyOf(renderers);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        FormRenderer renderer = renderers.get(viewName);
        if (renderer == null) return null;
        return new View() {
            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public void render(Map<String, ?> model, HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
                if (model == null || !(model.get(FORM_MODEL_ATTRIBUTE) instanceof FormRenderModel form)) {
                    throw new IllegalStateException("View '" + viewName + "' requires a FormRenderModel named '"
                            + FORM_MODEL_ATTRIBUTE + "'");
                }
                String html = renderer.render(form);
                response.setContentType(CONTENT_TYPE);
                response.getWriter().write(html);
            }
        };
    }
}
