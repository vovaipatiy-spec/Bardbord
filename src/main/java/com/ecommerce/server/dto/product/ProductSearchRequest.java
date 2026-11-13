package com.ecommerce.server.dto.product;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductSearchRequest(
        @Size(max = 150) String query,
        @Positive Long categoryId,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice,
        @PositiveOrZero Integer minStock,
        @PositiveOrZero Integer maxStock,
        @Size(max = 50) String sortBy,
        SortDirection sortDirection
) {}