package com.example.api.product;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ProductController {

  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/products/{id}")
  public ResponseEntity<?> getProduct(@PathVariable("id") Long id) {
    ProductDto dto = productService.getProduct(id);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/products/{id}/price")
  public ResponseEntity<?> updatePrice(@PathVariable("id") Long id, @Valid @RequestBody UpdatePriceRequest request) {
    return ResponseEntity.ok(productService.updatePrice(id, request));
  }
}
