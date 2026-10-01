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
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    @Query("SELECT COUNT(DISTINCT co.customerEmail) FROM CustomerOrder co")
    Long countDistinctCustomersWithOrders();
    // ==================== MÉTHODES DE BASE ====================
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    List<CustomerOrder> findByStatus(String status);
    List<CustomerOrder> findByCustomerEmailOrderByOrderDateDesc(String email);
    List<CustomerOrder> findByCustomerIdOrderByOrderDateDesc(Long customerId);
    List<CustomerOrder> findByOrderNumberContainingIgnoreCase(String orderNumber);
    List<CustomerOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // ==================== MÉTHODES DE TRI POUR L'ADMIN ====================
    List<CustomerOrder> findAllByOrderByOrderDateDesc();
    List<CustomerOrder> findByStatusOrderByOrderDateDesc(String status);
    List<CustomerOrder> findByPaymentStatusOrderByOrderDateDesc(String paymentStatus);
    List<CustomerOrder> findByCustomerEmail(String email);

    // ==================== MÉTHODES DE COMPTAGE ====================
    long countByStatus(String status);
    long countByPaymentStatus(String paymentStatus);
    long countByCustomerEmail(String email);
    long countByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    // ==================== MÉTHODES DE RECHERCHE AVANCÉES ====================
    List<CustomerOrder> findTop10ByOrderByOrderDateDesc();
    List<CustomerOrder> findTop5ByOrderByOrderDateDesc();
    List<CustomerOrder> findByStatusIn(List<String> statuses);

    // ==================== REQUÊTES PERSONNALISÉES POUR L'ADMIN ====================

    // REVENUS ET STATISTIQUES FINANCIÈRES
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status NOT IN ('CANCELLED', 'PENDING')")
    Double getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status NOT IN ('CANCELLED', 'PENDING')")
    Double getTotalRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status = :status")
    Double getRevenueByStatus(@Param("status") String status);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM CustomerOrder o WHERE o.status NOT IN ('CANCELLED', 'PENDING')")
    Double getAverageOrderValue();

    // COMMANDES ACTIVES ET EN COURS
    @Query("SELECT o FROM CustomerOrder o WHERE o.status IN ('PENDING', 'CONFIRMED', 'SHIPPED') ORDER BY o.orderDate DESC")
    List<CustomerOrder> findActiveOrders();

    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'PENDING' ORDER BY o.orderDate ASC")
    List<CustomerOrder> findPendingOrders();

    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'CONFIRMED' ORDER BY o.orderDate ASC")
    List<CustomerOrder> findConfirmedOrders();

    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'SHIPPED' ORDER BY o.shippedAt DESC")
    List<CustomerOrder> findShippedOrders();

    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'DELIVERED' ORDER BY o.deliveredAt DESC")
    List<CustomerOrder> findDeliveredOrders();

    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'CANCELLED' ORDER BY o.cancelledAt DESC")
    List<CustomerOrder> findCancelledOrders();

    // STATISTIQUES PAR PÉRIODE
    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Long countOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Double findRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // RECHERCHE AVANCÉE
    @Query("SELECT o FROM CustomerOrder o WHERE " +
            "LOWER(o.customerFirstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.customerLastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.trackingNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY o.orderDate DESC")
    List<CustomerOrder> searchOrders(@Param("keyword") String keyword);

    // STATISTIQUES PAR MOIS (version corrigée pour MySQL)
    @Query(value = "SELECT DATE_FORMAT(o.order_date, '%Y-%m') as month, " +
            "COUNT(o.id) as orderCount, " +
            "COALESCE(SUM(o.total_amount), 0) as revenue " +
            "FROM customer_orders o " +
            "WHERE o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.status NOT IN ('CANCELLED') " +
            "GROUP BY DATE_FORMAT(o.order_date, '%Y-%m') " +
            "ORDER BY month DESC", nativeQuery = true)
    List<Object[]> getMonthlyStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // TOP CLIENTS
    @Query("SELECT o.customerEmail, COUNT(o) as orderCount, COALESCE(SUM(o.totalAmount), 0) as totalSpent " +
            "FROM CustomerOrder o " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY o.customerEmail " +
            "ORDER BY totalSpent DESC")
    List<Object[]> getTopCustomers();

    // COMMANDES RÉCENTES AVEC DÉLAIS
    @Query("SELECT o FROM CustomerOrder o WHERE " +
            "o.status IN ('PENDING', 'CONFIRMED') AND " +
            "o.orderDate < :thresholdDate " +
            "ORDER BY o.orderDate ASC")
    List<CustomerOrder> findOverdueOrders(@Param("thresholdDate") LocalDateTime thresholdDate);

    // STATISTIQUES DES MÉTHODES DE PAIEMENT
    @Query("SELECT o.paymentMethod, COUNT(o) as count, COALESCE(SUM(o.totalAmount), 0) as total " +
            "FROM CustomerOrder o " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY o.paymentMethod")
    List<Object[]> getPaymentMethodStats();

    // STATISTIQUES DES MÉTHODES DE LIVRAISON
    @Query("SELECT o.shippingMethod, COUNT(o) as count, COALESCE(SUM(o.totalAmount), 0) as total " +
            "FROM CustomerOrder o " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY o.shippingMethod")
    List<Object[]> getShippingMethodStats();

    // COMMANDES SANS TRACKING
    @Query("SELECT o FROM CustomerOrder o WHERE o.status = 'SHIPPED' AND o.trackingNumber IS NULL")
    List<CustomerOrder> findShippedOrdersWithoutTracking();

    // COMMANDES AVEC PROBLÈMES DE PAIEMENT
    @Query("SELECT o FROM CustomerOrder o WHERE o.paymentStatus IN ('FAILED', 'REFUNDED') ORDER BY o.orderDate DESC")
    List<CustomerOrder> findProblematicPaymentOrders();

    // NOUVEAUX CLIENTS (première commande)
    @Query(value = "SELECT o.customer_email, MIN(o.order_date) as firstOrderDate " +
            "FROM customer_orders o " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY o.customer_email " +
            "HAVING MIN(o.order_date) BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Object[]> findNewCustomers(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // COMMANDES AVEC REMISES
    @Query("SELECT o FROM CustomerOrder o WHERE o.discountAmount > 0 ORDER BY o.discountAmount DESC")
    List<CustomerOrder> findOrdersWithDiscounts();

    // PERFORMANCE DES LIVRAISONS - Version corrigée sans TIMESTAMPDIFF
    @Query("SELECT o FROM CustomerOrder o WHERE o.shippedAt IS NOT NULL AND o.deliveredAt IS NOT NULL")
    List<CustomerOrder> findCompletedDeliveries();

    // Pour le temps moyen de livraison, on le calculera dans le service
    @Query("SELECT o.shippedAt, o.deliveredAt FROM CustomerOrder o WHERE o.shippedAt IS NOT NULL AND o.deliveredAt IS NOT NULL")
    List<Object[]> getDeliveryTimestamps();

    // STATISTIQUES QUOTIDIENNES
    @Query(value = "SELECT DATE(o.order_date) as orderDate, " +
            "COUNT(o.id) as orderCount, " +
            "COALESCE(SUM(o.total_amount), 0) as revenue " +
            "FROM customer_orders o " +
            "WHERE o.order_date BETWEEN :startDate AND :endDate " +
            "AND o.status NOT IN ('CANCELLED') " +
            "GROUP BY DATE(o.order_date) " +
            "ORDER BY orderDate DESC", nativeQuery = true)
    List<Object[]> getDailyStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // PRODUITS LES PLUS VENDUS
    @Query(value = "SELECT oi.product_name, SUM(oi.quantity) as totalQuantity, " +
            "SUM(oi.quantity * oi.price) as totalRevenue " +
            "FROM order_items oi " +
            "JOIN customer_orders o ON oi.order_id = o.id " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY oi.product_name " +
            "ORDER BY totalQuantity DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> getTopSellingProducts();

    // COMMANDES PAR RÉGION (basé sur l'adresse)
    @Query("SELECT o.deliveryAddress, COUNT(o) as orderCount, COALESCE(SUM(o.totalAmount), 0) as totalRevenue " +
            "FROM CustomerOrder o " +
            "WHERE o.status NOT IN ('CANCELLED') " +
            "GROUP BY o.deliveryAddress " +
            "ORDER BY orderCount DESC")
    List<Object[]> getOrdersByRegion();

    // TAUX DE CONVERSION (commandes par statut)
    @Query("SELECT o.status, COUNT(o) as count " +
            "FROM CustomerOrder o " +
            "GROUP BY o.status")
    List<Object[]> getOrderStatusDistribution();

    // COMMANDES AVEC LE PLUS GRAND PANIER
    @Query("SELECT o FROM CustomerOrder o WHERE o.status NOT IN ('CANCELLED') ORDER BY o.totalAmount DESC")
    List<CustomerOrder> findHighestValueOrders();

    // COMMANDES RÉCURRENTES (clients avec multiples commandes)
    @Query(value = "SELECT customer_email, COUNT(id) as orderCount " +
            "FROM customer_orders " +
            "WHERE status NOT IN ('CANCELLED') " +
            "GROUP BY customer_email " +
            "HAVING COUNT(id) > 1 " +
            "ORDER BY orderCount DESC", nativeQuery = true)
    List<Object[]> findRecurringCustomers();

    List<CustomerOrder> findByAppliedCouponCode(String couponCode);
}