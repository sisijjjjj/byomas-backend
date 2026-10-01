package com.example.byomas.Controller;

import com.example.byomas.Entity.Coupon;
import com.example.byomas.Entity.CustomerOrder;
import com.example.byomas.Entity.OrderItem;
import com.example.byomas.Repository.CouponRepository;
import com.example.byomas.Service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CouponRepository couponRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==================== GET ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        try {
            List<CustomerOrder> orders = orderService.getAllOrders();
            List<Map<String, Object>> simplifiedOrders = orders.stream()
                    .map(this::simplifyOrder)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(simplifiedOrders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération des commandes: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            Optional<CustomerOrder> order = orderService.getOrderById(id);
            if (order.isPresent()) {
                return ResponseEntity.ok(simplifyOrder(order.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Commande non trouvée avec l'id: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(@PathVariable String orderNumber) {
        try {
            Optional<CustomerOrder> order = orderService.getOrderByNumber(orderNumber);
            if (order.isPresent()) {
                return ResponseEntity.ok(simplifyOrder(order.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Commande non trouvée avec le numéro: " + orderNumber));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<?> getOrdersByCustomerId(@PathVariable Long customerId) {
        try {
            List<CustomerOrder> orders = orderService.getOrdersByCustomerId(customerId);
            List<Map<String, Object>> simplifiedOrders = orders.stream()
                    .map(this::simplifyOrder)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(simplifiedOrders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    @GetMapping("/customer/email/{email}")
    public ResponseEntity<?> getOrdersByCustomerEmail(@PathVariable String email) {
        try {
            List<CustomerOrder> orders = orderService.getOrdersByCustomer(email);
            List<Map<String, Object>> simplifiedOrders = orders.stream()
                    .map(this::simplifyOrder)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(simplifiedOrders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getOrdersByStatus(@PathVariable String status) {
        try {
            List<CustomerOrder> orders = orderService.getOrdersByStatus(status);
            List<Map<String, Object>> simplifiedOrders = orders.stream()
                    .map(this::simplifyOrder)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(simplifiedOrders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats() {
        try {
            Map<String, Object> stats = orderService.getOrderStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du calcul des statistiques: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<?> getTrackingInfo(@PathVariable Long id) {
        try {
            Optional<CustomerOrder> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isPresent()) {
                CustomerOrder order = orderOpt.get();
                Map<String, Object> trackingInfo = new HashMap<>();
                trackingInfo.put("orderNumber", order.getOrderNumber());
                trackingInfo.put("trackingNumber", order.getTrackingNumber());
                trackingInfo.put("shippingMethod", order.getShippingMethod());
                trackingInfo.put("status", order.getStatus());
                trackingInfo.put("shippedAt", order.getShippedAt());
                trackingInfo.put("customerId", order.getCustomerId());
                trackingInfo.put("customerName", order.getCustomerName());
                return ResponseEntity.ok(trackingInfo);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Commande non trouvée avec l'id: " + id));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération: " + e.getMessage()));
        }
    }

    @GetMapping("/coupons/debug/{code}")
    public ResponseEntity<?> debugCoupon(@PathVariable String code) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                Map<String, Object> debug = new HashMap<>();
                debug.put("id", coupon.getId());
                debug.put("code", coupon.getCode());
                debug.put("usedCount", coupon.getUsedCount());
                debug.put("maxUses", coupon.getMaxUses());
                debug.put("remainingUses", coupon.getRemainingUses());
                debug.put("isActive", coupon.getIsActive());
                debug.put("isExpired", coupon.isExpired());
                debug.put("discountType", coupon.getDiscountType());
                debug.put("discountValue", coupon.getDiscountValue());

                List<CustomerOrder> ordersWithCoupon = orderService.getOrdersByCouponCode(code);
                debug.put("ordersWithThisCoupon", ordersWithCoupon.size());

                return ResponseEntity.ok(debug);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Coupon non trouvé: " + code));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== POST ENDPOINTS ====================

    @PostMapping
    @Transactional
    public ResponseEntity<?> createOrder(@RequestBody CustomerOrder order) {
        try {
            System.out.println("=== CRÉATION DE COMMANDE ===");

            // Validation
            String validationError = validateOrder(order);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", validationError));
            }

            // Gestion du coupon
            Map<String, Object> couponResult = processCoupon(order);

            // Créer la commande
            CustomerOrder createdOrder = orderService.createOrder(order);

            // Construire la réponse simplifiée
            Map<String, Object> response = buildOrderResponse(createdOrder, order, couponResult);

            System.out.println("📤 Commande créée avec succès: " + createdOrder.getOrderNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            System.err.println("❌ Erreur lors de la création: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la création: " + e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<?> addOrderItem(@PathVariable Long orderId, @RequestBody OrderItem item) {
        try {
            if (item.getProductId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'ID du produit est obligatoire"));
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "La quantité doit être supérieure à 0"));
            }

            CustomerOrder updatedOrder = orderService.addOrderItem(orderId, item);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Produit ajouté avec succès");
            response.put("order", simplifyOrder(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'ajout: " + e.getMessage()));
        }
    }

    // ==================== PUT ENDPOINTS ====================

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> statusUpdate) {
        try {
            String newStatus = statusUpdate.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le champ status est requis"));
            }

            CustomerOrder updatedOrder = orderService.updateOrderStatus(id, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statut mis à jour avec succès");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("newStatus", newStatus);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id, @RequestBody Map<String, String> paymentStatusUpdate) {
        try {
            String newPaymentStatus = paymentStatusUpdate.get("paymentStatus");
            if (newPaymentStatus == null || newPaymentStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le champ paymentStatus est requis"));
            }

            CustomerOrder updatedOrder = orderService.updatePaymentStatus(id, newPaymentStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statut de paiement mis à jour");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("newPaymentStatus", newPaymentStatus);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/payment-method")
    public ResponseEntity<?> updatePaymentMethod(@PathVariable Long id, @RequestBody Map<String, String> paymentUpdate) {
        try {
            String paymentMethod = paymentUpdate.get("paymentMethod");
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La méthode de paiement est obligatoire"));
            }

            CustomerOrder updatedOrder = orderService.updatePaymentMethod(id, paymentMethod);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Méthode de paiement mise à jour");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("paymentMethod", paymentMethod);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/shipping-method")
    public ResponseEntity<?> updateShippingMethod(@PathVariable Long id, @RequestBody Map<String, String> shippingUpdate) {
        try {
            String shippingMethod = shippingUpdate.get("shippingMethod");
            if (shippingMethod == null || shippingMethod.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La méthode de livraison est obligatoire"));
            }

            CustomerOrder updatedOrder = orderService.updateShippingMethod(id, shippingMethod);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Méthode de livraison mise à jour");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("shippingMethod", shippingMethod);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/tracking")
    public ResponseEntity<?> updateTrackingNumber(@PathVariable Long id, @RequestBody Map<String, String> trackingUpdate) {
        try {
            String trackingNumber = trackingUpdate.get("trackingNumber");
            if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de suivi est obligatoire"));
            }

            CustomerOrder updatedOrder = orderService.updateTrackingNumber(id, trackingNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Numéro de suivi mis à jour");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("trackingNumber", trackingNumber);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la mise à jour: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/ship")
    public ResponseEntity<?> shipOrder(@PathVariable Long id, @RequestBody Map<String, String> shippingInfo) {
        try {
            String trackingNumber = shippingInfo.get("trackingNumber");
            if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le numéro de suivi est obligatoire"));
            }

            CustomerOrder updatedOrder = orderService.shipOrder(id, trackingNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commande expédiée avec succès");
            response.put("order", simplifyOrder(updatedOrder));
            response.put("trackingNumber", trackingNumber);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'expédition: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/confirm")
    @Transactional
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            Optional<CustomerOrder> existingOrderOpt = orderService.getOrderById(id);

            if (!existingOrderOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Commande non trouvée avec l'id: " + id));
            }

            CustomerOrder existingOrder = existingOrderOpt.get();
            Map<String, Object> couponResult = new HashMap<>();

            // Incrémenter le coupon si nécessaire
            if (existingOrder.getAppliedCouponCode() != null &&
                    !existingOrder.getAppliedCouponCode().isEmpty() &&
                    !"CONFIRMED".equals(existingOrder.getStatus())) {

                couponResult = incrementCouponOnConfirmation(existingOrder);
            }

            CustomerOrder updatedOrder = orderService.confirmOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commande confirmée avec succès");
            response.put("order", simplifyOrder(updatedOrder));

            if (!couponResult.isEmpty() && (boolean) couponResult.getOrDefault("success", false)) {
                response.put("couponUtilise", true);
                response.put("couponCode", existingOrder.getAppliedCouponCode());
                response.put("couponAncienCompteur", couponResult.get("oldCount"));
                response.put("couponNouveauCompteur", couponResult.get("newCount"));
            }

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la confirmation: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/deliver")
    public ResponseEntity<?> deliverOrder(@PathVariable Long id) {
        try {
            CustomerOrder updatedOrder = orderService.deliverOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commande livrée avec succès");
            response.put("order", simplifyOrder(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la livraison: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
        try {
            CustomerOrder updatedOrder = orderService.cancelOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Commande annulée avec succès");
            response.put("order", simplifyOrder(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'annulation: " + e.getMessage()));
        }
    }

    // ==================== DELETE ENDPOINTS ====================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Commande supprimée avec succès");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la suppression: " + e.getMessage()));
        }
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private String validateOrder(CustomerOrder order) {
        if (order.getCustomerFirstName() == null || order.getCustomerFirstName().trim().isEmpty()) {
            return "Le prénom du client est obligatoire";
        }
        if (order.getCustomerLastName() == null || order.getCustomerLastName().trim().isEmpty()) {
            return "Le nom du client est obligatoire";
        }
        if (order.getCustomerPhone() == null || order.getCustomerPhone().trim().isEmpty()) {
            return "Le téléphone est obligatoire";
        }
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().trim().isEmpty()) {
            return "L'adresse de livraison est obligatoire";
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return "La commande doit contenir au moins un produit";
        }
        return null;
    }

    private Map<String, Object> processCoupon(CustomerOrder order) {
        Map<String, Object> couponResult = new HashMap<>();

        if (order.getAppliedCouponCode() != null && !order.getAppliedCouponCode().isEmpty()) {
            String couponCode = order.getAppliedCouponCode();
            System.out.println("🎫 Coupon détecté: " + couponCode);

            try {
                Optional<Coupon> couponOpt = couponRepository.findByCodeForUpdate(couponCode);

                if (couponOpt.isPresent()) {
                    Coupon coupon = couponOpt.get();
                    List<String> validationErrors = validateCoupon(coupon, order.getTotalAmount());

                    if (!validationErrors.isEmpty()) {
                        couponResult.put("success", false);
                        couponResult.put("message", validationErrors.get(0));
                    } else {
                        int oldCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
                        int newCount = oldCount + 1;

                        coupon.setUsedCount(newCount);
                        coupon.setUpdatedAt(LocalDateTime.now());

                        Coupon savedCoupon = couponRepository.save(coupon);
                        couponRepository.flush();

                        couponResult.put("success", true);
                        couponResult.put("oldCount", oldCount);
                        couponResult.put("newCount", newCount);

                        if (savedCoupon.getMaxUses() != null) {
                            int remaining = savedCoupon.getMaxUses() - newCount;
                            couponResult.put("remainingUses", remaining);

                            if (remaining <= 0) {
                                couponResult.put("epuise", true);
                                couponResult.put("warning", "Ce coupon a atteint son nombre maximum d'utilisations");
                            } else if (remaining <= 3) {
                                couponResult.put("warning", "Attention: Il ne reste que " + remaining + " utilisation(s) pour ce coupon");
                            }
                        }
                    }
                } else {
                    couponResult.put("success", false);
                    couponResult.put("message", "Coupon non trouvé: " + couponCode);
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du traitement du coupon: " + e.getMessage());
                couponResult.put("success", false);
                couponResult.put("message", "Erreur technique: " + e.getMessage());
            }
        }

        return couponResult;
    }

    private Map<String, Object> incrementCouponOnConfirmation(CustomerOrder order) {
        Map<String, Object> couponResult = new HashMap<>();
        String couponCode = order.getAppliedCouponCode();

        try {
            Optional<Coupon> couponOpt = couponRepository.findByCodeForUpdate(couponCode);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                if (order.getCouponIncremented() != null && order.getCouponIncremented()) {
                    System.out.println("⚠️ Coupon déjà incrémenté pour cette commande");
                    couponResult.put("success", false);
                    couponResult.put("message", "Coupon déjà incrémenté");
                } else {
                    List<String> errors = validateCoupon(coupon, order.getTotalAmount());

                    if (errors.isEmpty()) {
                        int oldCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
                        int newCount = oldCount + 1;

                        coupon.setUsedCount(newCount);
                        coupon.setUpdatedAt(LocalDateTime.now());

                        Coupon savedCoupon = couponRepository.save(coupon);
                        couponRepository.flush();

                        order.setCouponIncremented(true);

                        couponResult.put("success", true);
                        couponResult.put("oldCount", oldCount);
                        couponResult.put("newCount", newCount);

                        if (savedCoupon.getMaxUses() != null) {
                            int remaining = savedCoupon.getMaxUses() - newCount;
                            if (remaining <= 3) {
                                couponResult.put("warning", "Attention: Il ne reste que " + remaining + " utilisation(s)");
                            }
                        }
                    } else {
                        couponResult.put("success", false);
                        couponResult.put("message", errors.get(0));
                    }
                }
            } else {
                couponResult.put("success", false);
                couponResult.put("message", "Coupon non trouvé");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'incrémentation du coupon: " + e.getMessage());
            couponResult.put("success", false);
            couponResult.put("message", "Erreur technique: " + e.getMessage());
        }

        return couponResult;
    }

    private List<String> validateCoupon(Coupon coupon, Double orderAmount) {
        List<String> errors = new ArrayList<>();

        if (coupon == null) {
            errors.add("Coupon inexistant");
            return errors;
        }

        if (!coupon.getIsActive()) {
            errors.add("Coupon désactivé");
        }

        if (coupon.isExpired()) {
            errors.add("Coupon expiré");
        }

        if (coupon.hasReachedMaxUses()) {
            errors.add("Coupon épuisé (utilisé " + coupon.getUsedCount() + "/" + coupon.getMaxUses() + " fois)");
        }

        if (orderAmount != null && coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
            errors.add("Montant minimum non atteint: " + coupon.getMinOrderAmount() + " DT");
        }

        return errors;
    }

    private Map<String, Object> simplifyOrder(CustomerOrder order) {
        if (order == null) return null;

        Map<String, Object> simplified = new HashMap<>();
        simplified.put("id", order.getId());
        simplified.put("orderNumber", order.getOrderNumber());
        simplified.put("customerId", order.getCustomerId());
        simplified.put("customerName", order.getCustomerName());
        simplified.put("customerFirstName", order.getCustomerFirstName());
        simplified.put("customerLastName", order.getCustomerLastName());
        simplified.put("customerPhone", order.getCustomerPhone());
        simplified.put("customerEmail", order.getCustomerEmail());
        simplified.put("deliveryAddress", order.getDeliveryAddress());
        simplified.put("governorate", order.getGovernorate());
        simplified.put("city", order.getCity());
        simplified.put("notes", order.getNotes());
        simplified.put("status", order.getStatus());
        simplified.put("paymentStatus", order.getPaymentStatus());
        simplified.put("subtotal", order.getSubtotal());
        simplified.put("shippingCost", order.getShippingCost());
        simplified.put("discountAmount", order.getDiscountAmount());
        simplified.put("totalAmount", order.getTotalAmount());
        simplified.put("paymentMethod", order.getPaymentMethod());
        simplified.put("shippingMethod", order.getShippingMethod());
        simplified.put("trackingNumber", order.getTrackingNumber());
        simplified.put("shippedAt", order.getShippedAt());
        simplified.put("appliedCouponCode", order.getAppliedCouponCode());
        simplified.put("orderDate", order.getOrderDate());
        simplified.put("createdAt", order.getCreatedAt());
        simplified.put("updatedAt", order.getUpdatedAt());

        return simplified;
    }

    private Map<String, Object> buildOrderResponse(CustomerOrder createdOrder, CustomerOrder originalOrder, Map<String, Object> couponResult) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Commande créée avec succès");
        response.put("orderNumber", createdOrder.getOrderNumber());
        response.put("orderId", createdOrder.getId());
        response.put("customerId", createdOrder.getCustomerId());
        response.put("totalAmount", createdOrder.getTotalAmount());
        response.put("order", simplifyOrder(createdOrder));

        if (originalOrder.getAppliedCouponCode() != null && !originalOrder.getAppliedCouponCode().isEmpty()) {
            response.put("couponCode", originalOrder.getAppliedCouponCode());

            if (couponResult.containsKey("success") && (boolean) couponResult.get("success")) {
                response.put("couponUtilise", true);
                response.put("couponAncienCompteur", couponResult.get("oldCount"));
                response.put("couponNouveauCompteur", couponResult.get("newCount"));

                if (couponResult.containsKey("remainingUses")) {
                    response.put("couponUtilisationsRestantes", couponResult.get("remainingUses"));
                }
                if (couponResult.containsKey("warning")) {
                    response.put("couponWarning", couponResult.get("warning"));
                }
                if (couponResult.containsKey("epuise") && (boolean) couponResult.get("epuise")) {
                    response.put("couponEpuise", true);
                }
            } else {
                response.put("couponUtilise", false);
                response.put("couponMessage", couponResult.get("message"));
            }
        }

        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}