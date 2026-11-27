package com.fooddelivery.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class AddressResponse {
    private Long id;
    private String label;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private boolean defaultAddress;
}
