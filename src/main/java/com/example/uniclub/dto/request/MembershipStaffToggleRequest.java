package com.example.uniclub.dto.request;

import jakarta.validation.constraints.NotNull;

public record MembershipStaffToggleRequest(
        @NotNull Boolean staff
) {}
