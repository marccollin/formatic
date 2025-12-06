package com.formatic.example.dto;

import com.formatic.core.annotation.*;

import java.util.List;

public class UserClub {

    @TextInput(minLength = 2, maxLength = 20, label = "name", required = true)
    private String name;

    @SelectInput(optionsProvider = "getCities")
    private String city;

    @CheckboxInput(options = {"sport:Sport", "musique:Musique", "lecture:Lecture"})
    private List<String> interests;

    @TextareaInput(rows = 5, cols = 50, required = true)
    private String comment;

    @RadioInput(options = {"visa:Visa", "paypal:Paypal"})
    private String paymentMethod;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
