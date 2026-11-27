package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class AddressRequest {
    @Size(max=50)
    private String label;

    @NotBlank @Size(max=300)
    private String addressLine;

    @NotBlank @Size(max=100)
    private String city;

    @NotBlank @Size(max=100)
    private String state;

    @NotBlank @Pattern(regexp="^[1-9][0-9]{5}$", message="Invalid pincode")
    private String pincode;

    private Double latitude;
    private Double longitude;
    private boolean defaultAddress;
}
