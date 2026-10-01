package com.example.byomas.Service;

import com.example.byomas.Entity.CustomerOrder;
import com.example.byomas.Entity.OrderItem;
import com.example.byomas.Entity.Product;
import com.example.byomas.Repository.CustomerOrderRepository;
import com.example.byomas.Repository.OrderItemRepository;
import com.example.byomas.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class OrderService {

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // === CRÉATION DE COMMANDE ===
    public CustomerOrder createOrder(CustomerOrder order) {
        try {
            // 1. Validation des données
            validateOrderData(order);

            // 2. Validation du stock
            validateStock(order);

            // 3. Préparation de la commande
            prepareOrder(order);

            // 4. Calcul des totaux initiaux
            order.calculateTotals();

            // 5. Sauvegarde de la commande (sans les items)
            CustomerOrder savedOrder = customerOrderRepository.save(order);

            // 6. Création et sauvegarde des items
            List<OrderItem> savedItems = createAndSaveOrderItems(savedOrder, order.getOrderItems());

            // 7. Association des items à la commande
            savedOrder.setOrderItems(savedItems);

            // 8. Calcul final des totaux
            savedOrder.calculateTotals();

            // 9. Mise à jour du stock
            updateStockAndSales(savedOrder);

            // 10. Sauvegarde finale
            CustomerOrder finalOrder = customerOrderRepository.save(savedOrder);

            System.out.println("✅ Commande créée: " + finalOrder.getOrderNumber());
            System.out.println("📦 Total items: " + finalOrder.getOrderItems().size());
            System.out.println("💰 Total amount: " + finalOrder.getTotalAmount());
            System.out.println("👤 Customer ID: " + finalOrder.getCustomerId());

            return finalOrder;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la commande: " + e.getMessage(), e);
        }
    }

    private void validateOrderData(CustomerOrder order) {
        if (order.getCustomerFirstName() == null || order.getCustomerFirstName().trim().isEmpty()) {
            throw new RuntimeException("Le prénom du client est obligatoire");
        }
        if (order.getCustomerLastName() == null || order.getCustomerLastName().trim().isEmpty()) {
            throw new RuntimeException("Le nom du client est obligatoire");
        }
        if (order.getCustomerPhone() == null || order.getCustomerPhone().trim().isEmpty()) {
            throw new RuntimeException("Le téléphone du client est obligatoire");
        }
        if (order.getDeliveryAddress() == null || order.getDeliveryAddress().trim().isEmpty()) {
            throw new RuntimeException("L'adresse de livraison est obligatoire");
        }
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new RuntimeException("La commande doit contenir au moins un produit");
        }
    }

    private void prepareOrder(CustomerOrder order) {
        // Générer le numéro de commande
        if (order.getOrderNumber() == null) {
            order.setOrderNumber(generateOrderNumber());
        }

        // Dates par défaut
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Statuts par défaut
        if (order.getStatus() == null) {
            order.setStatus("PENDING");
        }
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus("PENDING");
        }
        if (order.getPaymentMethod() == null) {
            order.setPaymentMethod("CARD");
        }
        if (order.getShippingMethod() == null) {
            order.setShippingMethod("STANDARD");
        }

        // Initialiser les totaux
        order.setSubtotal(0.0);
        order.setShippingCost(order.getShippingCost() != null ? order.getShippingCost() : 0.0);
        order.setTaxAmount(order.getTaxAmount() != null ? order.getTaxAmount() : 0.0);
        order.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0);
        order.setTotalAmount(0.0);
    }

    private List<OrderItem> createAndSaveOrderItems(CustomerOrder savedOrder, List<OrderItem> originalItems) {
        List<OrderItem> orderItems = new ArrayList<>();

        if (originalItems != null) {
            for (OrderItem originalItem : originalItems) {
                OrderItem newItem = createOrderItemFromOriginal(originalItem, savedOrder);
                orderItems.add(newItem);
            }
        }

        // Sauvegarder tous les items
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        // Recalculer chaque item
        for (OrderItem item : savedItems) {
            item.calculateTotal();
            orderItemRepository.save(item);
        }

        return savedItems;
    }

    private OrderItem createOrderItemFromOriginal(OrderItem originalItem, CustomerOrder savedOrder) {
        OrderItem newItem = new OrderItem();

        // Associer la commande
        newItem.setOrder(savedOrder);

        // Informations produit
        newItem.setProductId(originalItem.getProductId());
        newItem.setProductName(originalItem.getProductName());
        newItem.setPrice(originalItem.getPrice() != null ? originalItem.getPrice() : 0.0);
        newItem.setQuantity(originalItem.getQuantity() != null ? originalItem.getQuantity() : 1);

        // Informations client
        newItem.setCustomerId(savedOrder.getCustomerId());
        newItem.setCustomerName(savedOrder.getCustomerFirstName() + " " + savedOrder.getCustomerLastName());
        newItem.setOrderDate(savedOrder.getOrderDate());

        // Métadonnées
        newItem.setProductCategory(originalItem.getProductCategory());
        newItem.setProductImage(originalItem.getProductImage());
        newItem.setProductSku(originalItem.getProductSku());

        // Valeurs par défaut
        newItem.setCurrency("TND");
        newItem.setStatus("PENDING");
        newItem.setDiscountApplied(0.0);
        newItem.setTaxAmount(0.0);
        newItem.setShippingCost(0.0);

        // Calcul initial
        newItem.calculateTotal();

        return newItem;
    }

    private String generateOrderNumber() {
        return "CMD" + System.currentTimeMillis();
    }

    // === MÉTHODES DE RÉCUPÉRATION ===
    public List<CustomerOrder> getAllOrders() {
        List<CustomerOrder> orders = customerOrderRepository.findAll();
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public Optional<CustomerOrder> getOrderById(Long id) {
        Optional<CustomerOrder> order = customerOrderRepository.findById(id);
        order.ifPresent(CustomerOrder::calculateTotals);
        return order;
    }

    public Optional<CustomerOrder> getOrderByNumber(String orderNumber) {
        Optional<CustomerOrder> order = customerOrderRepository.findByOrderNumber(orderNumber);
        order.ifPresent(CustomerOrder::calculateTotals);
        return order;
    }

    public List<CustomerOrder> getOrdersByStatus(String status) {
        List<CustomerOrder> orders = customerOrderRepository.findByStatus(status);
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public List<CustomerOrder> getRecentOrders() {
        List<CustomerOrder> orders = customerOrderRepository.findTop10ByOrderByOrderDateDesc();
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public List<CustomerOrder> getOrdersByCustomer(String email) {
        List<CustomerOrder> orders = customerOrderRepository.findByCustomerEmailOrderByOrderDateDesc(email);
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public List<CustomerOrder> getOrdersByCustomerId(Long customerId) {
        List<CustomerOrder> orders = customerOrderRepository.findByCustomerIdOrderByOrderDateDesc(customerId);
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    // === MÉTHODES DE MISE À JOUR ===
    public CustomerOrder updateOrder(CustomerOrder order) {
        if (order.getId() == null) {
            throw new RuntimeException("Impossible de mettre à jour une commande sans ID");
        }

        order.calculateTotals();
        order.setUpdatedAt(LocalDateTime.now());

        return customerOrderRepository.save(order);
    }

    public CustomerOrder updateOrderStatus(Long id, String status) {
        return customerOrderRepository.findById(id)
                .map(order -> {
                    String oldStatus = order.getStatus();
                    order.setStatus(status);
                    order.setUpdatedAt(LocalDateTime.now());

                    updateOrderTimestamps(order, status, oldStatus);

                    order.calculateTotals();
                    CustomerOrder updatedOrder = customerOrderRepository.save(order);

                    System.out.println("🔄 Statut commande " + order.getOrderNumber() + " : " + oldStatus + " -> " + status);

                    // Envoyer email selon le statut
                    sendStatusEmail(updatedOrder, status, oldStatus);

                    return updatedOrder;
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'id: " + id));
    }

    private void updateOrderTimestamps(CustomerOrder order, String newStatus, String oldStatus) {
        LocalDateTime now = LocalDateTime.now();

        if ("CONFIRMED".equals(newStatus) && !"CONFIRMED".equals(oldStatus)) {
            order.setConfirmedAt(now);
        } else if ("SHIPPED".equals(newStatus) && !"SHIPPED".equals(oldStatus)) {
            order.setShippedAt(now);
        } else if ("DELIVERED".equals(newStatus) && !"DELIVERED".equals(oldStatus)) {
            order.setDeliveredAt(now);
        } else if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(oldStatus)) {
            order.setCancelledAt(now);
            restoreStock(order);
        }
    }

    public CustomerOrder updatePaymentStatus(Long id, String paymentStatus) {
        return customerOrderRepository.findById(id)
                .map(order -> {
                    order.setPaymentStatus(paymentStatus);
                    order.setUpdatedAt(LocalDateTime.now());
                    order.calculateTotals();
                    return customerOrderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'id: " + id));
    }

    public CustomerOrder updatePaymentMethod(Long id, String paymentMethod) {
        return customerOrderRepository.findById(id)
                .map(order -> {
                    order.setPaymentMethod(paymentMethod);
                    order.setUpdatedAt(LocalDateTime.now());
                    return customerOrderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'id: " + id));
    }

    public CustomerOrder updateShippingMethod(Long id, String shippingMethod) {
        return customerOrderRepository.findById(id)
                .map(order -> {
                    order.setShippingMethod(shippingMethod);
                    order.setUpdatedAt(LocalDateTime.now());
                    return customerOrderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'id: " + id));
    }

    public CustomerOrder updateTrackingNumber(Long id, String trackingNumber) {
        return customerOrderRepository.findById(id)
                .map(order -> {
                    order.setTrackingNumber(trackingNumber);
                    order.setUpdatedAt(LocalDateTime.now());
                    return customerOrderRepository.save(order);
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée avec l'id: " + id));
    }

    // === GESTION DU STOCK ===
    private void validateStock(CustomerOrder order) {
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductId() != null) {
                    Optional<Product> productOpt = productRepository.findById(item.getProductId());
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        int requestedQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
                        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

                        if (availableStock < requestedQuantity) {
                            throw new RuntimeException(
                                    "Stock insuffisant pour " + product.getName() +
                                            ". Disponible: " + availableStock +
                                            ", Demandé: " + requestedQuantity
                            );
                        }
                    } else {
                        throw new RuntimeException("Produit non trouvé: " + item.getProductId());
                    }
                }
            }
        }
    }

    private void updateStockAndSales(CustomerOrder order) {
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductId() != null) {
                    Optional<Product> productOpt = productRepository.findById(item.getProductId());
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                        int newStock = currentStock - quantity;

                        product.setStockQuantity(Math.max(0, newStock));
                        productRepository.save(product);

                        productService.incrementSalesCount(product.getId(), quantity);

                        System.out.println("📦 Stock mis à jour " + product.getName() + ": " + currentStock + " -> " + newStock);
                    }
                }
            }
        }
    }

    private void restoreStock(CustomerOrder order) {
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProductId() != null) {
                    Optional<Product> productOpt = productRepository.findById(item.getProductId());
                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                        int newStock = currentStock + quantity;

                        product.setStockQuantity(newStock);
                        productRepository.save(product);

                        Integer currentSales = product.getSalesCount() != null ? product.getSalesCount() : 0;
                        product.setSalesCount(Math.max(0, currentSales - quantity));
                        productRepository.save(product);

                        System.out.println("🔄 Stock restauré " + product.getName() + ": " + currentStock + " -> " + newStock);
                    }
                }
            }
        }
    }

    // === MÉTHODES DE SUPPRESSION ===
    public void deleteOrder(Long id) {
        CustomerOrder order = customerOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + id));

        if (!"CANCELLED".equals(order.getStatus())) {
            restoreStock(order);
        }

        customerOrderRepository.deleteById(id);
        System.out.println("🗑️ Commande supprimée: " + id);
    }

    // === MÉTHODES DE RECHERCHE ===
    public List<CustomerOrder> searchOrdersByNumber(String orderNumber) {
        List<CustomerOrder> orders = customerOrderRepository.findByOrderNumberContainingIgnoreCase(orderNumber);
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public List<CustomerOrder> getActiveOrders() {
        List<CustomerOrder> orders = customerOrderRepository.findActiveOrders();
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    public List<CustomerOrder> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<CustomerOrder> orders = customerOrderRepository.findByOrderDateBetween(startDate, endDate);
        orders.forEach(CustomerOrder::calculateTotals);
        return orders;
    }

    // === STATISTIQUES ===
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", customerOrderRepository.count());
        stats.put("pendingOrders", customerOrderRepository.countByStatus("PENDING"));
        stats.put("confirmedOrders", customerOrderRepository.countByStatus("CONFIRMED"));
        stats.put("shippedOrders", customerOrderRepository.countByStatus("SHIPPED"));
        stats.put("deliveredOrders", customerOrderRepository.countByStatus("DELIVERED"));
        stats.put("cancelledOrders", customerOrderRepository.countByStatus("CANCELLED"));
        stats.put("totalRevenue", getTotalRevenue());

        long totalOrders = customerOrderRepository.count();
        double avgOrderValue = totalOrders > 0 ? getTotalRevenue() / totalOrders : 0.0;
        stats.put("averageOrderValue", avgOrderValue);

        return stats;
    }

    public double getTotalRevenue() {
        Double revenue = customerOrderRepository.getTotalRevenue();
        return revenue != null ? revenue : 0.0;
    }

    public double getRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = customerOrderRepository.getTotalRevenueBetweenDates(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    // === GESTION DES ITEMS ===
    public CustomerOrder addOrderItem(Long orderId, OrderItem item) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));

        // Validation stock
        if (item.getProductId() != null) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent() && productOpt.get().getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Stock insuffisant");
            }
        }

        item.setOrder(order);
        item.calculateTotal();
        OrderItem savedItem = orderItemRepository.save(item);

        order.getOrderItems().add(savedItem);
        order.calculateTotals();
        order.setUpdatedAt(LocalDateTime.now());

        updateStockForItem(savedItem);

        return customerOrderRepository.save(order);
    }

    private void updateStockForItem(OrderItem item) {
        if (item.getProductId() != null) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int newStock = product.getStockQuantity() - item.getQuantity();
                product.setStockQuantity(Math.max(0, newStock));
                productRepository.save(product);
                productService.incrementSalesCount(product.getId(), item.getQuantity());
            }
        }
    }

    // === MÉTHODES DE WORKFLOW ===
    public CustomerOrder confirmOrder(Long orderId) {
        CustomerOrder order = updateOrderStatus(orderId, "CONFIRMED");

        // Envoyer email de confirmation
        sendConfirmationEmail(order);

        return order;
    }

    public CustomerOrder shipOrder(Long orderId, String trackingNumber) {
        return customerOrderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus("SHIPPED");
                    order.setTrackingNumber(trackingNumber);
                    order.setShippedAt(LocalDateTime.now());
                    order.setUpdatedAt(LocalDateTime.now());

                    if (order.getShippingMethod() == null) {
                        order.setShippingMethod("STANDARD");
                    }

                    order.calculateTotals();
                    CustomerOrder updatedOrder = customerOrderRepository.save(order);

                    // Envoyer email d'expédition
                    sendShippingEmail(updatedOrder, trackingNumber);

                    return updatedOrder;
                })
                .orElseThrow(() -> new RuntimeException("Commande non trouvée: " + orderId));
    }

    public CustomerOrder deliverOrder(Long orderId) {
        CustomerOrder order = updateOrderStatus(orderId, "DELIVERED");

        // Envoyer email de livraison
        sendDeliveryEmail(order);

        return order;
    }

    public CustomerOrder cancelOrder(Long orderId) {
        CustomerOrder order = updateOrderStatus(orderId, "CANCELLED");

        // Envoyer email d'annulation
        sendCancellationEmail(order);

        return order;
    }

    // === MÉTHODES D'ENVOI D'EMAILS ===

    private void sendStatusEmail(CustomerOrder order, String newStatus, String oldStatus) {
        try {
            switch (newStatus) {
                case "CONFIRMED":
                    sendConfirmationEmail(order);
                    break;
                case "SHIPPED":
                    if (order.getTrackingNumber() != null) {
                        sendShippingEmail(order, order.getTrackingNumber());
                    }
                    break;
                case "DELIVERED":
                    sendDeliveryEmail(order);
                    break;
                case "CANCELLED":
                    sendCancellationEmail(order);
                    break;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Échec envoi email pour statut " + newStatus + ": " + e.getMessage());
        }
    }

    private void sendConfirmationEmail(CustomerOrder order) {
        try {
            // Construire les détails des produits
            StringBuilder productsDetails = new StringBuilder();
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    productsDetails.append("- ")
                            .append(item.getProductName())
                            .append(" (x")
                            .append(item.getQuantity())
                            .append("): ")
                            .append(item.getTotal())
                            .append(" TND\n");
                }
            }

            String emailContent = "Bonjour " + order.getCustomerFirstName() + " " + order.getCustomerLastName() + ",\n\n" +
                    "Votre commande #" + order.getOrderNumber() + " a été confirmée avec succès.\n\n" +
                    "📦 **Détails de la commande:**\n" +
                    "Numéro de commande: " + order.getOrderNumber() + "\n" +
                    "Date: " + order.getOrderDate().format(DATE_FORMATTER) + "\n" +
                    "Statut: Confirmée\n\n" +
                    "🛒 **Produits commandés:**\n" + productsDetails.toString() + "\n" +
                    "💰 **Récapitulatif:**\n" +
                    "Sous-total: " + order.getSubtotal() + " TND\n" +
                    "Frais de livraison: " + order.getShippingCost() + " TND\n" +
                    "Total: " + order.getTotalAmount() + " TND\n\n" +
                    "📍 **Adresse de livraison:**\n" +
                    order.getDeliveryAddress() + "\n\n" +
                    "📞 **Vos informations:**\n" +
                    "Téléphone: " + order.getCustomerPhone() + "\n" +
                    "Email: " + order.getCustomerEmail() + "\n\n" +
                    "Nous traiterons votre commande dans les plus brefs délais.\n\n" +
                    "Merci pour votre confiance !\n" +
                    "L'équipe Byomas";

            emailService.sendSimpleEmail(
                    order.getCustomerEmail(),
                    "Confirmation de votre commande #" + order.getOrderNumber(),
                    emailContent
            );

            System.out.println("✅ Email de confirmation envoyé à: " + order.getCustomerEmail());

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email confirmation: " + e.getMessage());
        }
    }

    private void sendShippingEmail(CustomerOrder order, String trackingNumber) {
        try {
            String emailContent = "Bonjour " + order.getCustomerFirstName() + " " + order.getCustomerLastName() + ",\n\n" +
                    "Bonne nouvelle ! Votre commande #" + order.getOrderNumber() + " a été expédiée.\n\n" +
                    "📦 **Informations d'expédition:**\n" +
                    "Numéro de commande: " + order.getOrderNumber() + "\n" +
                    "Numéro de suivi: " + trackingNumber + "\n" +
                    "Date d'expédition: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n" +
                    "Méthode de livraison: " + order.getShippingMethod() + "\n\n" +
                    "📍 **Adresse de livraison:**\n" +
                    order.getDeliveryAddress() + "\n\n" +
                    "Vous pouvez suivre votre colis avec le numéro de suivi ci-dessus.\n\n" +
                    "Livraison estimée: 2-3 jours ouvrables\n\n" +
                    "Merci pour votre patience !\n" +
                    "L'équipe Byomas";

            emailService.sendSimpleEmail(
                    order.getCustomerEmail(),
                    "Votre commande #" + order.getOrderNumber() + " a été expédiée",
                    emailContent
            );

            System.out.println("✅ Email d'expédition envoyé à: " + order.getCustomerEmail() +
                    " - Tracking: " + trackingNumber);

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email expédition: " + e.getMessage());
        }
    }

    private void sendDeliveryEmail(CustomerOrder order) {
        try {
            String emailContent = "Bonjour " + order.getCustomerFirstName() + " " + order.getCustomerLastName() + ",\n\n" +
                    "Excellent ! Votre commande #" + order.getOrderNumber() + " a été livrée avec succès.\n\n" +
                    "✅ **Livraison confirmée**\n" +
                    "Numéro de commande: " + order.getOrderNumber() + "\n" +
                    "Date de livraison: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n" +
                    "Montant total: " + order.getTotalAmount() + " TND\n\n" +
                    "Nous espérons que vous serez satisfait(e) de votre achat.\n\n" +
                    "Avez-vous besoin d'aide ou avez-vous des questions concernant votre commande ?\n" +
                    "N'hésitez pas à nous contacter.\n\n" +
                    "Merci pour votre confiance !\n" +
                    "L'équipe Byomas";

            emailService.sendSimpleEmail(
                    order.getCustomerEmail(),
                    "Votre commande #" + order.getOrderNumber() + " a été livrée",
                    emailContent
            );

            System.out.println("✅ Email de livraison envoyé à: " + order.getCustomerEmail());

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email livraison: " + e.getMessage());
        }
    }

    private void sendCancellationEmail(CustomerOrder order) {
        try {
            String emailContent = "Bonjour " + order.getCustomerFirstName() + " " + order.getCustomerLastName() + ",\n\n" +
                    "Nous vous informons que votre commande #" + order.getOrderNumber() + " a été annulée.\n\n" +
                    "❌ **Détails de l'annulation:**\n" +
                    "Numéro de commande: " + order.getOrderNumber() + "\n" +
                    "Date d'annulation: " + LocalDateTime.now().format(DATE_FORMATTER) + "\n" +
                    "Montant remboursé: " + order.getTotalAmount() + " TND\n\n" +
                    "Raison: Annulée par l'administrateur\n\n" +
                    "Si vous avez des questions concernant cette annulation,\n" +
                    "n'hésitez pas à nous contacter.\n\n" +
                    "Nous espérons vous revoir bientôt !\n" +
                    "L'équipe Byomas";

            emailService.sendSimpleEmail(
                    order.getCustomerEmail(),
                    "Annulation de votre commande #" + order.getOrderNumber(),
                    emailContent
            );

            System.out.println("✅ Email d'annulation envoyé à: " + order.getCustomerEmail());

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email annulation: " + e.getMessage());
        }
    }

    public Double calculateAverageDeliveryTime() {
        List<Object[]> timestamps = customerOrderRepository.getDeliveryTimestamps();

        if (timestamps.isEmpty()) {
            return 0.0;
        }

        long totalHours = 0;
        int count = 0;

        for (Object[] timestamp : timestamps) {
            LocalDateTime shippedAt = (LocalDateTime) timestamp[0];
            LocalDateTime deliveredAt = (LocalDateTime) timestamp[1];

            if (shippedAt != null && deliveredAt != null) {
                long hours = java.time.Duration.between(shippedAt, deliveredAt).toHours();
                totalHours += hours;
                count++;
            }
        }

        return count > 0 ? (double) totalHours / count : 0.0;
    }
    // Dans OrderService.java
    public List<CustomerOrder> getOrdersByCouponCode(String couponCode) {
        return customerOrderRepository.findByAppliedCouponCode(couponCode);
    }
}