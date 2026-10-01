package com.example.byomas.Repository;

import com.example.byomas.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // === METHODES DE BASE ===

    // Trouver par catégorie
    List<Product> findByCategory(String category);

    // Trouver par référence
    Optional<Product> findByReference(String reference);

    // Trouver par quantité en stock
    List<Product> findByStockQuantity(Integer stockQuantity);

    // Trouver les produits actifs
    List<Product> findByIsActiveTrue();

    // Trouver les produits inactifs
    List<Product> findByIsActiveFalse();

    // Recherche par nom (insensible à la casse)
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Recherche par description (insensible à la casse)
    List<Product> findByDescriptionContainingIgnoreCase(String keyword);

    // Trouver les produits avec badge
    List<Product> findByBadgeIsNotNull();

    // === METHODES AVANCEES AVEC @Query ===

    // Produits en stock faible
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < :threshold AND p.stockQuantity > 0 AND p.isActive = true")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Produits en rupture de stock
    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProducts();

    // Produits par plage de prix
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true")
    List<Product> findByPriceBetween(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    // Produits avec offre spéciale
    @Query("SELECT p FROM Product p WHERE p.specialOffer IS NOT NULL AND p.specialOffer != '' AND p.isActive = true")
    List<Product> findProductsWithSpecialOffer();

    // Produits récemment ajoutés
    @Query("SELECT p FROM Product p WHERE p.createdAt >= :date AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRecentlyAddedProducts(@Param("date") LocalDateTime date);

    // Produits récemment mis à jour
    @Query("SELECT p FROM Product p WHERE p.updatedAt >= :date AND p.isActive = true ORDER BY p.updatedAt DESC")
    List<Product> findRecentlyUpdatedProducts(@Param("date") LocalDateTime date);

    // Produits les plus vendus
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.salesCount DESC")
    List<Product> findTopSellingProducts();

    // Produits populaires (avec note élevée)
    @Query("SELECT p FROM Product p WHERE p.rating >= :minRating AND p.isActive = true ORDER BY p.rating DESC")
    List<Product> findTopRatedProducts(@Param("minRating") Double minRating);

    // AJOUTEZ CETTE METHODE - CORRECTION DU PROBLEME
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.createdAt DESC LIMIT 10")
    List<Product> findRecentProducts();

    // Recherche avancée de produits
    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.reference) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "p.isActive = true")
    List<Product> searchProducts(@Param("query") String query);

    // Recherche avec filtres multiples
    @Query("SELECT p FROM Product p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:minStock IS NULL OR p.stockQuantity >= :minStock) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive)")
    List<Product> findProductsByFilters(@Param("category") String category,
                                        @Param("minPrice") Double minPrice,
                                        @Param("maxPrice") Double maxPrice,
                                        @Param("minStock") Integer minStock,
                                        @Param("isActive") Boolean isActive);

    // === METHODES DE COMPTAGE ===

    // Compter par catégorie
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category")
    long countByCategory(@Param("category") String category);

    // Compter les produits actifs
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();

    // Compter les produits en rupture de stock
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0 AND p.isActive = true")
    long countOutOfStockProducts();

    // Compter les produits en stock faible
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity < :threshold AND p.stockQuantity > 0 AND p.isActive = true")
    long countLowStockProducts(@Param("threshold") Integer threshold);

    // Compter par quantité de stock
    long countByStockQuantity(int stockQuantity);

    // Compter les produits avec une quantité de stock inférieure à
    long countByStockQuantityLessThan(int stockQuantity);

    // Compter les produits actifs
    long countByIsActiveTrue();

    // Compter les produits inactifs
    long countByIsActiveFalse();

    // === METHODES DE STATISTIQUES ===

    // Statistiques par catégorie
    @Query("SELECT p.category, COUNT(p), SUM(p.stockQuantity), AVG(p.price) FROM Product p WHERE p.isActive = true GROUP BY p.category")
    List<Object[]> getCategoryStatistics();

    // Valeur totale du stock
    @Query("SELECT SUM(p.price * p.stockQuantity) FROM Product p WHERE p.isActive = true")
    Double getTotalStockValue();

    // Prix moyen des produits
    @Query("SELECT AVG(p.price) FROM Product p WHERE p.isActive = true")
    Double getAverageProductPrice();

    // Produits les plus chers
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.price DESC")
    List<Product> findMostExpensiveProducts();

    // Produits les moins chers
    @Query("SELECT p FROM Product p WHERE p.isActive = true ORDER BY p.price ASC")
    List<Product> findCheapestProducts();

    // === METHODES DE TRI ===

    // Tous les produits triés par date de création (décroissant)
    List<Product> findAllByOrderByCreatedAtDesc();

    // Produits actifs triés par date de création (décroissant)
    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    // Produits triés par nom
    List<Product> findAllByOrderByNameAsc();

    // Produits triés par prix (croissant)
    List<Product> findAllByOrderByPriceAsc();

    // Produits triés par prix (décroissant)
    List<Product> findAllByOrderByPriceDesc();

    // Produits triés par quantité en stock (croissant)
    List<Product> findAllByOrderByStockQuantityAsc();

    // Produits triés par quantité en stock (décroissant)
    List<Product> findAllByOrderByStockQuantityDesc();

    // Produits triés par nombre de ventes (décroissant)
    List<Product> findAllByOrderBySalesCountDesc();

    // Produits triés par note (décroissant)
    List<Product> findAllByOrderByRatingDesc();

    // === METHODES DE MISE A JOUR ===

    // Mettre à jour le stock
    @Query("UPDATE Product p SET p.stockQuantity = :newQuantity, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void updateStockQuantity(@Param("id") Long id, @Param("newQuantity") Integer newQuantity);

    // Incrémenter le compteur de ventes
    @Query("UPDATE Product p SET p.salesCount = p.salesCount + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void incrementSalesCount(@Param("id") Long id);

    // Mettre à jour le prix
    @Query("UPDATE Product p SET p.price = :newPrice, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void updatePrice(@Param("id") Long id, @Param("newPrice") Double newPrice);

    // Activer/Désactiver un produit
    @Query("UPDATE Product p SET p.isActive = :isActive, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :id")
    void updateActiveStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);

    // === METHODES UTILITAIRES ===

    // Vérifier si une référence existe
    boolean existsByReference(String reference);

    // Vérifier si un nom existe (insensible à la casse)
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE LOWER(p.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // Trouver les doublons potentiels
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND p.id != :id")
    List<Product> findPotentialDuplicates(@Param("name") String name, @Param("id") Long id);

    // Produits nécessitant une réapprovisionnement
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.minStockThreshold AND p.isActive = true ORDER BY p.stockQuantity ASC")
    List<Product> findProductsNeedingRestock();

    // Produits avec images
    @Query("SELECT p FROM Product p WHERE p.imageUrl IS NOT NULL AND p.imageUrl != '' AND p.isActive = true")
    List<Product> findProductsWithImages();

    // Produits sans images
    @Query("SELECT p FROM Product p WHERE (p.imageUrl IS NULL OR p.imageUrl = '') AND p.isActive = true")
    List<Product> findProductsWithoutImages();

    // === METHODES PAR DEFAUT ===

    // Méthode utilitaire pour les produits récemment ajoutés (30 derniers jours)
    default List<Product> findRecentlyAddedProducts() {
        return findRecentlyAddedProducts(LocalDateTime.now().minusDays(30));
    }

    // Méthode utilitaire pour les produits récemment mis à jour (7 derniers jours)
    default List<Product> findRecentlyUpdatedProducts() {
        return findRecentlyUpdatedProducts(LocalDateTime.now().minusDays(7));
    }

    // Méthode utilitaire pour les produits bien notés (4+ étoiles)
    default List<Product> findTopRatedProducts() {
        return findTopRatedProducts(4.0);
    }

    // Méthode utilitaire pour le stock faible (moins de 10 unités)
    default List<Product> findLowStockProducts() {
        return findLowStockProducts(10);
    }

    // Méthode utilitaire pour compter le stock faible
    default long countLowStockProducts() {
        return countLowStockProducts(10);
    }

    List<Product> findByStockQuantityLessThanOrderByStockQuantityAsc(int i);

    // Solution alternative - vous pouvez aussi utiliser cette méthode
    List<Product> findTop10ByOrderByCreatedAtDesc();

    // Compter les produits par catégorie
    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.isActive = true GROUP BY p.category")
    List<Object[]> countProductsByCategory();

    List<Product> findTop5ByOrderBySalesCountDesc();


    Object countByStockQuantityGreaterThanEqual(int i);

    // Ou une version plus simple avec une requête native
    @Query(value = "SELECT * FROM products WHERE stock_quantity IS NOT NULL AND CAST(stock_quantity AS INTEGER) <= :threshold AND is_active = true ORDER BY CAST(stock_quantity AS INTEGER) ASC", nativeQuery = true)
    List<Product> findLowStockProducts(@Param("threshold") int threshold);
}