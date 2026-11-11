package com.formatic.spring.thymeleaf;

import com.formatic.core.annotation.FileInput;
import com.formatic.core.annotation.handler.FileInputHandler;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = FileUploadBindingTest.TestApplication.class)
@AutoConfigureMockMvc
class FileUploadBindingTest {
    @Autowired
    private MockMvc mockMvc;

    static Stream<String> templates() {
        return Stream.of("bootstrap/forms/vertical/form-base", "bootstrap/forms/horizontal/form-base",
                "tailwind/forms/vertical/form-base", "bootstrap/forms/form-base-hybrid");
    }

    @ParameterizedTest
    @MethodSource("templates")
    void rendersFileFieldsAndBindsMultipartUploads(String template) throws Exception {
        String html = render(template, true);
        assertThat(html).contains("enctype=\"multipart/form-data\"");
        assertThat(input(html, "file")).contains("type=\"file\"", "accept=\"text/plain\"",
                "required=\"required\"", "id=\"file\"").doesNotContain("value=", "multiple=");
        assertThat(input(html, "files")).contains("type=\"file\"", "multiple=\"multiple\"")
                .doesNotContain("value=", "accept=");
        assertThat(input(html, "disabled")).contains("disabled=\"disabled\"");
        assertThat(html).contains("for=\"file\"");

        mockMvc.perform(multipart("/upload-test")
                        .file(new MockMultipartFile("file", "single.txt", "text/plain",
                                "Single file".getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("files", "first.txt", "text/plain", new byte[]{1}))
                        .file(new MockMultipartFile("files", "second.txt", "text/plain", new byte[]{2})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("single.txt"))
                .andExpect(jsonPath("$.content").value("Single file"))
                .andExpect(jsonPath("$.files[0]").value("first.txt"))
                .andExpect(jsonPath("$.files[1]").value("second.txt"));
    }

    @ParameterizedTest
    @MethodSource("templates")
    void formsWithoutUploadsKeepDefaultEncoding(String template) throws Exception {
        assertThat(render(template, false)).doesNotContain("enctype=", "type=\"file\"");
    }

    private String render(String template, boolean uploads) throws Exception {
        return mockMvc.perform(get("/upload-test").param("template", template)
                        .param("uploads", Boolean.toString(uploads)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String input(String html, String name) {
        var matcher = Pattern.compile("<input\\b[^>]*\\bname=\"" + Pattern.quote(name) + "\"[^>]*>")
                .matcher(html);
        assertThat(matcher.find()).as("Input %s in rendered form", name).isTrue();
        return matcher.group();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    static class TestApplication {
        @Bean
        UploadController uploadController() {
            return new UploadController();
        }
    }

    @Controller
    static class UploadController {
        @GetMapping("/upload-test")
        String form(@RequestParam String template, @RequestParam boolean uploads, Model model)
                throws NoSuchFieldException {
            FileInputHandler handler = new FileInputHandler();
            List<FormFieldMetadata> fields = uploads ? List.of(
                    handler.handle(UploadForm.class.getDeclaredField("file")),
                    handler.handle(UploadForm.class.getDeclaredField("files")),
                    handler.handle(UploadForm.class.getDeclaredField("disabled"))) : List.of();
            model.addAttribute("fields", fields);
            model.addAttribute("formData", new UploadForm());
            model.addAttribute("formName", "Uploads");
            model.addAttribute("editorId", 1);
            return template;
        }

        @PostMapping("/upload-test")
        @ResponseBody
        Map<String, Object> submit(@ModelAttribute UploadForm form) throws Exception {
            return Map.of("name", form.getFile().getOriginalFilename(),
                    "content", new String(form.getFile().getBytes(), StandardCharsets.UTF_8),
                    "files", form.getFiles().stream().map(MultipartFile::getOriginalFilename).toList());
        }
    }

    public static class UploadForm {
        @FileInput(accept = "text/plain", required = true)
        private MultipartFile file;

        @FileInput(multiple = true)
        private List<MultipartFile> files;

        @FileInput(disabled = true)
        private MultipartFile disabled;

        public MultipartFile getFile() { return file; }
        public void setFile(MultipartFile file) { this.file = file; }
        public List<MultipartFile> getFiles() { return files; }
        public void setFiles(List<MultipartFile> files) { this.files = files; }
        public MultipartFile getDisabled() { return disabled; }
        public void setDisabled(MultipartFile disabled) { this.disabled = disabled; }
    }
}
