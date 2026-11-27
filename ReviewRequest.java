package com.fooddelivery.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class ReviewRequest {
    @NotNull private Long orderId;
    @NotNull @Min(1) @Max(5) private Integer rating;
    @Size(max=1000) private String comment;
}
