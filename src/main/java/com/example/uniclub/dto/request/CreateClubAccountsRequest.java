package com.example.uniclub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClubAccountsRequest {

    @NotNull(message = "Club ID is required")
    private Long clubId;


    @NotBlank(message = "Leader full name is required")
    private String leaderFullName;

    @Email(message = "Leader email must be valid")
    @NotBlank(message = "Leader email is required")
    private String leaderEmail;


    @NotBlank(message = "Vice leader full name is required")
    private String viceFullName;

    @Email(message = "Vice leader email must be valid")
    @NotBlank(message = "Vice leader email is required")
    private String viceEmail;

    @NotBlank(message = "Default password is required")
    private String defaultPassword;
}
