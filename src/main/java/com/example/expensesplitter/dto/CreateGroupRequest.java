package com.example.expensesplitter.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupRequest {

    @NotBlank(message = "name is required")
    private String name;

    /** Friend names to add. Any name matching a registered username is linked
     *  to that account automatically; any other name is added as a guest
     *  member with no account required. */
    private List<String> members = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
