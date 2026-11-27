package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class RegisterRequest {
    @NotBlank @Size(min=2, max=150)
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$", message="Invalid Indian mobile number")
    private String phone;

    @NotBlank @Size(min=8, message="Password must be at least 8 characters")
    @Pattern(regexp="^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).+$",
             message="Password must contain uppercase, digit, and special character")
    private String password;

    /** CUSTOMER | RESTAURANT_OWNER | DELIVERY_PARTNER */
    @NotBlank
    private String role;
}
