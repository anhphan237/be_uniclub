package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubRenameRequest {
    @NotBlank(message = "Club name cannot be empty.")
    private String newName;
}
