package com.example.byomas.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_first_name", nullable = false)
    private String customerFirstName;

    @Column(name = "customer_last_name", nullable = false)
    private String customerLastName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "city")
    private String city;

    @Column(name = "governorate")
    private String governorate;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "status")
    private String status;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "shipping_method")
    private String shippingMethod;

    @Column(name = "tracking_number")
    private String trackingNumber;

    // ✅ NOUVEAU CHAMP : Code coupon appliqué
    @Column(name = "applied_coupon_code")
    private String appliedCouponCode;

    // ✅ NOUVEAU CHAMP : Indique si le coupon a été incrémenté
    @Column(name = "coupon_incremented")
    private Boolean couponIncremented = false;

    // CORRECTION : Charger les items en EAGER pour l'admin
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "subtotal")
    private Double subtotal = 0.0;

    @Column(name = "shipping_cost")
    private Double shippingCost = 0.0;

    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Constructeurs
    public CustomerOrder() {
        this.orderDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
        this.subtotal = 0.0;
        this.shippingCost = 0.0;
        this.taxAmount = 0.0;
        this.discountAmount = 0.0;
        this.totalAmount = 0.0;
        this.couponIncremented = false;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerFirstName() { return customerFirstName; }
    public void setCustomerFirstName(String customerFirstName) { this.customerFirstName = customerFirstName; }

    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getGovernorate() { return governorate; }
    public void setGovernorate(String governorate) { this.governorate = governorate; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    // ✅ Getter et Setter pour appliedCouponCode
    public String getAppliedCouponCode() { return appliedCouponCode; }
    public void setAppliedCouponCode(String appliedCouponCode) {
        this.appliedCouponCode = appliedCouponCode;
    }

    // ✅ Getter et Setter pour couponIncremented
    public Boolean getCouponIncremented() { return couponIncremented; }
    public void setCouponIncremented(Boolean couponIncremented) {
        this.couponIncremented = couponIncremented;
    }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Double getShippingCost() { return shippingCost; }
    public void setShippingCost(Double shippingCost) { this.shippingCost = shippingCost; }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
        if (orderItems != null) {
            for (OrderItem item : orderItems) {
                item.setOrder(this);
            }
        }
        calculateTotals();
    }

    // Méthodes de calcul
    public void calculateTotals() {
        // Calcul du sous-total à partir des items
        this.subtotal = 0.0;
        if (this.orderItems != null && !this.orderItems.isEmpty()) {
            for (OrderItem item : this.orderItems) {
                if (item.getTotal() != null) {
                    this.subtotal += item.getTotal();
                } else if (item.getPrice() != null && item.getQuantity() != null) {
                    this.subtotal += item.getPrice() * item.getQuantity();
                }
            }
        }

        // Calcul du total final
        double total = this.subtotal;

        // Appliquer la remise
        if (this.discountAmount != null && this.discountAmount > 0) {
            total -= this.discountAmount;
        }

        // Ajouter les taxes
        if (this.taxAmount != null && this.taxAmount > 0) {
            total += this.taxAmount;
        }

        // Ajouter les frais de livraison
        if (this.shippingCost != null && this.shippingCost > 0) {
            total += this.shippingCost;
        }

        // S'assurer que le total n'est pas négatif
        this.totalAmount = Math.max(0, total);
    }

    // Méthodes utilitaires
    public String getCustomerName() {
        return this.customerFirstName + " " + this.customerLastName;
    }

    public String getStatusInFrench() {
        switch (this.status != null ? this.status : "") {
            case "PENDING": return "En attente";
            case "CONFIRMED": return "Confirmée";
            case "SHIPPED": return "Expédiée";
            case "DELIVERED": return "Livrée";
            case "CANCELLED": return "Annulée";
            default: return this.status;
        }
    }

    public String getPaymentStatusInFrench() {
        switch (this.paymentStatus != null ? this.paymentStatus : "") {
            case "PENDING": return "En attente";
            case "PAID": return "Payée";
            case "FAILED": return "Échouée";
            case "REFUNDED": return "Remboursée";
            default: return this.paymentStatus;
        }
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
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = "PENDING";
        }
        if (this.couponIncremented == null) {
            this.couponIncremented = false;
        }
        calculateTotals();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateTotals();
    }

    @Override
    public String toString() {
        return "CustomerOrder{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", customerId=" + customerId +
                ", customerName='" + getCustomerName() + '\'' +
                ", appliedCouponCode='" + appliedCouponCode + '\'' +
                ", couponIncremented=" + couponIncremented +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}