package com.formatic.example.dto;

import com.formatic.core.annotation.*;

import java.time.LocalDate;

public class User {
    @HiddenInput
    private Long idUser;

    @TextInput(minLength = 2, maxLength = 20, outerCssClass = "col-md-6")
    private String firstName;

    @TextInput(minLength = 2, maxLength = 20, outerCssClass = "col-md-6")
    private String lastName;

    @TextInput(outerCssClass = "col-md-12")
    private String address;

    @SelectInput(optionsProvider = "getCities", required = true, outerCssClass = "col-md-4")
    private String city;

    @SelectInput(optionsProvider = "getProvinces", required = true, outerCssClass = "col-md-4")
    private String provinceState;

    @TextInput(defaultValue = "Canada", disabled = true, outerCssClass = "col-md-4")
    private String country;

    @TextInput(pattern = "[A-Za-z][0-9][A-Za-z] [0-9][A-Za-z][0-9]", maxLength = 7, placeholder = "A1A 1A1", title = "Enter a valid Canadian postal code (e.g., A1A 1A1)")
    private String codePostal;

    @PhoneInput(pattern = "[0-9]{3}-[0-9]{3}-[0-9]{4}", placeholder = "514-272-2323", outerCssClass = "col-md-4")
    private String phone;

    @CheckboxInput(outerCssClass = "col-md-8")
    private boolean confidential;

    @EmailInput( outerCssClass = "col-md-6")
    private String email;

    @UrlInput( outerCssClass = "col-md-6")
    private String webSite;

    @DateInput( outerCssClass = "col-md-4")
    private LocalDate birthDate;

    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(Long idUser) {
        this.idUser = idUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvinceState() {
        return provinceState;
    }

    public void setProvinceState(String provinceState) {
        this.provinceState = provinceState;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebSite() {
        return webSite;
    }

    public void setWebSite(String webSite) {
        this.webSite = webSite;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
