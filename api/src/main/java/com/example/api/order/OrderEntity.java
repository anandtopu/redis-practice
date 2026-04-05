package com.example.api.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String userId;

  private Long productId;

  private Instant createdAt;

  protected OrderEntity() {
  }

  public OrderEntity(String userId, Long productId, Instant createdAt) {
    this.userId = userId;
    this.productId = productId;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public Long getProductId() {
    return productId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
