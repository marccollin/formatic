package com.formatic.example;

import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.annotation.handler.TextareaInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.core.annotation.handler.DateInputHandler;
import com.formatic.core.annotation.handler.CheckBoxHandler;
import com.formatic.core.form.ConstraintValidationException;
import com.formatic.example.controller.FormaticController;
import com.formatic.example.controller.FormaticRestController;
import com.formatic.example.dto.Editor;
import com.formatic.example.service.EditorService;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = FormaticControllerIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class FormaticControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReflectionFormFieldMetadataBuilder reflectionBuilder;

    @MockitoBean
    private BytecodeFormFieldMetadataBuilder bytecodeBuilder;

    @MockitoBean
    private CompileTimeFormFieldMetadataBuilder compileTimeBuilder;

    @BeforeEach
    void metadata() throws Exception {
        when(reflectionBuilder.buildMetadata(Editor.class)).thenReturn(List.of(
                new TextareaInputHandler().handle(Editor.class.getDeclaredField("comment")),
                new TextInputHandler().handle(Editor.class.getDeclaredField("codePostal")),
                new DateInputHandler().handle(Editor.class.getDeclaredField("birthDate")),
                new CheckBoxHandler(null).handle(Editor.class.getDeclaredField("confidential"))
        ));
    }

    @Test
    void postRedisplaysSubmittedValuesWithConfirmation() throws Exception {
        mockMvc.perform(post("/formfeeded")
                        .param("codePostal", "H2X 1Y4")
                        .param("comment", "Updated comment")
                        .param("birthDate", "2000-01-02")
                        .param("_confidential", "on")
                        .param("confidential", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("bootstrap/forms/vertical/form-base"))
                .andExpect(content().string(containsString("Form submitted successfully. This demo does not save changes.")))
                .andExpect(content().string(containsString("value=\"H2X 1Y4\"")))
                .andExpect(content().string(containsString(">Updated comment</textarea>")))
                .andExpect(content().string(containsString("value=\"2000-01-02\"")))
                .andExpect(content().string(containsString("checked=\"checked\"")));
    }

    @Test
    void postRedisplaysBindingErrorsWithoutConfirmation() throws Exception {
        mockMvc.perform(post("/formfeeded")
                        .param("codePostal", "H2X 1Y4")
                        .param("birthDate", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(view().name("bootstrap/forms/vertical/form-base"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeHasFieldErrors("formData", "birthDate"))
                .andExpect(content().string(containsString("id=\"birthDate-errors\"")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("value=\"H2X 1Y4\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Form submitted successfully"))));
    }

    @Test
    void prefilledFormUsesExistingVerticalTemplate() throws Exception {
        mockMvc.perform(get("/formfeeded"))
                .andExpect(status().isOk())
                .andExpect(view().name("bootstrap/forms/vertical/form-base"))
                .andExpect(content().string(containsString("name=\"comment\"")))
                .andExpect(content().string(containsString("value=\"J4J1T5\"")));
    }

    @Test
    void bulmaRendersVerticalFormWithBulmaStyles() throws Exception {
        mockMvc.perform(get("/form")
                        .param("class", Editor.class.getName())
                        .param("cssLibrary", "BULMA")
                        .param("form", "VERTICAL")
                        .param("mode", "REFLECTION"))
                .andExpect(status().isOk())
                .andExpect(view().name("bulma/forms/vertical/form-base"))
                .andExpect(content().string(containsString("class=\"input\"")))
                .andExpect(content().string(containsString("class=\"textarea\"")))
                .andExpect(content().string(containsString("class=\"button is-primary\"")))
                .andExpect(content().string(containsString("name=\"comment\"")));
    }

    @Test
    void bulmaHorizontalFormUsesResponsiveTwoColumnLayout() throws Exception {
        mockMvc.perform(get("/form")
                        .param("class", Editor.class.getName())
                        .param("cssLibrary", "BULMA")
                        .param("form", "HORIZONTAL")
                        .param("mode", "REFLECTION"))
                .andExpect(status().isOk())
                .andExpect(view().name("bulma/forms/horizontal/form-base"))
                .andExpect(content().string(containsString("column is-half-desktop")))
                .andExpect(content().string(containsString("column is-full")))
                .andExpect(content().string(containsString("name=\"comment\"")));
    }

    @Test
    void hybridFormResolvesHorizontalFieldFragments() throws Exception {
        mockMvc.perform(get("/formhybrid/1")
                        .param("class", "com.formatic.example.dto.Editor"))
                .andExpect(status().isOk())
                .andExpect(view().name("bootstrap/forms/form-base-hybrid"))
                .andExpect(content().string(containsString("col-sm-10")))
                .andExpect(content().string(containsString("name=\"comment\"")))
                .andExpect(content().string(containsString("const editorId = 1;")));
    }

    @Test
    void hybridDataEndpointSuppliesEditorText() throws Exception {
        mockMvc.perform(get("/api/editors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEditeur").value(1))
                .andExpect(jsonPath("$.comment").value("Specializes in technical books and journals."));
    }

    static Stream<String> formModes() {
        return Stream.of("BYTECODE", "REFLECTION", "COMPILE_TIME_GENERATION");
    }

    @ParameterizedTest
    @MethodSource("formModes")
    void invalidConfigurationReturnsServerErrorForEveryBuilderMode(String mode) throws Exception {
        ConstraintValidationException failure = new ConstraintValidationException("Editor.score: step invalid; received 0");
        switch (mode) {
            case "BYTECODE" -> doThrow(failure).when(bytecodeBuilder).buildMetadata(Editor.class);
            case "REFLECTION" -> doThrow(failure).when(reflectionBuilder).buildMetadata(Editor.class);
            case "COMPILE_TIME_GENERATION" -> doThrow(failure).when(compileTimeBuilder).buildMetadata(Editor.class);
            default -> throw new IllegalArgumentException(mode);
        }

        mockMvc.perform(get("/form").param("class", Editor.class.getName())
                        .param("cssLibrary", "BOOTSTRAP").param("mode", mode))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unable to generate the form because its configuration is invalid."));
    }

    @Test
    void invalidConfigurationReturnsServerErrorForPrefilledAndHybridForms() throws Exception {
        doThrow(new ConstraintValidationException("Editor.score: step invalid; received 0"))
                .when(reflectionBuilder).buildMetadata(Editor.class);

        mockMvc.perform(get("/formfeeded"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unable to generate the form because its configuration is invalid."));
        mockMvc.perform(get("/formhybrid/1").param("class", Editor.class.getName()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unable to generate the form because its configuration is invalid."));
        mockMvc.perform(post("/formfeeded"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Unable to generate the form because its configuration is invalid."));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @Import({FormaticController.class, FormaticRestController.class, EditorService.class})
    static class TestApplication {
    }
}
