package com.formatic.example.dto;

import com.formatic.core.annotation.NestedForm;
import com.formatic.core.annotation.TextInput;

public class CustomerForm {
    @TextInput(label = "Customer name", required = true)
    private String name;

    @NestedForm
    private Address address = new Address();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public static class Address {
        @TextInput(label = "Street", required = true)
        private String street;

        @TextInput(label = "City", required = true)
        private String city;

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }
}
