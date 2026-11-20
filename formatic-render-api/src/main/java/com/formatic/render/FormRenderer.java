package com.formatic.render;

/** Renders a form as HTML without a servlet request or template-engine-specific model. */
@FunctionalInterface
public interface FormRenderer {
    String render(FormRenderModel form);
}
