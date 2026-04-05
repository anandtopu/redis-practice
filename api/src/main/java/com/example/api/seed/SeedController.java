package com.example.api.seed;

import com.example.api.product.Product;
import com.example.api.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeedController {
  private final ProductRepository productRepository;

  public SeedController(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @PostMapping("/seed")
  @Transactional
  public ResponseEntity<?> seed() {
    List<Product> products = List.of(
        new Product(1L, "Keyboard", new BigDecimal("49.99")),
        new Product(2L, "Mouse", new BigDecimal("19.99")),
        new Product(3L, "Monitor", new BigDecimal("199.99"))
    );

    productRepository.saveAll(products);
    return ResponseEntity.ok("seeded");
  }
}
