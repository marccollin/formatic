package com.formatic.core.processor;

import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.DateInput;
import com.formatic.core.annotation.NumberInput;
import com.formatic.core.annotation.handler.*;
import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.ConstraintValidationException;
import com.formatic.core.form.FormConstraintValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstraintValidationTest {
    @TempDir
    Path directory;

    static Stream<Object[]> invalidAnnotations() {
        return Stream.of(
                new Object[]{"TextInput", "minLength = -2", "minLength"},
                new Object[]{"EmailInput", "maxLength = -2", "maxLength"},
                new Object[]{"UrlInput", "minLength = 10, maxLength = 9", "minLength"},
                new Object[]{"PhoneInput", "maxLength = -2", "maxLength"},
                new Object[]{"PasswordInput", "minLength = 4, maxLength = 3", "minLength"},
                new Object[]{"TextareaInput", "minLength = -2", "minLength"},
                new Object[]{"NumberInput", "min = Double.NEGATIVE_INFINITY", "min"},
                new Object[]{"NumberInput", "max = Double.POSITIVE_INFINITY", "max"},
                new Object[]{"NumberInput", "step = Double.POSITIVE_INFINITY", "step"},
                new Object[]{"NumberInput", "step = 0", "step"},
                new Object[]{"NumberInput", "step = -0.5", "step"},
                new Object[]{"NumberInput", "min = 2, max = 1", "min"},
                new Object[]{"DateInput", "min = \"0000-01-01\"", "min"},
                new Object[]{"DateInput", "max = \"2023-02-29\"", "max"},
                new Object[]{"DateInput", "min = \"2025-01-01\", max = \"2024-12-31\"", "min"},
                new Object[]{"DateInput", "step = \"0\"", "step"},
                new Object[]{"DateInput", "step = \"-1\"", "step"},
                new Object[]{"DateInput", "step = \"NaN\"", "step"},
                new Object[]{"DateInput", "step = \"1e9999\"", "step"},
                new Object[]{"DateInput", "step = \"1e-9999\"", "step"},
                new Object[]{"DateInput", "step = \"1 day\"", "step"}
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAnnotations")
    void compilerAndRuntimeBuildersRejectSameInvalidConfiguration(String annotation, String attributes, String attribute)
            throws Exception {
        String source = """
                package fixture;
                import com.formatic.core.annotation.%s;
                public class InvalidForm {
                    @%s(%s)
                    public String value;
                }
                """.formatted(annotation, annotation, attributes);
        Path file = directory.resolve("InvalidForm.java");
        Files.writeString(file, source);
        DiagnosticCollector<JavaFileObject> diagnostics = compile(file, true);
        assertThat(diagnostics.getDiagnostics()).anySatisfy(diagnostic -> {
            assertThat(diagnostic.getKind()).isEqualTo(Diagnostic.Kind.ERROR);
            assertThat(diagnostic.getMessage(null)).contains("fixture.InvalidForm.value", attribute);
            assertThat(diagnostic.getLineNumber()).isEqualTo(5);
        });
        assertThat(directory.resolve("fixture/InvalidFormFormMetadata.java")).doesNotExist();

        assertThat(compile(file, false).getDiagnostics())
                .noneMatch(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> type = loader.loadClass("fixture.InvalidForm");
            List<FormFieldAnnotationHandler<?>> handlers = List.of(
                    new TextInputHandler(), new EmailInputHandler(), new UrlInputHandler(), new PhoneInputHandler(),
                    new PasswordInputHandler(), new TextareaInputHandler(), new NumberInputHandler(), new DateInputHandler());
            var reflection = new ReflectionFormFieldMetadataBuilder(handlers);
            var bytecode = new BytecodeFormFieldMetadataBuilder(handlers);
            for (var builder : List.of(reflection, bytecode)) {
                for (int attempt = 0; attempt < 2; attempt++) {
                    assertThatThrownBy(() -> builder.buildMetadata(type))
                            .isInstanceOf(ConstraintValidationException.class)
                            .hasMessageContaining("fixture.InvalidForm.value")
                            .hasMessageContaining(attribute);
                }
            }
        }
    }

    @Test
    void invalidPreviouslyGeneratedMetadataIsNotSilentlySwallowed() {
        var builder = new CompileTimeFormFieldMetadataBuilder(null, new ReflectionFormFieldMetadataBuilder(List.of()));
        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> builder.buildMetadata(InvalidGeneratedForm.class))
                    .isInstanceOf(ConstraintValidationException.class)
                    .hasMessageContaining("InvalidGeneratedForm.amount")
                    .hasMessageContaining("step");
        }
    }

    @Test
    void compilerReportsAllInvalidFieldsWithoutGeneratingPartialMetadata() throws Exception {
        Path file = directory.resolve("InvalidForm.java");
        Files.writeString(file, """
                package fixture;
                import com.formatic.core.annotation.*;
                public class InvalidForm {
                    @TextInput(maxLength = -2) public String first;
                    @NumberInput(step = 0) public int second;
                }
                """);
        var diagnostics = compile(file, true).getDiagnostics();
        assertThat(diagnostics).filteredOn(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .hasSize(2)
                .anySatisfy(diagnostic -> assertThat(diagnostic.getMessage(null)).contains("InvalidForm.first", "maxLength"))
                .anySatisfy(diagnostic -> assertThat(diagnostic.getMessage(null)).contains("InvalidForm.second", "step"));
        assertThat(directory.resolve("fixture/InvalidFormFormMetadata.java")).doesNotExist();
    }

    @Test
    void allBuildersAcceptValidConstraintsAndCacheMetadata() {
        List<FormFieldAnnotationHandler<?>> handlers = List.of(
                new TextInputHandler(), new NumberInputHandler(), new DateInputHandler());
        var reflection = new ReflectionFormFieldMetadataBuilder(handlers);
        var bytecode = new BytecodeFormFieldMetadataBuilder(handlers);
        var generated = new CompileTimeFormFieldMetadataBuilder(null, reflection);
        for (var builder : List.of(reflection, bytecode, generated)) {
            var metadata = builder.buildMetadata(ValidGeneratedForm.class);
            assertThat(metadata).hasSize(3);
            assertThat(metadata).isSameAs(builder.buildMetadata(ValidGeneratedForm.class));
            assertThat(metadata).anySatisfy(field -> {
                assertThat(field.getName()).isEqualTo("amount");
                assertThat(field.getStep()).isEqualTo(0.5);
            });
        }
    }

    @Test
    void permitsSentinelsBoundariesAndHtmlDates() {
        assertThatCode(() -> {
            FormConstraintValidator.validateLengths("x", -1, -1);
            FormConstraintValidator.validateLengths("x", 0, 0);
            FormConstraintValidator.validateLengths("x", Integer.MAX_VALUE, Integer.MAX_VALUE);
            FormConstraintValidator.validateNumber("x", Double.NaN, Double.NaN, Double.NaN);
            FormConstraintValidator.validateNumber("x", -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE);
            FormConstraintValidator.validateDate("x", "", "", "");
            FormConstraintValidator.validateDate("x", "0001-01-01", "10000-02-29", ".5");
            FormConstraintValidator.validateDate("x", "2000-02-29", "2000-02-29", "1e-3");
            FormConstraintValidator.validateDate("x", "", "", "any");
        }).doesNotThrowAnyException();
    }

    private DiagnosticCollector<JavaFileObject> compile(Path source, boolean processor) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        Path coreClasses = Path.of(TextInput.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var options = new java.util.ArrayList<>(List.of("-classpath", coreClasses.toString(),
                    "-d", directory.toString(), "-s", directory.toString()));
            if (!processor) options.add("-proc:none");
            var task = compiler.getTask(null, manager, diagnostics, options, null,
                    manager.getJavaFileObjects(source.toFile()));
            if (processor) task.setProcessors(List.of(new FormMetadataProcessor()));
            assertThat(task.call()).isEqualTo(!processor);
        }
        return diagnostics;
    }

    static class InvalidGeneratedForm {}

    static class ValidGeneratedForm {
        @TextInput(minLength = 0, maxLength = 20)
        private String name;
        @NumberInput(min = -1, max = 10, step = 0.5)
        private double amount;
        @DateInput(min = "2000-02-29", max = "2024-02-29", step = "any")
        private String date;
    }
}
