package com.formatic.core.form;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses a conservative subset of HTML's Unicode-set regular expressions and
 * translates it to a whole-value Java matcher.
 */
public final class HtmlPattern {
    private static final int MAX_LENGTH = 4096;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_REPEAT = 10000;
    private final Pattern pattern;

    private HtmlPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    /** Compiles the supported portable subset, or throws for unsupported or invalid syntax. */
    public static HtmlPattern compile(String source) {
        if (source == null || source.isEmpty() || source.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("pattern must contain 1 to " + MAX_LENGTH + " characters");
        }
        try {
            Node expression = new Parser(source).parse();
            return new HtmlPattern(Pattern.compile(expression.javaRegex()));
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("invalid translated pattern: " + exception.getDescription(), exception);
        }
    }

    public boolean matches(CharSequence value) {
        return pattern.matcher(value).matches();
    }

    private interface Node {
        String javaRegex();
    }

    private record Token(String javaRegex) implements Node {}

    private record Sequence(List<Node> nodes) implements Node {
        public String javaRegex() {
            var result = new StringBuilder();
            nodes.forEach(node -> result.append(node.javaRegex()));
            return result.toString();
        }
    }

    private record Alternatives(List<Node> nodes) implements Node {
        public String javaRegex() {
            return String.join("|", nodes.stream().map(Node::javaRegex).toList());
        }
    }

    private record Group(Node child) implements Node {
        public String javaRegex() {
            return "(?:" + child.javaRegex() + ")";
        }
    }

    private record Repetition(Node child, String quantifier) implements Node {
        public String javaRegex() {
            return child.javaRegex() + quantifier;
        }
    }

    private static final class Parser {
        private final String source;
        private int index;

        private Parser(String source) {
            this.source = source;
        }

        private Node parse() {
            Node node = alternatives(0);
            if (index != source.length()) {
                throw error("unexpected character");
            }
            return node;
        }

        private Node alternatives(int depth) {
            var options = new ArrayList<Node>();
            options.add(sequence(depth));
            while (take('|')) {
                options.add(sequence(depth));
            }
            return new Alternatives(List.copyOf(options));
        }

        private Node sequence(int depth) {
            var nodes = new ArrayList<Node>();
            while (index < source.length() && peek() != ')' && peek() != '|') {
                Node atom = atom(depth);
                String quantifier = quantifier();
                nodes.add(quantifier.isEmpty() ? atom : new Repetition(atom, quantifier));
            }
            return new Sequence(List.copyOf(nodes));
        }

        private Node atom(int depth) {
            char current = source.charAt(index++);
            if (current == '(') {
                if (depth >= MAX_DEPTH) {
                    throw error("group nesting limit exceeded");
                }
                if (take('?') && !take(':')) {
                    throw error("lookarounds, named groups and flags are unsupported");
                }
                Node child = alternatives(depth + 1);
                if (!take(')')) {
                    throw error("unclosed group");
                }
                return new Group(child);
            }
            if (current == '[') {
                return characterClass();
            }
            if (current == '\\') {
                return escape();
            }
            if (current == '.') {
                return new Token("[^\\x{000A}\\x{000D}\\x{2028}\\x{2029}]");
            }
            if (asciiLiteral(current)) {
                return new Token(hex(current));
            }
            throw error("unsupported or misplaced character");
        }

        private Node characterClass() {
            boolean negated = take('^');
            var content = new StringBuilder(negated ? "[^" : "[");
            boolean any = false;
            while (index < source.length() && peek() != ']') {
                char start = source.charAt(index++);
                if (!classLiteral(start)) {
                    throw error("unsupported character class syntax");
                }
                content.append(hex(start));
                any = true;
                if (take('-')) {
                    if (index == source.length() || !classLiteral(peek())) {
                        throw error("unsupported or incomplete character range");
                    }
                    char end = source.charAt(index++);
                    if (end < start) {
                        throw error("invalid character range");
                    }
                    content.append('-').append(hex(end));
                }
            }
            if (!any || !take(']')) {
                throw error("empty or unclosed character class");
            }
            return new Token(content.append(']').toString());
        }

        private Node escape() {
            if (index == source.length()) {
                throw error("trailing escape");
            }
            char escaped = source.charAt(index++);
            return switch (escaped) {
                case 'd' -> new Token("[0-9]");
                case 'D' -> new Token("[^0-9]");
                case 'w' -> new Token("[A-Za-z0-9_]");
                case 'W' -> new Token("[^A-Za-z0-9_]");
                case '^', '$', '\\', '.', '*', '+', '?', '(', ')', '[', ']', '{', '}', '|', '/' ->
                        new Token(hex(escaped));
                default -> throw error("unsupported escape");
            };
        }

        private String quantifier() {
            if (index == source.length()) {
                return "";
            }
            char first = peek();
            if (first == '*' || first == '+' || first == '?') {
                index++;
                return String.valueOf(first) + (take('?') ? "?" : "");
            }
            if (first != '{') {
                return "";
            }
            int begin = index++;
            int min = count();
            int max = min;
            boolean comma = take(',');
            if (comma) {
                max = index < source.length() && Character.isDigit(peek()) ? count() : -1;
            }
            if (!take('}') || max != -1 && max < min) {
                throw error("invalid repetition");
            }
            String result = source.substring(begin, index);
            return result + (take('?') ? "?" : "");
        }

        private int count() {
            int start = index;
            int value = 0;
            while (index < source.length() && peek() >= '0' && peek() <= '9') {
                value = value * 10 + source.charAt(index++) - '0';
                if (value > MAX_REPEAT) {
                    throw error("repetition limit exceeded");
                }
            }
            if (index == start) {
                throw error("missing repetition count");
            }
            return value;
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            return source.charAt(index);
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException("pattern at offset " + index + ": " + message);
        }
    }

    private static boolean asciiLiteral(char value) {
        return value >= 0x20 && value <= 0x7e && "^$\\.*+?()[]{}|".indexOf(value) < 0;
    }

    private static boolean classLiteral(char value) {
        return value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9' || value == '_';
    }

    private static String hex(char value) {
        return String.format("\\x{%04X}", (int) value);
    }
}
