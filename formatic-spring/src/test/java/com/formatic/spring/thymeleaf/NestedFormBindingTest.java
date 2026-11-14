package com.formatic.spring.thymeleaf;

import com.formatic.core.annotation.*;
import com.formatic.core.annotation.handler.*;
import com.formatic.core.builder.ReflectionFormFieldMetadataBuilder;
import com.formatic.core.form.FieldIdStyle;
import com.formatic.core.form.FormFieldMetadata;
import com.formatic.core.service.OptionsProviderService;
import com.formatic.spring.config.FormaticProperties;
import com.formatic.spring.config.FormaticSpringAutoConfiguration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = NestedFormBindingTest.TestApplication.class,
        properties = "formatic.nested.id-style=SNAKE_CASE")
@AutoConfigureMockMvc
class NestedFormBindingTest {
    @Autowired MockMvc mvc;
    @Autowired FormaticProperties properties;

    static Stream<Object[]> cases() {
        return Stream.of("bootstrap/forms/vertical", "bootstrap/forms/horizontal",
                        "tailwind/forms/vertical", "bulma/forms/vertical", "bulma/forms/horizontal")
                .flatMap(layout -> Stream.of(FieldIdStyle.values()).map(style -> new Object[]{layout, style}));
    }

    @ParameterizedTest
    @MethodSource("cases")
    void rendersAndBindsNestedControlsAndCascadesValidation(String layout, FieldIdStyle style) throws Exception {
        assertThat(properties.getNested().getIdStyle()).isEqualTo(FieldIdStyle.SNAKE_CASE);
        String streetId = style.format("address.street");
        String html = mvc.perform(get("/nested-test").param("layout", layout).param("style", style.name()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"address.street\"", "id=\"" + streetId + "\"",
                "for=\"" + streetId + "\"", "value=\"Main street\"",
                "name=\"address.city\"", "id=\"" + style.format("address.city") + "\"",
                "id=\"" + style.format("address.newsletter") + "\"",
                "id=\"" + style.format("address.payment") + "_visa\"",
                "id=\"" + style.format("address.interests") + "_sport\"",
                "name=\"_address.newsletter\"", "name=\"_address.interests\"",
                "multipart/form-data", "name=\"address.upload\"");
        if (!layout.startsWith("bulma")) {
            assertThat(html).contains("for=\"" + style.format("address.payment") + "_visa\"");
        }
        var result = mvc.perform(post("/nested-test").param("layout", layout).param("style", style.name())
                        .param("address.street", "").param("address.city", "Paris")
                        .param("address.newsletter", "true").param("address.payment", "visa")
                        .param("address.interests", "sport"))
                .andExpect(status().isOk()).andReturn();
        Customer data = (Customer) result.getModelAndView().getModel().get("formData");
        assertThat(data.getAddress().getCity()).isEqualTo("Paris");
        assertThat(data.getAddress().isNewsletter()).isTrue();
        assertThat(data.getAddress().getInterests()).containsExactly("sport");
        assertThat(result.getResponse().getContentAsString()).contains("Street is required",
                "id=\"" + streetId + "-errors\"", "aria-describedby=\"" + streetId + "-errors\"");
    }

    @ParameterizedTest
    @MethodSource("cases")
    void rendersNullNestedDtoWithoutChangingItsInitializationContract(String layout, FieldIdStyle style) throws Exception {
        mvc.perform(get("/nested-test").param("layout", layout).param("style", style.name()).param("empty", "true"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"address.street\"")));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = FormaticSpringAutoConfiguration.class)
    @EnableConfigurationProperties(FormaticProperties.class)
    static class TestApplication {
        @Bean NestedController nestedController() { return new NestedController(); }
    }

    @Controller
    static class NestedController {
        @GetMapping("/nested-test")
        String form(@RequestParam String layout, @RequestParam FieldIdStyle style,
                    @RequestParam(defaultValue = "false") boolean empty, Model model) {
            Customer customer = new Customer();
            if (!empty) {
                customer.setAddress(new Address());
                customer.getAddress().setStreet("Main street");
            }
            model.addAttribute("formData", customer);
            return render(layout, style, model);
        }

        @PostMapping("/nested-test")
        String submit(@Valid @ModelAttribute("formData") Customer customer, BindingResult errors,
                      @RequestParam String layout, @RequestParam FieldIdStyle style, Model model) {
            return render(layout, style, model);
        }

        private String render(String layout, FieldIdStyle style, Model model) {
            OptionsProviderService options = org.mockito.Mockito.mock(OptionsProviderService.class);
            List<FormFieldMetadata> fields = new ReflectionFormFieldMetadataBuilder(List.of(
                    new TextInputHandler(), new SelectInputHandler(options), new CheckBoxHandler(options),
                    new RadioInputHandler(options), new FileInputHandler()), style).buildMetadata(Customer.class);
            model.addAttribute("fields", fields);
            model.addAttribute("formName", "Customer");
            return layout + "/form-base";
        }
    }

    public static class Customer {
        @NestedForm @Valid private Address address;
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
    }

    public static class Address {
        @TextInput @NotBlank(message = "Street is required") private String street;
        @SelectInput(options = {"Paris:Paris", "London:London"}) private String city;
        @CheckboxInput private boolean newsletter;
        @RadioInput(options = {"visa:Visa", "cash:Cash"}) private String payment;
        @CheckboxInput(options = {"sport:Sport", "music:Music"}) private List<String> interests;
        @FileInput private org.springframework.web.multipart.MultipartFile upload;
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public boolean isNewsletter() { return newsletter; }
        public void setNewsletter(boolean newsletter) { this.newsletter = newsletter; }
        public String getPayment() { return payment; }
        public void setPayment(String payment) { this.payment = payment; }
        public List<String> getInterests() { return interests; }
        public void setInterests(List<String> interests) { this.interests = interests; }
        public org.springframework.web.multipart.MultipartFile getUpload() { return upload; }
        public void setUpload(org.springframework.web.multipart.MultipartFile upload) { this.upload = upload; }
    }
}
