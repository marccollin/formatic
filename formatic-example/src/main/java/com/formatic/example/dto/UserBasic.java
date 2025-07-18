package com.formatic.example.dto;

import com.formatic.core.annotation.TextInput;

public class UserBasic {

    @TextInput(minLength = 2, maxLength = 20, label = "name")
    private String name;

    public UserBasic() {
    }

    public UserBasic(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
