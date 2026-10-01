package com.example.byomas.Repository;

import com.example.byomas.Entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Trouver les order items par orderId - CORRIGÉ
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    // Trouver les order items par productId
    List<OrderItem> findByProductId(Long productId);

    // Trouver les order items par customerId
    List<OrderItem> findByCustomerId(Long customerId);

    // Trouver les order items par statut
    List<OrderItem> findByStatus(String status);

    // Trouver les order items par plage de dates
    List<OrderItem> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    // Compter les order items par orderId - CORRIGÉ
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    long countByOrderId(@Param("orderId") Long orderId);

    // Compter les order items par productId
    long countByProductId(Long productId);

    // Compter les order items par statut
    long countByStatus(String status);

    // Trouver les order items avec un total supérieur à
    @Query("SELECT oi FROM OrderItem oi WHERE oi.total > :minTotal")
    List<OrderItem> findByTotalGreaterThan(@Param("minTotal") Double minTotal);

    // Trouver les order items les plus récents
    List<OrderItem> findAllByOrderByCreatedAtDesc();

    // Calculer le total des ventes pour un produit
    @Query("SELECT SUM(oi.total) FROM OrderItem oi WHERE oi.productId = :productId")
    Double getTotalSalesByProductId(@Param("productId") Long productId);

    // Calculer la quantité totale vendue pour un produit
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalQuantitySoldByProductId(@Param("productId") Long productId);

    // Trouver les produits les plus vendus
    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) as totalQuantity " +
            "FROM OrderItem oi GROUP BY oi.productId, oi.productName ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts();

    // Trouver les order items avec des informations produit
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderIdWithProduct(@Param("orderId") Long orderId);

    // Statistiques de vente par période
    @Query("SELECT SUM(oi.total), COUNT(oi), AVG(oi.total) " +
            "FROM OrderItem oi WHERE oi.orderDate BETWEEN :start AND :end")
    Object[] getSalesStatisticsByPeriod(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);

    // Vérifier si un produit a déjà été commandé
    boolean existsByProductId(Long productId);

    // Trouver les order items par catégorie de produit
    @Query("SELECT oi FROM OrderItem oi WHERE oi.productCategory = :category")
    List<OrderItem> findByProductCategory(@Param("category") String category);

    // NOUVELLES MÉTHODES UTILES

    // Trouver les order items par commande avec pagination
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId ORDER BY oi.createdAt DESC")
    List<OrderItem> findByOrderIdWithPagination(@Param("orderId") Long orderId);

    // Calculer le revenu total par produit
    @Query("SELECT oi.productId, oi.productName, SUM(oi.total) as revenue " +
            "FROM OrderItem oi GROUP BY oi.productId, oi.productName ORDER BY revenue DESC")
    List<Object[]> findProductRevenue();

    // Trouver les order items en attente
    @Query("SELECT oi FROM OrderItem oi WHERE oi.status = 'PENDING'")
    List<OrderItem> findPendingOrderItems();

    // Trouver les order items expédiés
    @Query("SELECT oi FROM OrderItem oi WHERE oi.status = 'SHIPPED'")
    List<OrderItem> findShippedOrderItems();

    // Trouver les order items livrés
    @Query("SELECT oi FROM OrderItem oi WHERE oi.status = 'DELIVERED'")
    List<OrderItem> findDeliveredOrderItems();

    // Mettre à jour le statut d'un order item
    @Query("UPDATE OrderItem oi SET oi.status = :status WHERE oi.id = :id")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // Trouver les order items par commande et statut
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.status = :status")
    List<OrderItem> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") String status);

    // Calculer le total des ventes par mois
    @Query("SELECT YEAR(oi.orderDate), MONTH(oi.orderDate), SUM(oi.total) " +
            "FROM OrderItem oi GROUP BY YEAR(oi.orderDate), MONTH(oi.orderDate) " +
            "ORDER BY YEAR(oi.orderDate) DESC, MONTH(oi.orderDate) DESC")
    List<Object[]> getMonthlySales();

    // Trouver les order items avec remise appliquée
    @Query("SELECT oi FROM OrderItem oi WHERE oi.discountApplied > 0")
    List<OrderItem> findOrderItemsWithDiscount();

    // Calculer la moyenne des quantités commandées par produit
    @Query("SELECT oi.productId, oi.productName, AVG(oi.quantity) as avgQuantity " +
            "FROM OrderItem oi GROUP BY oi.productId, oi.productName")
    List<Object[]> findAverageQuantityPerProduct();

    // Trouver les commandes récentes d'un client
    @Query("SELECT oi FROM OrderItem oi WHERE oi.customerId = :customerId ORDER BY oi.orderDate DESC")
    List<OrderItem> findRecentOrderItemsByCustomer(@Param("customerId") Long customerId);

    // Vérifier le stock disponible pour un produit
    @Query("SELECT p.stockQuantity FROM Product p WHERE p.id = :productId")
    Integer getProductStock(@Param("productId") Long productId);

    // Mettre à jour la quantité d'un order item
    @Query("UPDATE OrderItem oi SET oi.quantity = :quantity, oi.total = :price * :quantity WHERE oi.id = :id")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("price") Double price);
}