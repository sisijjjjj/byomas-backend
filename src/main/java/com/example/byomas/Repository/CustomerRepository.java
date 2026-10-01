package com.example.byomas.Repository;

import com.example.byomas.Entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Méthodes de base
    List<Customer> findAllByOrderByCreatedAtDesc();
    List<Customer> findByIsActiveTrue();

    List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    // Compteurs
    long countByIsActiveTrue();

    // Nouveaux clients par période
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.registrationDate BETWEEN :startDate AND :endDate")
    long countByRegistrationDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Clients avec commandes
    @Query("SELECT c FROM Customer c WHERE c.totalOrders > 0 ORDER BY c.totalOrders DESC")
    List<Customer> findCustomersWithOrders();
    Optional<Customer> findByEmail(String email);
    // Top clients par dépenses
    @Query("SELECT c FROM Customer c WHERE c.totalSpent > 0 ORDER BY c.totalSpent DESC")
    List<Customer> findTopCustomersBySpending();

    // Recherche avancée
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.governorate) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY c.createdAt DESC")
    List<Customer> searchCustomers(@Param("keyword") String keyword);

    // Clients par ville
    @Query("SELECT c.city, COUNT(c) FROM Customer c GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<Object[]> countCustomersByCity();

    // Clients inactifs (pas de commandes récentes)
    @Query("SELECT c FROM Customer c WHERE c.lastOrderDate < :thresholdDate OR c.lastOrderDate IS NULL")
    List<Customer> findInactiveCustomers(@Param("thresholdDate") LocalDateTime thresholdDate);

    boolean existsByEmail(String email);
}