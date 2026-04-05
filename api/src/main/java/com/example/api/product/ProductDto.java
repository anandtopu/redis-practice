package com.example.api.product;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, BigDecimal price, String source) {
}
