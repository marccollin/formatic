package com.formatic.example;

import com.formatic.core.builder.BytecodeFormFieldMetadataBuilder;
import com.formatic.core.builder.CompileTimeFormFieldMetadataBuilder;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.annotation.handler.TextareaInputHandler;
import com.formatic.core.annotation.handler.TextInputHandler;
import com.formatic.example.controller.FormaticController;
import com.formatic.example.controller.FormaticRestController;
import com.formatic.example.dto.Editor;
import com.formatic.example.service.EditorService;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                new TextInputHandler().handle(Editor.class.getDeclaredField("codePostal"))
        ));
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

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @Import({FormaticController.class, FormaticRestController.class, EditorService.class})
    static class TestApplication {
    }
}
