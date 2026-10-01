package com.example.byomas.Service;

import com.example.byomas.Entity.Coupon;
import com.example.byomas.Repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * ✅ SIMPLE : Incrémenter le compteur d'utilisation d'un coupon
     */
    @Transactional
    public Coupon useCoupon(String code) {
        // 1. Récupérer le coupon
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Coupon non trouvé: " + code));

        // 2. Vérifier si le coupon est valide
        if (!coupon.isActive()) {
            throw new RuntimeException("Coupon inactif");
        }

        if (coupon.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Coupon expiré");
        }

        // 3. Vérifier la limite d'utilisations
        if (coupon.getMaxUses() != null &&
                coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new RuntimeException("Coupon déjà utilisé le maximum de fois");
        }

        // 4. ✅ INC RÉMENTER LE COMPTEUR (LA SOLUTION À VOTRE PROBLÈME)
        int ancienCompteur = coupon.getUsedCount();
        coupon.setUsedCount(ancienCompteur + 1);
        coupon.setUpdatedAt(java.time.LocalDateTime.now());

        // 5. Sauvegarder
        Coupon couponMisAJour = couponRepository.save(coupon);

        System.out.println("=== INC RÉMENTATION RÉUSSIE ===");
        System.out.println("Code: " + couponMisAJour.getCode());
        System.out.println("Ancien compteur: " + ancienCompteur);
        System.out.println("Nouveau compteur: " + couponMisAJour.getUsedCount());

        return couponMisAJour;
    }

    /**
     * ✅ Encore PLUS SIMPLE : Utiliser la méthode directe du repository
     */
    @Transactional
    public int incrementerDirectement(String code) {
        return couponRepository.incrementUsedCountByCode(code);
    }
}