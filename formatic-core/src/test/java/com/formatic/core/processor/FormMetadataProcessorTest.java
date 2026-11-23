package com.formatic.core.processor;

import com.formatic.core.annotation.TextareaInput;
import com.formatic.core.annotation.ColorInput;
import com.formatic.core.annotation.DateTimeInput;
import com.formatic.core.annotation.RangeInput;
import com.formatic.core.annotation.TimeInput;
import com.formatic.core.annotation.handler.ColorInputHandler;
import com.formatic.core.annotation.handler.DateTimeInputHandler;
import com.formatic.core.annotation.handler.DateInputHandler;
import com.formatic.core.annotation.handler.RangeInputHandler;
import com.formatic.core.annotation.handler.NumberInputHandler;
import com.formatic.core.annotation.handler.EmailInputHandler;
import com.formatic.core.annotation.handler.PhoneInputHandler;
import com.formatic.core.annotation.handler.PasswordInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.annotation.handler.UrlInputHandler;
import com.formatic.core.annotation.handler.FormFieldAnnotationHandler;
import com.formatic.core.annotation.handler.HiddenInputHandler;
import com.formatic.core.annotation.handler.TextareaInputHandler;
import com.formatic.core.annotation.handler.TimeInputHandler;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
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
    void generatedNestedMetadataMatchesRuntimeBuildersAndIsNotMutatedByIdStyle() throws Exception {
        Path source = temporaryDirectory.resolve("Customer.java");
        Files.writeString(source, """
                package fixture;
                import com.formatic.core.annotation.*;
                public class Customer {
                    @TextInput private String name;
                    @NestedForm private Address billing;
                    @NestedForm private Address shipping;
                    public static class Address {
                        @TextInput(required = true) private String street;
                        @NestedForm private Location location;
                        @FileInput(accept = ".pdf", multiple = true, disabled = true) private Object file;
                        @CheckboxInput(options = {"sport:Sport"}) private java.util.List<String> interests;
                        @SelectInput(options = {"Paris:Paris", "London:London"}, required = true,
                            defaultValue = "Paris") private String city;
                        @CheckboxInput(defaultChecked = true) private boolean subscribed;
                        @CheckboxInput(defaultChecked = true) private String enabled;
                        @CheckboxInput private Boolean optional;
                        @RadioInput(options = {"visa:Visa", "cash:Cash"}, defaultValue = "cash") private String payment;
                    }
                    public static class Location {
                        @TextInput private String city;
                    }
                }
                """);
        compile(source, true);
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{temporaryDirectory.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("fixture.Customer");
            Class<?> addressType = loader.loadClass("fixture.Customer$Address");
            for (FieldIdStyle style : FieldIdStyle.values()) {
                var options = org.mockito.Mockito.mock(com.formatic.core.service.OptionsProviderService.class);
                var reflection = new ReflectionFormFieldMetadataBuilder(List.of(new TextInputHandler(),
                        new com.formatic.core.annotation.handler.FileInputHandler(),
                        new com.formatic.core.annotation.handler.CheckBoxHandler(options),
                        new com.formatic.core.annotation.handler.SelectInputHandler(options),
                        new com.formatic.core.annotation.handler.RadioInputHandler(options)), style);
                var generated = new CompileTimeFormFieldMetadataBuilder(null, reflection, style);
                for (Class<?> dto : List.of(type, addressType)) {
                    assertThat(generated.buildMetadata(dto))
                            .usingRecursiveComparison().ignoringFields("order")
                            .withComparatorForType(Double::compare, Double.class)
                            .isEqualTo(reflection.buildMetadata(dto));
                }
            }
            List<?> original = (List<?>) loader.loadClass("fixture.CustomerFormMetadata")
                    .getMethod("getMetadata").invoke(null);
            assertThat(((FormFieldMetadata) original.get(1)).getId()).isEqualTo("billing.street");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "@NestedForm private Invalid child;",
            "@NestedForm private java.util.List<Invalid> children;",
            "@NestedForm @TextInput private Child child; static class Child {}",
            "@NestedForm private String text;"
    })
    void rejectsInvalidNestedDeclarationsAtCompileTime(String declaration) throws Exception {
        Path source = temporaryDirectory.resolve("Invalid.java");
        Files.writeString(source, """
                package fixture;
                import com.formatic.core.annotation.*;
                public class Invalid { %s }
                """.formatted(declaration));
        compile(source, false);
    }

    private void compile(Path source, boolean expectedSuccess) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path coreClasses = Path.of(TextareaInput.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = compiler.getTask(null, manager, diagnostics,
                    List.of("-classpath", coreClasses.toString(), "-d", temporaryDirectory.toString(),
                            "-s", temporaryDirectory.toString()), null, manager.getJavaFileObjects(source.toFile()));
            task.setProcessors(List.of(new FormMetadataProcessor()));
            assertThat(task.call()).as("Compiler diagnostics: %s", diagnostics.getDiagnostics()).isEqualTo(expectedSuccess);
            if (!expectedSuccess) {
                assertThat(diagnostics.getDiagnostics().toString()).contains("@NestedForm");
            }
        }
    }

    @Test
    void nestedConstraintDiagnosticsIncludeTheParentBindingPath() throws Exception {
        Path source = temporaryDirectory.resolve("Invalid.java");
        Files.writeString(source, """
                package fixture;
                import com.formatic.core.annotation.*;
                public class Invalid {
                    @NestedForm private Address address;
                    static class Address {
                        @TextInput(minLength = 10, maxLength = 2) private String street;
                    }
                }
                """);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Path coreClasses = Path.of(TextareaInput.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = compiler.getTask(null, manager, diagnostics,
                    List.of("-classpath", coreClasses.toString(), "-d", temporaryDirectory.toString(),
                            "-s", temporaryDirectory.toString()), null, manager.getJavaFileObjects(source.toFile()));
            task.setProcessors(List.of(new FormMetadataProcessor()));
            assertThat(task.call()).isFalse();
            assertThat(diagnostics.getDiagnostics().toString()).contains("fixture.Invalid.address.street");
        }
    }

    @Test
    void generatedDataKindsMatchRuntimeInference() throws Exception {
        assertGeneratedMetadataMatchesReflection("DataKinds", """
                package fixture;
                import com.formatic.core.annotation.TextInput;
                public class DataKinds<T> {
                    @TextInput private String text;
                    @TextInput private int count;
                    @TextInput private java.math.BigDecimal amount;
                    @TextInput private Boolean checked;
                    @TextInput private Choice choice;
                    @TextInput private java.util.List<Long> numbers;
                    @TextInput private String[] names;
                    @TextInput private java.util.List<?> unknownItems;
                    @TextInput private java.util.List rawItems;
                    @TextInput private T[] genericItems;
                    @TextInput private java.util.Map<String, String> unsupported;
                    enum Choice { FIRST, SECOND }
                }
                """, new TextInputHandler(), List.of("text", "count", "amount", "checked",
                "choice", "numbers", "names", "unknownItems", "rawItems", "genericItems", "unsupported"));
    }

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

    @Test
    void generatedColorMetadataMatchesReflectionForOpaqueAndAlphaPickers() throws Exception {
        assertGeneratedMetadataMatchesReflection("ColorForm", """
                package fixture;

                import com.formatic.core.annotation.ColorInput;

                public class ColorForm {
                    @ColorInput(defaultValue = "#336699")
                    private String opaque;

                    @ColorInput(name = "overlay", label = "Overlay color", alpha = true,
                        required = true, disabled = true, cssClass = "color-input",
                        outerCssClass = "color-field", defaultValue = "#336699cc")
                    private String translucent;
                }
                """, new ColorInputHandler(), List.of("opaque", "translucent"));
    }

    @Test
    void generatedDateTimeMetadataMatchesReflection() throws Exception {
        assertGeneratedMetadataMatchesReflection("DateTimeForm", """
                package fixture;

                import com.formatic.core.annotation.DateTimeInput;

                public class DateTimeForm {
                    @DateTimeInput(min = "2025-01-01T09:00", max = "2025-12-31T17:00",
                        step = "60", defaultValue = "2025-03-08T10:30", required = true)
                    private String appointment;
                }
                """, new DateTimeInputHandler(), List.of("appointment"));
    }

    @Test
    void generatedTimeMetadataMatchesReflection() throws Exception {
        assertGeneratedMetadataMatchesReflection("TimeForm", """
                package fixture;

                import com.formatic.core.annotation.TimeInput;

                public class TimeForm {
                    @TimeInput(min = "09:00", max = "17:00", step = "300",
                        defaultValue = "09:15", required = true)
                    private String meetingTime;
                }
                """, new TimeInputHandler(), List.of("meetingTime"));
    }

    @Test
    void generatedRangeMetadataMatchesReflection() throws Exception {
        assertGeneratedMetadataMatchesReflection("RangeForm", """
                package fixture;

                import com.formatic.core.annotation.RangeInput;

                public class RangeForm {
                    @RangeInput(min = 0, max = 100, step = 5, defaultValue = "50")
                    private double rating;
                }
                """, new RangeInputHandler(), List.of("rating"));
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
