package com.formatic.render.internal;

import java.util.Objects;

/** Joins an application-configured folder and an explicitly selected template. */
public final class TemplatePaths {
    private TemplatePaths() {
    }

    public static String resolve(String prefix, String templateName) {
        String folder = Objects.requireNonNull(prefix, "template prefix").strip();
        if (folder.endsWith("/")) folder = folder.substring(0, folder.length() - 1);
        if (!prefix.isBlank()) validate(folder, "Template prefix");
        validate(Objects.requireNonNull(templateName, "template name"), "Template name");
        return folder.isEmpty() ? templateName : folder + "/" + templateName;
    }

    private static void validate(String path, String label) {
        if (path.indexOf('\\') >= 0 || path.indexOf(':') >= 0) {
            throw new IllegalArgumentException(label + " must be a relative path: " + path);
        }
        for (String segment : path.split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException(label + " must be a relative path: " + path);
            }
        }
    }
}
