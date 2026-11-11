package com.formatic.core.processor;

import com.formatic.core.annotation.TextareaInput;
import com.formatic.core.annotation.handler.DateInputHandler;
import com.formatic.core.annotation.handler.NumberInputHandler;
import com.formatic.core.annotation.handler.EmailInputHandler;
import com.formatic.core.annotation.handler.PhoneInputHandler;
import com.formatic.core.annotation.handler.PasswordInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.annotation.handler.UrlInputHandler;
import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.core.annotation.handler.HiddenInputHandler;
import com.formatic.core.annotation.handler.TextareaInputHandler;
import com.formatic.core.form.FormFieldMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormMetadataProcessorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatedNumberMetadataMatchesReflection() throws Exception {
        assertGeneratedMetadataMatchesReflection("NumberForm", """
                package fixture;

                import com.formatic.core.annotation.NumberInput;

                public class NumberForm {
                    @NumberInput
                    private double amount;

                    @NumberInput(name = "price", label = "Price", min = -10.5, max = 100.0,
                        step = 0.01, placeholder = "Enter price", defaultValue = "1.25",
                        required = true, disabled = true, readonly = true,
                        cssClass = "amount", outerCssClass = "group", errorMessage = "Invalid price")
                    private double custom;

                    @NumberInput(min = 0, max = 0, step = 1)
                    private int zero;

                    @NumberInput(min = -Double.MAX_VALUE, max = Double.MAX_VALUE,
                        step = Double.MIN_VALUE)
                    private double extremes;

                }
                """, new NumberInputHandler(), List.of("amount", "custom", "zero", "extremes"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TextInput", "EmailInput", "UrlInput", "PhoneInput", "PasswordInput"})
    void generatedTextLikeMetadataMatchesReflection(String annotationName) throws Exception {
        FormFieldAnnotationHandler<?> handler = switch (annotationName) {
            case "TextInput" -> new TextInputHandler();
            case "EmailInput" -> new EmailInputHandler();
            case "UrlInput" -> new UrlInputHandler();
            case "PhoneInput" -> new PhoneInputHandler();
            case "PasswordInput" -> new PasswordInputHandler();
            default -> throw new IllegalArgumentException(annotationName);
        };
        String titleAttribute = annotationName.equals("TextInput") || annotationName.equals("UrlInput")
                ? ", title = \"Input title\"" : "";
        assertGeneratedMetadataMatchesReflection("TextLikeForm", """
                package fixture;

                import com.formatic.core.annotation.%s;

                public class TextLikeForm {
                    @%s
                    private String value;

                    @%s(
                        name = "customName",
                        label = "Custom label",
                        cssClass = "text-input",
                        outerCssClass = "form-group",
                        disabled = true,
                        required = true,
                        readonly = true,
                        placeholder = "Enter \\"value\\"",
                        defaultValue = "First line\\nSecond line",
                        minLength = 2,
                        maxLength = 120,
                        pattern = "\\\\d+",
                        errorMessage = "Invalid value"%s
                    )
                    private String custom;

                    @%s(minLength = 0, maxLength = 0)
                    private String zeroLength;
                }
                """.formatted(annotationName, annotationName, annotationName, titleAttribute, annotationName),
                handler, List.of("value", "custom", "zeroLength"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"soft", "hard", "off"})
    void generatedTextareaMetadataMatchesReflection(String wrap) throws Exception {
        assertGeneratedMetadataMatchesReflection("TextareaForm", """
                package fixture;

                import com.formatic.core.annotation.TextareaInput;

                public class TextareaForm {
                    @TextareaInput
                    private String comment;

                    @TextareaInput(
                        name = "description",
                        label = "Detailed description",
                        cssClass = "text-input",
                        outerCssClass = "form-group",
                        disabled = true,
                        required = true,
                        readonly = true,
                        placeholder = "Enter \\"details\\"",
                        defaultValue = "First line\\nSecond line",
                        minLength = 0,
                        maxLength = 120,
                        rows = 8,
                        cols = 60,
                        wrap = "%s"
                    )
                    private String details;
                }
                """.formatted(wrap), new TextareaInputHandler(), List.of("comment", "details"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "7", "0.5", "any"})
    void generatedDateMetadataMatchesReflection(String step) throws Exception {
        assertGeneratedMetadataMatchesReflection("DateForm", """
                package fixture;

                import com.formatic.core.annotation.DateInput;

                public class DateForm {
                    @DateInput
                    private String date;

                    @DateInput(
                        name = "birthDate",
                        label = "Birth date",
                        cssClass = "date-input",
                        outerCssClass = "form-group",
                        disabled = true,
                        required = true,
                        readonly = true,
                        placeholder = "Choose a \\"date\\"",
                        defaultValue = "2000-01-01",
                        min = "1900-01-01",
                        max = "2026-12-31",
                        step = "%s"
                    )
                    private String birthday;
                }
                """.formatted(step), new DateInputHandler(), List.of("date", "birthday"));
    }

    @Test
    void generatedHiddenMetadataMatchesReflection() throws Exception {
        assertGeneratedMetadataMatchesReflection("HiddenForm", """
                package fixture;

                import com.formatic.core.annotation.HiddenInput;

                public class HiddenForm {
                    @HiddenInput
                    private String id;

                    @HiddenInput(name = "token", defaultValue = "A\\"B\\\\C")
                    private String secret;
                }
                """, new HiddenInputHandler(), List.of("id", "secret"));
    }

    private void assertGeneratedMetadataMatchesReflection(
            String className, String sourceCode, FormFieldAnnotationHandler<?> handler,
            List<String> fieldNames) throws Exception {
        Path source = temporaryDirectory.resolve(className + ".java");
        Files.writeString(source, sourceCode, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path coreClasses = Path.of(TextareaInput.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    List.of("-classpath", coreClasses.toString(), "-d", temporaryDirectory.toString(),
                            "-s", temporaryDirectory.toString()),
                    null,
                    fileManager.getJavaFileObjects(source.toFile())
            );
            task.setProcessors(List.of(new FormMetadataProcessor()));
            assertThat(task.call()).as("Compiler diagnostics: %s", diagnostics.getDiagnostics()).isTrue();
        }

        assertThat(temporaryDirectory.resolve("fixture/" + className + "FormMetadata.java")).exists();
        try (URLClassLoader classLoader = new URLClassLoader(
                new URL[]{temporaryDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> formClass = classLoader.loadClass("fixture." + className);
            Class<?> generatedClass = classLoader.loadClass("fixture." + className + "FormMetadata");
            Object result = generatedClass.getMethod("getMetadata").invoke(null);
            assertThat(result).isInstanceOf(List.class);
            List<?> generatedMetadata = (List<?>) result;
            assertThat(generatedMetadata).hasSize(fieldNames.size());

            for (int index = 0; index < fieldNames.size(); index++) {
                String fieldName = fieldNames.get(index);
                FormFieldMetadata expected = handler.handle(formClass.getDeclaredField(fieldName));
                assertThat(new FormFieldMetadata(expected))
                        .usingRecursiveComparison()
                        .withComparatorForType(Double::compare, Double.class)
                        .isEqualTo(expected);
                assertThat(generatedMetadata.get(index))
                        .usingRecursiveComparison()
                        .withComparatorForType(Double::compare, Double.class)
                        .ignoringFields("order")
                        .isEqualTo(expected);
            }
        }
    }
}
