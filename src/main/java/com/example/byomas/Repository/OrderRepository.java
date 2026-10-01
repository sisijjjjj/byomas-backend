package com.example.byomas.Repository;

import com.example.byomas.Entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    // Méthodes de base
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    List<CustomerOrder> findByStatus(String status);
    List<CustomerOrder> findByCustomerEmailOrderByOrderDateDesc(String email);
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);
    List<CustomerOrder> findByOrderNumberContainingIgnoreCase(String orderNumber);
    List<CustomerOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Méthodes de tri
    List<CustomerOrder> findAllByOrderByOrderDateDesc();
    List<CustomerOrder> findByStatusOrderByOrderDateDesc(String status);

    // Compteurs
    long countByStatus(String status);

    // REQUÊTES PERSONNALISÉES CORRIGÉES
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status NOT IN ('CANCELLED', 'PENDING')")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status NOT IN ('CANCELLED', 'PENDING')")
    Double findRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM CustomerOrder o WHERE o.status IN ('PENDING', 'CONFIRMED', 'SHIPPED') ORDER BY o.orderDate DESC")
    List<CustomerOrder> findActiveOrders();
}