package com.example.byomas.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String governorate;

    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    @Column(name = "total_spent")
    private Double totalSpent = 0.0;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructeurs
    public Customer() {
        this.registrationDate = LocalDateTime.now();
        this.totalOrders = 0;
        this.totalSpent = 0.0;
        this.isActive = true;
    }

    public Customer(String firstName, String lastName, String email) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public Customer(String firstName, String lastName, String email, String phoneNumber) {
        this(firstName, lastName, email);
        this.phoneNumber = phoneNumber;
    }

    public Customer(String firstName, String lastName, String email, String phoneNumber, String address) {
        this(firstName, lastName, email, phoneNumber);
        this.address = address;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getGovernorate() {
        return governorate;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public Double getTotalSpent() {
        return totalSpent;
    }

    public LocalDateTime getLastOrderDate() {
        return lastOrderDate;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setGovernorate(String governorate) {
        this.governorate = governorate;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders != null ? totalOrders : 0;
    }

    public void setTotalSpent(Double totalSpent) {
        this.totalSpent = totalSpent != null ? totalSpent : 0.0;
    }

    public void setLastOrderDate(LocalDateTime lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Méthodes utilitaires
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getName() {
        return getFullName();
    }

    public void setName(String name) {
        if (name != null && name.contains(" ")) {
            String[] names = name.split(" ", 2);
            this.firstName = names[0];
            this.lastName = names[1];
        } else {
            this.firstName = name;
            this.lastName = "";
        }
    }

    public String getPhone() {
        return phoneNumber;
    }

    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }

    public void addOrder(Double amount) {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.totalSpent = (this.totalSpent == null ? 0.0 : this.totalSpent) + (amount != null ? amount : 0.0);
        this.lastOrderDate = LocalDateTime.now();
    }

    public void updateSpending(Double amount) {
        if (amount != null) {
            this.totalSpent = (this.totalSpent == null ? 0.0 : this.totalSpent) + amount;
        }
    }

    public void incrementOrderCount() {
        this.totalOrders = (this.totalOrders == null ? 0 : this.totalOrders) + 1;
        this.lastOrderDate = LocalDateTime.now();
    }

    public boolean hasOrders() {
        return totalOrders != null && totalOrders > 0;
    }

    public boolean isNewCustomer() {
        return totalOrders == null || totalOrders == 0;
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (governorate != null && !governorate.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(governorate);
        }
        return sb.toString();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public boolean isActive() {
        return isActive != null && isActive;
    }

    // Méthodes de callback JPA
    @PrePersist
    protected void onCreate() {
        if (this.registrationDate == null) {
            this.registrationDate = LocalDateTime.now();
        }
        if (this.totalOrders == null) {
            this.totalOrders = 0;
        }
        if (this.totalSpent == null) {
            this.totalSpent = 0.0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) &&
                Objects.equals(email, customer.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    // toString
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", totalOrders=" + totalOrders +
                ", totalSpent=" + totalSpent +
                ", isActive=" + isActive +
                ", registrationDate=" + registrationDate +
                '}';
    }

    // Builder pattern (optionnel)
    public static class Builder {
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;
        private String address;
        private String city;
        private String governorate;

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder governorate(String governorate) {
            this.governorate = governorate;
            return this;
        }

        public Customer build() {
            Customer customer = new Customer(firstName, lastName, email);
            customer.setPhoneNumber(phoneNumber);
            customer.setAddress(address);
            customer.setCity(city);
            customer.setGovernorate(governorate);
            return customer;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Méthodes de validation
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                isValidEmail(email);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.append("First name is required. ");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            errors.append("Last name is required. ");
        }
        if (email == null || email.trim().isEmpty()) {
            errors.append("Email is required. ");
        } else if (!isValidEmail(email)) {
            errors.append("Email format is invalid. ");
        }
        return errors.toString().trim();
    }
}