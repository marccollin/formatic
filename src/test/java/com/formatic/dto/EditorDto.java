package com.formatic.dto;

import com.formatic.form.FormField;
import com.formatic.form.FormFieldType;

import java.time.LocalDate;
import java.util.List;

public class EditorDto {

    @FormField(type = FormFieldType.HIDDEN, order = 1)
    private Long idEditeur;

    @FormField(type = FormFieldType.TEXT, required = true, order=2, label="name")
    private String name;

    @FormField(type = FormFieldType.TEXT, order = 3)
    private String address;

    @FormField(type = FormFieldType.TEXT, order = 4)
    private String address2;

    @FormField(type = FormFieldType.SELECT, optionsProvider = "getCities", order = 5 )
    private String city;

    @FormField(type = FormFieldType.SELECT, optionsProvider = "getProvinces", order = 6 )
    private String provinceState;

    @FormField(type = FormFieldType.TEXT, order = 7)
    private String country;

    @FormField(type = FormFieldType.TEXT, order = 8, pattern = "a9a 9a9")
    private String codePostal;

    @FormField(type = FormFieldType.PHONE, order = 9, pattern = "999 999-9999",  htmlAttributes = {"data-pattern:canada", "data-region:montreal"})
    private String phone;

    @FormField(type = FormFieldType.CHECKBOX, order = 10)
    private boolean confidential;

    @FormField(type = FormFieldType.EMAIL, order = 11)
    private String email;

    @FormField(type = FormFieldType.URL, order = 12)
    private String webSite;

    @FormField(type = FormFieldType.DATE, order = 13)
    private LocalDate birthDate;

    @FormField(type = FormFieldType.TEXTAREA, rows = 5, cols = 51, order = 14, required = true)
    private String comment;

    @FormField(type = FormFieldType.CHECKBOX, options = {"sportCheck:Sport", "musiqueCheck:Musique", "lectureCheck:Lecture"}, order = 15 )
    private List<String> interest;

    @FormField(type = FormFieldType.RADIO, options = {"visa:Visa", "paypal:Paypal"})
    private String paymentMethod;

    public Long getIdEditeur() {
        return idEditeur;
    }

    public void setIdEditeur(Long idEditeur) {
        this.idEditeur = idEditeur;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<String> getInterest() {
        return interest;
    }

    public void setInterest(List<String> interest) {
        this.interest = interest;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}