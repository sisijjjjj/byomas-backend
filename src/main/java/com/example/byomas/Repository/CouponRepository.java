package com.example.byomas.Repository;

import com.example.byomas.Entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // Trouver un coupon par code
    Optional<Coupon> findByCode(String code);

    // ✅ CORRECTION: Méthode avec verrouillage pessimiste pour éviter les problèmes de concurrence
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    // Trouver un coupon par code avec produits applicables
    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE c.code = :code")
    Optional<Coupon> findByCodeWithProducts(@Param("code") String code);

    // Vérifier si un code existe déjà
    boolean existsByCode(String code);

    // Trouver les coupons actifs
    List<Coupon> findByIsActiveTrue();

    // Trouver les coupons actifs avec produits applicables
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE c.isActive = true")
    List<Coupon> findActiveCouponsWithProducts();

    // Trouver les coupons inactifs
    List<Coupon> findByIsActiveFalse();

    // Trouver les coupons expirés
    @Query("SELECT c FROM Coupon c WHERE c.expiryDate < :currentDate AND c.isActive = true")
    List<Coupon> findExpiredCoupons(@Param("currentDate") LocalDateTime currentDate);

    default List<Coupon> findExpiredCoupons() {
        return findExpiredCoupons(LocalDateTime.now());
    }

    // Trouver les coupons actifs et non expirés
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.expiryDate >= :currentDate")
    List<Coupon> findActiveAndValidCoupons(@Param("currentDate") LocalDateTime currentDate);

    default List<Coupon> findActiveAndValidCoupons() {
        return findActiveAndValidCoupons(LocalDateTime.now());
    }

    // Trouver les coupons actifs et valides avec produits applicables
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE c.isActive = true AND c.expiryDate >= CURRENT_TIMESTAMP")
    List<Coupon> findActiveAndValidCouponsWithProducts();

    // Trouver les coupons par type de réduction
    List<Coupon> findByDiscountType(String discountType);

    // Trouver les coupons actifs par type de réduction
    List<Coupon> findByDiscountTypeAndIsActiveTrue(String discountType);

    // Compter le nombre de coupons actifs
    long countByIsActiveTrue();

    // Trouver les coupons qui expirent bientôt
    @Query("SELECT c FROM Coupon c WHERE c.expiryDate BETWEEN :currentDate AND :futureDate AND c.isActive = true")
    List<Coupon> findCouponsExpiringSoon(@Param("currentDate") LocalDateTime currentDate,
                                         @Param("futureDate") LocalDateTime futureDate);

    default List<Coupon> findCouponsExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        return findCouponsExpiringSoon(now, now.plusDays(7));
    }

    // Trouver tous les coupons triés par date de création
    List<Coupon> findAllByOrderByCreatedAtDesc();

    // Trouver tous les coupons actifs triés par date de création
    List<Coupon> findByIsActiveTrueOrderByCreatedAtDesc();

    // ==================== MÉTHODES D'INCRÉMENTATION ====================

    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = COALESCE(c.usedCount, 0) + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.code = :code")
    int incrementUsedCountByCode(@Param("code") String code);

    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.usedCount = COALESCE(c.usedCount, 0) + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    int incrementUsedCountById(@Param("id") Long id);

    @Transactional
    default Optional<Coupon> incrementAndFetchCoupon(String code) {
        int updated = incrementUsedCountByCode(code);
        if (updated > 0) {
            return findByCode(code);
        }
        return Optional.empty();
    }

    // ==================== MÉTHODES DE VALIDATION ====================

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Coupon c " +
            "WHERE c.code = :code AND c.isActive = true " +
            "AND c.expiryDate >= CURRENT_TIMESTAMP " +
            "AND (c.maxUses IS NULL OR COALESCE(c.usedCount, 0) < c.maxUses)")
    boolean isUsableCoupon(@Param("code") String code);

    @Query("SELECT CASE WHEN c.maxUses IS NULL THEN NULL ELSE c.maxUses - COALESCE(c.usedCount, 0) END " +
            "FROM Coupon c WHERE c.code = :code")
    Integer getRemainingUses(@Param("code") String code);

    // ==================== MÉTHODES DE STATISTIQUES ====================

    // Méthode 1: Calculer la somme des ratios
    @Query("SELECT SUM(COALESCE(c.usedCount, 0) * 1.0 / c.maxUses) FROM Coupon c WHERE c.maxUses IS NOT NULL AND c.maxUses > 0")
    Double getSumOfUsageRatios();

    // Méthode 2: Compter le nombre de coupons avec maxUses défini
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.maxUses IS NOT NULL AND c.maxUses > 0")
    Long countCouponsWithMaxUses();

    // ✅ Méthode par défaut qui calcule la moyenne sans erreur de type
    default Double getAverageUsageRate() {
        Double sum = getSumOfUsageRatios();
        Long count = countCouponsWithMaxUses();

        if (sum == null || count == null || count == 0) {
            return 0.0;
        }

        return sum / count;
    }

    // Alternative: Version native query si la version JPA ne fonctionne pas
    @Query(value = "SELECT AVG(used_count * 1.0 / max_uses) FROM coupons WHERE max_uses IS NOT NULL AND max_uses > 0", nativeQuery = true)
    Double getAverageUsageRateNative();

    // Trouver les coupons les plus utilisés
    @Query("SELECT c FROM Coupon c ORDER BY c.usedCount DESC NULLS LAST")
    List<Coupon> findMostUsedCoupons();

    // Trouver les coupons qui ont atteint leur limite
    @Query("SELECT c FROM Coupon c WHERE c.maxUses IS NOT NULL AND COALESCE(c.usedCount, 0) >= c.maxUses")
    List<Coupon> findFullyUsedCoupons();

    // Trouver les coupons jamais utilisés
    @Query("SELECT c FROM Coupon c WHERE COALESCE(c.usedCount, 0) = 0")
    List<Coupon> findUnusedCoupons();

    // ==================== MÉTHODES POUR APPLICABLEPRODUCTS ====================

    @Query("SELECT c FROM Coupon c WHERE :productId MEMBER OF c.applicableProducts AND c.isActive = true")
    List<Coupon> findCouponsApplicableToProduct(@Param("productId") Long productId);

    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE :productId MEMBER OF c.applicableProducts AND c.isActive = true")
    List<Coupon> findCouponsApplicableToProductWithDetails(@Param("productId") Long productId);

    // ==================== MÉTHODES DE CHARGEMENT ====================

    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts WHERE c.id = :id")
    Optional<Coupon> findByIdWithProducts(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts")
    List<Coupon> findAllWithProducts();

    @Override
    @Query("SELECT DISTINCT c FROM Coupon c LEFT JOIN FETCH c.applicableProducts")
    List<Coupon> findAll();
}