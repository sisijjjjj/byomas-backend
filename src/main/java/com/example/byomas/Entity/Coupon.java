package com.example.byomas.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private Double discountValue;

    @Column(nullable = false)
    private String discountType; // PERCENTAGE, FIXED, FREE_SHIPPING

    private Double discountExtra;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private LocalDateTime startDate;

    private String description;

    private Integer maxUses;

    private Double minOrderAmount;

    @Column(nullable = false)
    private Boolean isActive = true;

    private Integer usedCount = 0;

    private Boolean freeShipping = false;

    // CORRECTION : Ajoutez fetch = FetchType.EAGER et @JsonIgnoreProperties
    @ElementCollection(fetch = FetchType.EAGER) // EAGER pour éviter LazyInitializationException
    @CollectionTable(
            name = "coupon_applicable_products",
            joinColumns = @JoinColumn(name = "coupon_id")
    )
    @Column(name = "product_id")
    private List<Long> applicableProducts = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructeurs
    public Coupon() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.usedCount = 0;
        this.freeShipping = false;
        this.applicableProducts = new ArrayList<>();
    }

    public Coupon(String code, Double discountValue, String discountType, LocalDateTime expiryDate) {
        this();
        this.code = code;
        this.discountValue = discountValue;
        this.discountType = discountType;
        this.expiryDate = expiryDate;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) {
        this.code = code;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) {
        this.discountType = discountType;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getDiscountExtra() { return discountExtra; }
    public void setDiscountExtra(Double discountExtra) {
        this.discountExtra = discountExtra;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getMaxUses() { return maxUses; }
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
        this.updatedAt = LocalDateTime.now();
    }

    public Boolean getFreeShipping() { return freeShipping; }
    public void setFreeShipping(Boolean freeShipping) {
        this.freeShipping = freeShipping;
        this.updatedAt = LocalDateTime.now();
    }

    // CORRECTION : Initialisez toujours la liste
    public List<Long> getApplicableProducts() {
        if (applicableProducts == null) {
            applicableProducts = new ArrayList<>();
        }
        return applicableProducts;
    }

    public void setApplicableProducts(List<Long> applicableProducts) {
        if (applicableProducts == null) {
            this.applicableProducts = new ArrayList<>();
        } else {
            this.applicableProducts = applicableProducts;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Méthodes utilitaires
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean hasReachedMaxUses() {
        return maxUses != null && usedCount >= maxUses;
    }

    public boolean isValid() {
        return isActive && !isExpired() && !hasReachedMaxUses();
    }

    public void incrementUsedCount() {
        this.usedCount = (this.usedCount == null) ? 1 : this.usedCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    // Ajouter un produit à la liste
    public void addApplicableProduct(Long productId) {
        if (applicableProducts == null) {
            applicableProducts = new ArrayList<>();
        }
        if (!applicableProducts.contains(productId)) {
            applicableProducts.add(productId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Retirer un produit de la liste
    public void removeApplicableProduct(Long productId) {
        if (applicableProducts != null) {
            applicableProducts.remove(productId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Vérifier si un produit est applicable
    public boolean isProductApplicable(Long productId) {
        if (applicableProducts == null || applicableProducts.isEmpty()) {
            return true; // Si pas de produits spécifiques, le coupon est universel
        }
        return applicableProducts.contains(productId);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (usedCount == null) {
            usedCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (freeShipping == null) {
            freeShipping = false;
        }
        if (applicableProducts == null) {
            applicableProducts = new ArrayList<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", discountValue=" + discountValue +
                ", discountType='" + discountType + '\'' +
                ", discountExtra=" + discountExtra +
                ", expiryDate=" + expiryDate +
                ", startDate=" + startDate +
                ", isActive=" + isActive +
                ", usedCount=" + usedCount +
                ", maxUses=" + maxUses +
                ", minOrderAmount=" + minOrderAmount +
                ", freeShipping=" + freeShipping +
                ", applicableProducts=" + (applicableProducts != null ? applicableProducts.size() : 0) + " produits" +
                '}';
    }


    public void setLastUsedAt(LocalDateTime now) {
    }

    public Object getRemainingUses() {
        return null;
    }
    // Dans votre fichier Coupon.java
    public void incrementerCompteurUtilisation() {
        if (this.usedCount == null) {
            this.usedCount = 0;
        }
        this.usedCount++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean peutIncrementer() {
        if (this.maxUses == null) {
            return true; // Utilisations illimitées
        }
        int utilisationsActuelles = this.usedCount != null ? this.usedCount : 0;
        return utilisationsActuelles < this.maxUses;
    }

    public boolean isActive() {
        return false;
    }
}