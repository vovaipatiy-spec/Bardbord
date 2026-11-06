package com.ecommerce.server.dto.product;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductPatchRequest(
        @Size(max = 150) String name,
        @Size(max = 5000) String description,
        @Positive BigDecimal price,
        @PositiveOrZero Integer stock,
        Long categoryId
) {}
