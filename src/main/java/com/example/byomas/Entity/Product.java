package com.example.byomas.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer stockQuantity;

    private String category;

    private String imageUrl;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private String reference;

    private String badge;

    private String specialOffer;

    private Integer salesCount = 0;

    private Double rating = 0.0;

    private Integer ratingCount = 0;

    private Integer minStockThreshold = 5;

    private Double weight;

    private String dimensions;

    private String brand;

    // Constructeurs
    public Product() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.stockQuantity = 0;
        this.price = 0.0;
        this.salesCount = 0;
        this.rating = 0.0;
        this.ratingCount = 0;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getSpecialOffer() { return specialOffer; }
    public void setSpecialOffer(String specialOffer) { this.specialOffer = specialOffer; }

    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }

    public Integer getMinStockThreshold() { return minStockThreshold; }
    public void setMinStockThreshold(Integer minStockThreshold) { this.minStockThreshold = minStockThreshold; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    // Méthodes utilitaires
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity > 0 && stockQuantity <= minStockThreshold;
    }

    public boolean isOutOfStock() {
        return stockQuantity == 0;
    }

    public void incrementSalesCount() {
        this.salesCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public void addRating(Double newRating) {
        if (this.ratingCount == 0) {
            this.rating = newRating;
        } else {
            this.rating = ((this.rating * this.ratingCount) + newRating) / (this.ratingCount + 1);
        }
        this.ratingCount++;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (price == null) {
            price = 0.0;
        }
        if (salesCount == null) {
            salesCount = 0;
        }
        if (rating == null) {
            rating = 0.0;
        }
        if (ratingCount == null) {
            ratingCount = 0;
        }
        if (minStockThreshold == null) {
            minStockThreshold = 5;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stockQuantity=" + stockQuantity +
                ", category='" + category + '\'' +
                ", isActive=" + isActive +
                '}';
    }

    public void getDuration() {
    }


    public boolean getComposition() {
        return false;
    }
}