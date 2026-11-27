package com.formatic.example;

import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.TextInput;
import com.formatic.core.annotation.DateInput;
import com.formatic.core.annotation.SelectInput;
import com.formatic.core.annotation.EmailInput;
import com.formatic.core.annotation.UrlInput;
import com.formatic.core.annotation.PhoneInput;
import com.formatic.core.annotation.ColorInput;
import com.formatic.core.annotation.TimeInput;
import com.formatic.validation.jakarta.FormaticValidated;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FormaticValidationIntegrationTest {
    @Test
    void htmlAndJsonBindingApplyTheSamePatternConstraint() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new Controller()).setValidator(validator).build();
            mvc.perform(post("/html").param("address.street", "Main").param("postalCode", "A1A 1A11"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("postalCode"));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"postalCode":"A1A 1A11"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("postalCode"));
            mvc.perform(post("/html").param("address.street", "Main").param("postalCode", "A1A 1A1"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"postalCode":"A1A 1A1"}
                    """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void htmlAndJsonBindingApplyExplicitTemporalSteps() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new Controller()).setValidator(validator).build();
            mvc.perform(post("/html").param("address.street", "Main").param("time", "09:37"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("time"));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"time":"09:37"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("time"));
            mvc.perform(post("/html").param("address.street", "Main").param("time", "09:45"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"time":"09:45"}
                    """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void htmlAndJsonBindingApplyTheSameStringControlRules() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new Controller()).setValidator(validator).build();
            mvc.perform(post("/html").param("address.street", "Main")
                            .param("email", "invalid").param("url", "/relative")
                            .param("phone", "1").param("color", "red"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder("email", "url", "phone", "color")));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"email":"invalid","url":"/relative","phone":"1","color":"red"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$", org.hamcrest.Matchers.containsInAnyOrder("email", "url", "phone", "color")));
            mvc.perform(post("/html").param("address.street", "Main")
                            .param("email", "user@example.com").param("url", "https://example.com")
                            .param("phone", "call reception").param("color", "#aB12fF"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"email":"user@example.com","url":"https://example.com",
                     "phone":"call reception","color":"#aB12fF"}
                    """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void htmlAndJsonBindingRejectForgedStaticChoices() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new Controller()).setValidator(validator).build();
            mvc.perform(post("/html").param("address.street", "Main").param("category", "forged"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("category"));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"category":"forged"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("category"));
            mvc.perform(post("/html").param("address.street", "Main").param("category", "personal"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"category":"personal"}
                    """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void htmlBindingAndJsonBindingReceiveTheSameNestedFieldErrors() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new Controller()).setValidator(validator).build();
            mvc.perform(post("/html").param("address.street", "x"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("address.street"));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"x"}}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("address.street"));
            mvc.perform(post("/html").param("address.street", "Main"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"}}
                    """))
                    .andExpect(status().isOk());
            mvc.perform(post("/html").param("address.street", "Main").param("date", "2023-12-31"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("date"));
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"date":"2023-12-31"}
                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0]").value("date"));
            mvc.perform(post("/html").param("address.street", "Main").param("date", "2024-02-29"))
                    .andExpect(status().isOk());
            mvc.perform(post("/json").contentType("application/json").content("""
                    {"address":{"street":"Main"},"date":"2024-02-29"}
                    """))
                    .andExpect(status().isOk());
        }
    }

    @RestController
    static class Controller {
        @PostMapping("/html")
        ResponseEntity<?> html(@Valid @ModelAttribute Submission form, BindingResult errors) {
            return response(errors);
        }

        @PostMapping("/json")
        ResponseEntity<?> json(@Valid @RequestBody Submission form, BindingResult errors) {
            return response(errors);
        }

        private ResponseEntity<?> response(BindingResult errors) {
            return errors.hasErrors()
                    ? ResponseEntity.badRequest().body(errors.getFieldErrors().stream().map(error -> error.getField()).toList())
                    : ResponseEntity.ok().build();
        }
    }

    @FormaticValidated
    public static class Submission {
        @TextInput(pattern = "[A-Za-z][0-9][A-Za-z] [0-9][A-Za-z][0-9]")
        private String postalCode;
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        @TimeInput(min = "09:00", step = "900") private String time;
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        @EmailInput private String email;
        @UrlInput private String url;
        @PhoneInput(minLength = 2) private String phone;
        @ColorInput private String color;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        @SelectInput(options = {"personal:Personal", "business:Business"})
        private String category;
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        @NestedForm private Address address = new Address();
        @DateInput(min = "2024-01-01", max = "2024-12-31")
        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        private java.time.LocalDate date;
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }
        public java.time.LocalDate getDate() { return date; }
        public void setDate(java.time.LocalDate date) { this.date = date; }
    }

    public static class Address {
        @TextInput(required = true, minLength = 2) private String street;
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }
}
