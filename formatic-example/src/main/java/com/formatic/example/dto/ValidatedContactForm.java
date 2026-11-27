package com.formatic.example.dto;

import com.formatic.core.annotation.EmailInput;
import com.formatic.core.annotation.PhoneInput;
import com.formatic.core.annotation.TextInput;
import com.formatic.validation.jakarta.FormaticValidated;

@FormaticValidated
public class ValidatedContactForm {
    @TextInput(label = "Name", required = true, minLength = 2, maxLength = 80)
    private String name;

    @EmailInput(label = "Email", required = true)
    private String email;

    @PhoneInput(label = "Phone (optional)", maxLength = 40)
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
