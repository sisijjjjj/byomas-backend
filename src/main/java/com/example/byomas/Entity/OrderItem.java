package com.example.byomas.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "order_items")
public class OrderItem {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total")
    private Double total;

    // SUPPRIMER cette colonne - elle crée un conflit
    // @Column(name = "order_id", nullable = false)
    // private Long orderId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "discount_applied")
    private Double discountApplied;

    @Column(name = "tax_amount")
    private Double taxAmount;

    @Column(name = "shipping_cost")
    private Double shippingCost;

    @Column(name = "final_total")
    private Double finalTotal;

    @Column(name = "currency")
    private String currency;

    @Column(name = "product_category")
    private String productCategory;

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "product_image")
    private String productImage;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "dimensions")
    private String dimensions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Product product;

    // CORRECTION : Utiliser la relation JPA correctement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false) // Ici on définit la colonne order_id
    private CustomerOrder order;

    // Constructeurs
    public OrderItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.currency = "TND";
        this.discountApplied = 0.0;
        this.taxAmount = 0.0;
        this.shippingCost = 0.0;
        this.quantity = 0;
        this.price = 0.0;
        this.total = 0.0;
        this.finalTotal = 0.0;
    }

    public OrderItem(Long productId, String productName, Double price, Integer quantity) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.price = price != null ? price : 0.0;
        this.quantity = quantity != null ? quantity : 0;
        calculateTotals();
    }

    public OrderItem(Long productId, String productName, Double price, Integer quantity,
                     CustomerOrder order, Long customerId, String customerName) {
        this(productId, productName, price, quantity);
        this.order = order; // Utiliser l'objet Order directement
        this.customerId = customerId;
        this.customerName = customerName;
        this.orderDate = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) {
        this.productId = productId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) {
        this.productName = productName;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getPrice() { return price; }
    public void setPrice(Double price) {
        this.price = price != null ? price : 0.0;
        calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        this.quantity = quantity != null ? quantity : 0;
        calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTotal() { return total; }
    public void setTotal(Double total) {
        this.total = total != null ? total : 0.0;
        this.updatedAt = LocalDateTime.now();
    }

    // SUPPRIMER getOrderId() et setOrderId()
    // public Long getOrderId() { return orderId; }
    // public void setOrderId(Long orderId) { ... }

    // À la place, utiliser ces méthodes
    public Long getOrderId() {
        return this.order != null ? this.order.getId() : null;
    }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(Double discountApplied) {
        this.discountApplied = discountApplied != null ? discountApplied : 0.0;
        calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) {
        this.taxAmount = taxAmount != null ? taxAmount : 0.0;
        calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public Double getShippingCost() { return shippingCost; }
    public void setShippingCost(Double shippingCost) {
        this.shippingCost = shippingCost != null ? shippingCost : 0.0;
        calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public Double getFinalTotal() { return finalTotal; }
    public void setFinalTotal(Double finalTotal) {
        this.finalTotal = finalTotal != null ? finalTotal : 0.0;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) {
        this.currency = currency;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) {
        this.productSku = productSku;
        this.updatedAt = LocalDateTime.now();
    }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) {
        this.productImage = productImage;
        this.updatedAt = LocalDateTime.now();
    }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) {
        this.weight = weight;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
        this.updatedAt = LocalDateTime.now();
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            if (this.productName == null) {
                this.productName = product.getName();
            }
            if (this.productId == null) {
                this.productId = product.getId();
            }
            if (this.price == null) {
                this.price = product.getPrice();
            }
            if (this.productCategory == null) {
                this.productCategory = product.getCategory();
            }
            if (this.productImage == null && product.getImageUrl() != null) {
                this.productImage = product.getImageUrl();
            }
        }
        this.updatedAt = LocalDateTime.now();
    }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) {
        this.order = order;
        if (order != null) {
            // Ne pas définir orderId manuellement - laissé à JPA
            if (this.customerId == null) {
                this.customerId = order.getCustomerId();
            }
            if (this.customerName == null) {
                this.customerName = order.getCustomerName();
            }
            if (this.orderDate == null) {
                this.orderDate = order.getOrderDate();
            }
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Méthodes de calcul
    public void calculateTotal() {
        calculateTotals();
    }

    private void calculateTotals() {
        // Calcul du total de base
        double basePrice = this.price != null ? this.price : 0.0;
        int itemQuantity = this.quantity != null ? this.quantity : 0;
        this.total = basePrice * itemQuantity;

        // Calcul du total final avec taxes, remises et frais de livraison
        double discount = this.discountApplied != null ? this.discountApplied : 0.0;
        double tax = this.taxAmount != null ? this.taxAmount : 0.0;
        double shipping = this.shippingCost != null ? this.shippingCost : 0.0;

        this.finalTotal = this.total - discount + tax + shipping;

        // Assurer que le total final n'est pas négatif
        if (this.finalTotal < 0) {
            this.finalTotal = 0.0;
        }
    }

    // Méthodes utilitaires
    public void applyDiscount(Double discountPercentage) {
        if (discountPercentage != null && discountPercentage > 0 && discountPercentage <= 100) {
            double discountAmount = (this.total * discountPercentage) / 100;
            this.discountApplied = discountAmount;
            calculateTotals();
        }
    }

    public void applyFixedDiscount(Double fixedDiscount) {
        if (fixedDiscount != null && fixedDiscount > 0) {
            this.discountApplied = fixedDiscount;
            calculateTotals();
        }
    }

    public void applyTax(Double taxRate) {
        if (taxRate != null && taxRate > 0) {
            double taxAmount = (this.total * taxRate) / 100;
            this.taxAmount = taxAmount;
            calculateTotals();
        }
    }

    public void updateQuantity(Integer newQuantity) {
        if (newQuantity != null && newQuantity >= 0) {
            this.quantity = newQuantity;
            calculateTotals();
        }
    }

    public void incrementQuantity() {
        this.quantity = (this.quantity != null ? this.quantity : 0) + 1;
        calculateTotals();
    }

    public void decrementQuantity() {
        if (this.quantity != null && this.quantity > 0) {
            this.quantity--;
            calculateTotals();
        }
    }

    public Double getUnitPrice() {
        return this.price;
    }

    public Double getSubtotal() {
        return this.total;
    }

    public boolean isInStock() {
        return this.product != null && this.product.getStockQuantity() != null
                && this.product.getStockQuantity() >= this.quantity;
    }

    public boolean hasSufficientStock() {
        return this.product != null && this.product.getStockQuantity() != null
                && this.product.getStockQuantity() >= this.quantity;
    }

    public String getStatusInFrench() {
        switch (this.status) {
            case "PENDING": return "En attente";
            case "CONFIRMED": return "Confirmé";
            case "SHIPPED": return "Expédié";
            case "DELIVERED": return "Livré";
            case "CANCELLED": return "Annulé";
            case "RETURNED": return "Retourné";
            default: return this.status;
        }
    }

    public void markAsShipped() {
        this.status = "SHIPPED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.status = "DELIVERED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCancelled() {
        this.status = "CANCELLED";
        this.updatedAt = LocalDateTime.now();
    }

    // Callbacks JPA
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
        if (this.quantity == null) this.quantity = 0;
        if (this.price == null) this.price = 0.0;
        if (this.discountApplied == null) this.discountApplied = 0.0;
        if (this.taxAmount == null) this.taxAmount = 0.0;
        if (this.shippingCost == null) this.shippingCost = 0.0;
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "TND";

        calculateTotals();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", total=" + total +
                ", finalTotal=" + finalTotal +
                ", orderId=" + getOrderId() +
                ", status='" + status + '\'' +
                '}';
    }

    // Méthodes equals et hashCode pour une bonne gestion des collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;

        OrderItem orderItem = (OrderItem) o;

        if (getId() != null ? !getId().equals(orderItem.getId()) : orderItem.getId() != null) return false;
        return getProductId() != null ? getProductId().equals(orderItem.getProductId()) : orderItem.getProductId() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getProductId() != null ? getProductId().hashCode() : 0);
        return result;
    }
}