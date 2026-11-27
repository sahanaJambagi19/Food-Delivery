package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank @Size(min=8)
    @Pattern(regexp="^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).+$",
             message="Password must contain uppercase, digit, and special character")
    private String newPassword;
}
