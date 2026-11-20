package com.formatic.spring.thymeleaf;

import java.util.Map;
import java.util.stream.Collectors;

public class ThymeleafUtils {
    public static String formatDataAttributes(Map<String, String> attrs) {
        if (attrs == null || attrs.isEmpty()) return "";
        return attrs.entrySet().stream()
                .map(e -> "data-" + e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(","));
    }

    public static String formatAttributes2(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }
        return attributes.entrySet().stream()
                .map(e -> "data-" + e.getKey() + "=\"" + e.getValue() + "\"")
                .collect(Collectors.joining(" "));
    }

    public static String formatAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            sb.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
        }
        return sb.toString();
    }

    public static Map<String, String> filterEmptyValues(Map<String, String> attributes) {
        return attributes.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
