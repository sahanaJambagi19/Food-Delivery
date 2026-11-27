package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class UpdateProfileRequest {
    @NotBlank @Size(min=2, max=150)
    private String name;

    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$", message="Invalid Indian mobile number")
    private String phone;
}
