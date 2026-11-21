package com.formatic.example.controller;

import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.example.dto.CustomerForm;
import com.formatic.render.FormLayout;
import com.formatic.render.FormRenderer;
import com.formatic.render.FormTheme;
import com.formatic.render.jte.JteFormRenderer;
import com.formatic.render.pebble.PebbleFormRenderer;
import com.formatic.render.thymeleaf.ThymeleafFormRenderer;
import com.formatic.spring.config.FormaticProperties;
import com.formatic.spring.render.FormRendererViewResolver;
import com.formatic.spring.render.SpringFormRenderModels;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping(path = "/{engine:jte|pebble|thymeleaf}/customer", produces = "text/html;charset=UTF-8")
@Import(CustomerRenderController.Views.class)
public class CustomerRenderController {
    private final ReflectionFormFieldMetadataBuilder metadata;
    private final MessageSource messages;

    public CustomerRenderController(ReflectionFormFieldMetadataBuilder metadata, MessageSource messages) {
        this.metadata = metadata;
        this.messages = messages;
    }

    @GetMapping
    public ModelAndView show(@PathVariable("engine") String engine,
                       @RequestParam(name = "theme", defaultValue = "BOOTSTRAP") FormTheme theme,
                       @RequestParam(name = "layout", defaultValue = "VERTICAL") FormLayout layout,
                       HttpServletRequest request, Locale locale) {
        return view(new Style(engine, theme, layout),
                new BeanPropertyBindingResult(new CustomerForm(), "formData"), request, locale, null);
    }

    @PostMapping
    public ModelAndView submit(@PathVariable("engine") String engine,
                         @RequestParam(name = "theme", defaultValue = "BOOTSTRAP") FormTheme theme,
                         @RequestParam(name = "layout", defaultValue = "VERTICAL") FormLayout layout,
                         @Valid @ModelAttribute("formData") CustomerForm form, BindingResult binding,
                         HttpServletRequest request, Locale locale) {
        return view(new Style(engine, theme, layout), binding, request, locale,
                binding.hasErrors() ? null : "Customer details received (not saved).");
    }

    private ModelAndView view(Style style, BindingResult binding, HttpServletRequest request, Locale locale, String success) {
        String path = request.getContextPath() + "/" + style.engine() + "/customer";
        String action = style.theme() == FormTheme.BOOTSTRAP && style.layout() == FormLayout.VERTICAL
                ? path : path + "?theme=" + style.theme() + "&layout=" + style.layout();
        var form = SpringFormRenderModels.fromBinding(style.title(),
                action, metadata.buildMetadata(CustomerForm.class),
                binding, messages, locale, success, Map.of());
        return new ModelAndView(style.viewName(), FormRendererViewResolver.FORM_MODEL_ATTRIBUTE, form);
    }

    private static FormRenderer page(Style style, FormRenderer renderer) {
        return form -> """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>%s</title>
                    %s
                </head>
                <body>
                <main style="max-width: 72rem; margin: auto; padding: 1rem">
                %s
                """.formatted(style.title(), assets(style.theme()), styleLinks(style))
                + renderer.render(form) + "</main></body></html>";
    }

    private static String styleLinks(Style selected) {
        var links = new ArrayList<String>();
        for (FormTheme theme : FormTheme.values()) {
            for (FormLayout layout : FormLayout.values()) {
                String label = theme + " " + layout.name().toLowerCase(Locale.ROOT);
                links.add(theme == selected.theme() && layout == selected.layout()
                        ? "<strong aria-current=\"page\">" + label + "</strong>"
                        : "<a href=\"?theme=" + theme + "&amp;layout=" + layout + "\">" + label + "</a>");
            }
        }
        return "<nav aria-label=\"Form style\" style=\"margin-bottom: 1rem\">"
                + String.join(" | ", links) + "</nav>";
    }

    private static String assets(FormTheme theme) {
        return switch (theme) {
            case BOOTSTRAP -> """
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.7/dist/css/bootstrap.min.css"
                          integrity="sha384-LN+7fdVzj6u52u30Kp6M/trliBMCMKTyK833zpbD+pXdCLuTusPj697FH4R/5mcr"
                          crossorigin="anonymous">
                    """;
            case TAILWIND -> """
                    <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
                    """;
            case BULMA -> """
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css">
                    """;
        };
    }

    @Configuration(proxyBeanMethods = false)
    static class Views {
        @Bean
        FormRendererViewResolver customerFormViews(FormaticProperties properties) {
            var renderers = new HashMap<String, FormRenderer>();
            for (FormTheme theme : FormTheme.values()) {
                for (FormLayout layout : FormLayout.values()) {
                    var jte = new Style("jte", theme, layout);
                    var pebble = new Style("pebble", theme, layout);
                    var thymeleaf = new Style("thymeleaf", theme, layout);
                    renderers.put(jte.viewName(), page(jte, new JteFormRenderer(
                            properties.getJte().getTemplatePrefix(), "form.jte", theme, layout)));
                    renderers.put(pebble.viewName(), page(pebble, new PebbleFormRenderer(
                            properties.getPebble().getTemplatePrefix(), "form.peb", theme, layout)));
                    renderers.put(thymeleaf.viewName(), page(thymeleaf, new ThymeleafFormRenderer(
                            properties.getThymeleaf().getTemplatePrefix(), "form.html", theme, layout)));
                }
            }
            return new FormRendererViewResolver(renderers);
        }
    }

    private record Style(String engine, FormTheme theme, FormLayout layout) {
        String title() {
            return switch (engine) {
                case "jte" -> "JTE customer form";
                case "pebble" -> "Pebble customer form";
                case "thymeleaf" -> "Thymeleaf customer form";
                default -> throw new IllegalStateException("Unknown engine: " + engine);
            };
        }

        String viewName() {
            return "formatic-" + engine + "-" + theme + "-" + layout;
        }
    }
}
