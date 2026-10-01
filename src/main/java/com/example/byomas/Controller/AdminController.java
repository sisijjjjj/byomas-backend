package com.example.byomas.Controller;

import com.example.byomas.Entity.*;
import com.example.byomas.Repository.*;
import com.example.byomas.Service.EmailService;
import com.example.byomas.Service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminUserRepository adminUserRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private CustomerOrderRepository customerOrderRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private OrderService orderService;

    private Map<String, Object> dashboardStats = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Répertoire de stockage des uploads
    private final String UPLOAD_DIR = "uploads/";

    // ==================== UPLOAD D'IMAGES ====================

    @PostMapping("/upload/image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam("uploadType") String uploadType) {

        try {
            System.out.println("=== UPLOAD D'IMAGE ===");
            System.out.println("Nom fichier: " + file.getOriginalFilename());
            System.out.println("Type: " + file.getContentType());
            System.out.println("Taille: " + file.getSize());
            System.out.println("Upload type: " + uploadType);

            // Vérifications de sécurité
            if (file.isEmpty()) {
                return createErrorResponse("Fichier vide", HttpStatus.BAD_REQUEST);
            }

            // Vérification du type de fichier
            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList(
                    "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
            );

            if (!allowedTypes.contains(contentType)) {
                return createErrorResponse("Type de fichier non supporté: " + contentType, HttpStatus.BAD_REQUEST);
            }

            // Vérification de la taille (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return createErrorResponse("Fichier trop volumineux (max 5MB)", HttpStatus.BAD_REQUEST);
            }

            // Générer un nom de fichier unique
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Chemin de sauvegarde
            String uploadSubDir = UPLOAD_DIR + uploadType + "/";
            Path uploadPath = Paths.get(uploadSubDir);

            // Créer le dossier s'il n'existe pas
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Sauvegarder le fichier
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // URL d'accès à l'image
            String imageUrl = "/" + uploadSubDir + fileName;

            System.out.println("✅ Image sauvegardée: " + filePath.toString());
            System.out.println("📷 URL image: " + imageUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploadée avec succès");
            response.put("imageUrl", imageUrl);
            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            response.put("contentType", contentType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'upload: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de l'upload: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return ".jpg";
        int lastDot = fileName.lastIndexOf(".");
        return lastDot > 0 ? fileName.substring(lastDot).toLowerCase() : ".jpg";
    }

    // ==================== AUTHENTIFICATION ====================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                return createErrorResponse("Données manquantes", HttpStatus.BAD_REQUEST);
            }

            Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                AdminUser user = userOpt.get();

                if (!user.getIsActive()) {
                    return createErrorResponse("Compte désactivé", HttpStatus.UNAUTHORIZED);
                }

                if (user.getPassword().equals(password)) {
                    String token = "byomas-admin-token-" + System.currentTimeMillis();

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Connexion réussie");
                    response.put("token", token);
                    response.put("user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole()
                    ));

                    return ResponseEntity.ok(response);
                }
            }

            return createErrorResponse("Identifiants invalides", HttpStatus.UNAUTHORIZED);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'authentification", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/verify-token")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Mode sans authentification activé");
        response.put("valid", true);
        response.put("user", Map.of(
                "username", "admin",
                "role", "SUPER_ADMIN",
                "authenticated", true
        ));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Déconnexion réussie");
        return ResponseEntity.ok(response);
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        try {
            updateDashboardStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dashboard", dashboardStats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors du chargement du dashboard: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard/refresh")
    public ResponseEntity<Map<String, Object>> refreshDashboard() {
        try {
            updateDashboardStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dashboard actualisé");
            response.put("dashboard", dashboardStats);
            response.put("refreshedAt", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'actualisation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            updateDashboardStats();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", dashboardStats);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des statistiques: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void updateDashboardStats() {
        try {
            // Produits
            long totalProducts = productRepository.count();
            long lowStockProducts = productRepository.countByStockQuantityLessThan(10);
            long outOfStockProducts = productRepository.countByStockQuantity(0);
            long activeProducts = productRepository.countByIsActiveTrue();

            // Commandes
            Map<String, Object> orderStats = orderService.getOrderStatistics();
            long totalOrders = (Long) orderStats.get("totalOrders");
            long pendingOrders = (Long) orderStats.get("pendingOrders");
            long confirmedOrders = (Long) orderStats.get("confirmedOrders");
            long shippedOrders = (Long) orderStats.get("shippedOrders");
            long deliveredOrders = (Long) orderStats.get("deliveredOrders");
            long cancelledOrders = (Long) orderStats.get("cancelledOrders");
            double totalRevenue = (Double) orderStats.get("totalRevenue");
            double averageOrderValue = (Double) orderStats.get("averageOrderValue");

            // Clients
            long totalCustomers = customerRepository.count();
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            long newCustomersThisMonth = customerRepository.countByRegistrationDateBetween(startOfMonth, LocalDateTime.now());

            // Clients avec commandes
            long customersWithOrders = customerOrderRepository.countDistinctCustomersWithOrders();

            // Messages
            long totalMessages = messageRepository.count();
            long unreadMessages = messageRepository.countByIsReadFalse();
            long urgentMessages = messageRepository.countByPriority("URGENT");

            // Catégories
            long totalCategories = categoryRepository.count();

            // ✅ COUPONS - Statistiques améliorées
            long activeCoupons = couponRepository.countByIsActiveTrue();
            long totalCoupons = couponRepository.count();
            long expiredCoupons = couponRepository.findExpiredCoupons().size();
            long fullyUsedCoupons = couponRepository.findFullyUsedCoupons().size();
            Double averageUsageRate = couponRepository.getAverageUsageRate();

            // Revenus du mois
            double revenueThisMonth = orderService.getRevenueBetweenDates(startOfMonth, LocalDateTime.now());
            Long ordersThisMonth = customerOrderRepository.countOrdersBetweenDates(startOfMonth, LocalDateTime.now());

            // Statistiques avancées
            List<Object[]> monthlyStats = customerOrderRepository.getMonthlyStats(
                    LocalDateTime.now().minusMonths(6),
                    LocalDateTime.now()
            );

            // Top produits
            List<Product> topProductsList = productRepository.findTop5ByOrderBySalesCountDesc();
            List<Object[]> topProducts = topProductsList.stream()
                    .map(p -> new Object[]{
                            p.getName() != null ? p.getName() : "Sans nom",
                            p.getSalesCount() != null ? p.getSalesCount() : 0,
                            p.getPrice() != null ? p.getPrice() : 0.0
                    })
                    .collect(Collectors.toList());

            // ✅ Top coupons
            List<Coupon> mostUsedCoupons = couponRepository.findMostUsedCoupons().stream()
                    .limit(5)
                    .collect(Collectors.toList());

            dashboardStats.put("totalProducts", totalProducts);
            dashboardStats.put("activeProducts", activeProducts);
            dashboardStats.put("lowStockProducts", lowStockProducts);
            dashboardStats.put("outOfStockProducts", outOfStockProducts);
            dashboardStats.put("totalOrders", totalOrders);
            dashboardStats.put("totalCustomers", totalCustomers);
            dashboardStats.put("customersWithOrders", customersWithOrders);
            dashboardStats.put("customersWithoutOrders", totalCustomers - customersWithOrders);
            dashboardStats.put("customerOrderRate", totalCustomers > 0 ?
                    (double) customersWithOrders / totalCustomers * 100 : 0);
            dashboardStats.put("pendingOrders", pendingOrders);
            dashboardStats.put("confirmedOrders", confirmedOrders);
            dashboardStats.put("shippedOrders", shippedOrders);
            dashboardStats.put("deliveredOrders", deliveredOrders);
            dashboardStats.put("cancelledOrders", cancelledOrders);
            dashboardStats.put("totalRevenue", totalRevenue);
            dashboardStats.put("averageOrderValue", averageOrderValue);
            dashboardStats.put("ordersThisMonth", ordersThisMonth != null ? ordersThisMonth : 0);
            dashboardStats.put("revenueThisMonth", revenueThisMonth);
            dashboardStats.put("newCustomersThisMonth", newCustomersThisMonth);
            dashboardStats.put("unreadMessages", unreadMessages);
            dashboardStats.put("urgentMessages", urgentMessages);
            dashboardStats.put("totalMessages", totalMessages);
            dashboardStats.put("totalCategories", totalCategories);

            // ✅ Statistiques coupons améliorées
            dashboardStats.put("activeCoupons", activeCoupons);
            dashboardStats.put("totalCoupons", totalCoupons);
            dashboardStats.put("expiredCoupons", expiredCoupons);
            dashboardStats.put("fullyUsedCoupons", fullyUsedCoupons);
            dashboardStats.put("averageCouponUsageRate", averageUsageRate != null ? averageUsageRate : 0);
            dashboardStats.put("mostUsedCoupons", mostUsedCoupons);

            dashboardStats.put("monthlyStats", monthlyStats);
            dashboardStats.put("topProducts", topProducts);

        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du dashboard: " + e.getMessage());
            initializeDefaultStats();
        }
    }

    private void initializeDefaultStats() {
        dashboardStats.put("totalProducts", 0);
        dashboardStats.put("activeProducts", 0);
        dashboardStats.put("lowStockProducts", 0);
        dashboardStats.put("outOfStockProducts", 0);
        dashboardStats.put("totalOrders", 0);
        dashboardStats.put("totalCustomers", 0);
        dashboardStats.put("customersWithOrders", 0);
        dashboardStats.put("customersWithoutOrders", 0);
        dashboardStats.put("customerOrderRate", 0.0);
        dashboardStats.put("pendingOrders", 0);
        dashboardStats.put("confirmedOrders", 0);
        dashboardStats.put("shippedOrders", 0);
        dashboardStats.put("deliveredOrders", 0);
        dashboardStats.put("cancelledOrders", 0);
        dashboardStats.put("totalRevenue", 0.0);
        dashboardStats.put("averageOrderValue", 0.0);
        dashboardStats.put("ordersThisMonth", 0);
        dashboardStats.put("revenueThisMonth", 0.0);
        dashboardStats.put("newCustomersThisMonth", 0);
        dashboardStats.put("unreadMessages", 0);
        dashboardStats.put("urgentMessages", 0);
        dashboardStats.put("totalMessages", 0);
        dashboardStats.put("totalCategories", 0);
        dashboardStats.put("activeCoupons", 0);
        dashboardStats.put("totalCoupons", 0);
        dashboardStats.put("expiredCoupons", 0);
        dashboardStats.put("fullyUsedCoupons", 0);
        dashboardStats.put("averageCouponUsageRate", 0.0);
        dashboardStats.put("mostUsedCoupons", new ArrayList<>());
        dashboardStats.put("monthlyStats", new ArrayList<>());
        dashboardStats.put("topProducts", new ArrayList<>());
    }

    // ==================== GESTION DES MESSAGES ====================

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getAllMessages() {
        try {
            List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();
            List<Map<String, Object>> detailedMessages = messages.stream()
                    .map(this::createMessageDetails)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", detailedMessages);
            response.put("count", messages.size());
            response.put("unreadCount", messageRepository.countByIsReadFalse());
            response.put("urgentCount", messageRepository.countByPriority("URGENT"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des messages: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/messages/unread")
    public ResponseEntity<Map<String, Object>> getUnreadMessages() {
        try {
            List<Message> messages = messageRepository.findByIsReadFalseOrderByCreatedAtDesc();
            List<Map<String, Object>> detailedMessages = messages.stream()
                    .map(this::createMessageDetails)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", detailedMessages);
            response.put("count", messages.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des messages non lus: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/messages/urgent")
    public ResponseEntity<Map<String, Object>> getUrgentMessages() {
        try {
            List<Message> messages = messageRepository.findByPriorityOrderByCreatedAtDesc("URGENT");
            List<Map<String, Object>> detailedMessages = messages.stream()
                    .map(this::createMessageDetails)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", detailedMessages);
            response.put("count", messages.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des messages urgents: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> getMessageById(@PathVariable Long id) {
        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();

                if (!message.getIsRead()) {
                    message.setIsRead(true);
                    message.setStatus("READ");
                    messageRepository.save(message);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", createMessageDetails(message));
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/messages/{id}/read")
    public ResponseEntity<Map<String, Object>> markMessageAsRead(@PathVariable Long id) {
        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                message.setIsRead(true);
                message.setStatus("READ");
                message.setUpdatedAt(LocalDateTime.now());
                messageRepository.save(message);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Message marqué comme lu");
                response.put("messageData", createMessageDetails(message));

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors du marquage du message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/messages/{id}/unread")
    public ResponseEntity<Map<String, Object>> markMessageAsUnread(@PathVariable Long id) {
        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                message.setIsRead(false);
                message.setStatus("NEW");
                message.setUpdatedAt(LocalDateTime.now());
                messageRepository.save(message);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Message marqué comme non lu");
                response.put("messageData", createMessageDetails(message));

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors du marquage du message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/messages/{id}/respond")
    public ResponseEntity<Map<String, Object>> respondToMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> responseData) {

        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                String responseText = responseData.get("response");
                String adminNotes = responseData.get("adminNotes");

                if (responseText == null || responseText.trim().isEmpty()) {
                    return createErrorResponse("Le texte de réponse est requis", HttpStatus.BAD_REQUEST);
                }

                message.setAdminResponse(responseText);
                if (adminNotes != null) {
                    message.setAdminNotes(adminNotes);
                }
                message.setIsReplied(true);
                message.setStatus("REPLIED");
                message.setRepliedAt(LocalDateTime.now());
                message.setUpdatedAt(LocalDateTime.now());

                Message savedMessage = messageRepository.save(message);

                try {
                    emailService.sendMessageResponse(
                            message.getEmail(),
                            message.getName(),
                            message.getSubject(),
                            responseText
                    );
                } catch (Exception emailException) {
                    System.out.println("Email non envoyé: " + emailException.getMessage());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Réponse envoyée avec succès");
                response.put("messageData", createMessageDetails(savedMessage));

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'envoi de la réponse: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/messages/{id}/priority")
    public ResponseEntity<Map<String, Object>> updateMessagePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> priorityData) {

        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                String priority = priorityData.get("priority");

                if (priority != null && Arrays.asList("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority)) {
                    message.setPriority(priority);
                    message.setUpdatedAt(LocalDateTime.now());
                    messageRepository.save(message);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Priorité mise à jour: " + priority);
                    response.put("messageData", createMessageDetails(message));

                    return ResponseEntity.ok(response);
                } else {
                    return createErrorResponse("Priorité invalide", HttpStatus.BAD_REQUEST);
                }
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour de la priorité: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                messageRepository.deleteById(id);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Message supprimé avec succès");

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression du message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> createMessageDetails(Message message) {
        Map<String, Object> messageDetails = new HashMap<>();
        messageDetails.put("id", message.getId());
        messageDetails.put("name", message.getName());
        messageDetails.put("email", message.getEmail());
        messageDetails.put("phone", message.getPhone());
        messageDetails.put("company", message.getCompany());
        messageDetails.put("subject", message.getSubject());
        messageDetails.put("message", message.getMessage());
        messageDetails.put("adminResponse", message.getAdminResponse());
        messageDetails.put("adminNotes", message.getAdminNotes());
        messageDetails.put("status", message.getStatus());
        messageDetails.put("priority", message.getPriority());
        messageDetails.put("isRead", message.getIsRead());
        messageDetails.put("isReplied", message.getIsReplied());
        messageDetails.put("createdAt", message.getCreatedAt());
        messageDetails.put("updatedAt", message.getUpdatedAt());
        messageDetails.put("repliedAt", message.getRepliedAt());
        return messageDetails;
    }

    // ==================== GESTION DES COMMANDES ====================

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrders() {
        try {
            List<CustomerOrder> orders = orderService.getAllOrders();
            List<Map<String, Object>> orderDTOs = createSafeOrderDTOs(orders);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orderDTOs);
            response.put("count", orders.size());
            response.put("totalRevenue", orderService.getTotalRevenue());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Erreur dans /orders: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la récupération des commandes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/recent")
    public ResponseEntity<Map<String, Object>> getRecentOrders() {
        try {
            List<CustomerOrder> orders = orderService.getRecentOrders();
            List<Map<String, Object>> orderDTOs = createSafeOrderDTOs(orders);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orderDTOs);
            response.put("count", orders.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des commandes récentes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/status/{status}")
    public ResponseEntity<Map<String, Object>> getOrdersByStatus(@PathVariable String status) {
        try {
            List<CustomerOrder> orders = orderService.getOrdersByStatus(status);
            List<Map<String, Object>> orderDTOs = createSafeOrderDTOs(orders);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orderDTOs);
            response.put("count", orders.size());
            response.put("status", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des commandes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrderById(@PathVariable Long id) {
        try {
            Optional<CustomerOrder> orderOpt = orderService.getOrderById(id);

            if (orderOpt.isPresent()) {
                CustomerOrder order = orderOpt.get();
                Map<String, Object> orderData = createSafeOrderDTO(order);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("order", orderData);
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Commande non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération de la commande: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {

        try {
            String newStatus = statusUpdate.get("status");

            if (newStatus == null || newStatus.trim().isEmpty()) {
                return createErrorResponse("Le statut est requis", HttpStatus.BAD_REQUEST);
            }

            CustomerOrder updatedOrder = orderService.updateOrderStatus(id, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Statut de commande mis à jour: " + newStatus);
            response.put("order", createSafeOrderDTO(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour du statut: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/{id}/tracking")
    public ResponseEntity<Map<String, Object>> updateTrackingNumber(
            @PathVariable Long id,
            @RequestBody Map<String, String> trackingUpdate) {

        try {
            String trackingNumber = trackingUpdate.get("trackingNumber");

            if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                return createErrorResponse("Le numéro de suivi est requis", HttpStatus.BAD_REQUEST);
            }

            CustomerOrder updatedOrder = orderService.updateTrackingNumber(id, trackingNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Numéro de suivi mis à jour");
            response.put("order", createSafeOrderDTO(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour du suivi: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/{id}/ship")
    public ResponseEntity<Map<String, Object>> shipOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> shippingData) {

        try {
            String trackingNumber = shippingData.get("trackingNumber");

            if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                return createErrorResponse("Le numéro de suivi est requis", HttpStatus.BAD_REQUEST);
            }

            CustomerOrder updatedOrder = orderService.shipOrder(id, trackingNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande expédiée avec succès");
            response.put("order", createSafeOrderDTO(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'expédition: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/{id}/deliver")
    public ResponseEntity<Map<String, Object>> deliverOrder(@PathVariable Long id) {
        try {
            CustomerOrder updatedOrder = orderService.deliverOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande livrée avec succès");
            response.put("order", createSafeOrderDTO(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la livraison: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        try {
            CustomerOrder updatedOrder = orderService.cancelOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande annulée avec succès");
            response.put("order", createSafeOrderDTO(updatedOrder));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'annulation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Confirme une commande et incrémente automatiquement le compteur du coupon utilisé
     */
    @PutMapping("/orders/{id}/confirm")
    @Transactional
    public ResponseEntity<Map<String, Object>> confirmOrder(@PathVariable Long id) {
        try {
            // Vérifier si la commande existe et n'est pas déjà confirmée
            Optional<CustomerOrder> existingOrderOpt = customerOrderRepository.findById(id);

            if (!existingOrderOpt.isPresent()) {
                return createErrorResponse("Commande non trouvée", HttpStatus.NOT_FOUND);
            }

            CustomerOrder existingOrder = existingOrderOpt.get();

            // Vérifier si la commande est déjà confirmée pour éviter la double incrémentation
            if ("CONFIRMED".equals(existingOrder.getStatus())) {
                return createErrorResponse("Cette commande est déjà confirmée", HttpStatus.BAD_REQUEST);
            }

            // Confirmer la commande via le service
            CustomerOrder updatedOrder = orderService.confirmOrder(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande confirmée avec succès");
            response.put("order", createSafeOrderDTO(updatedOrder));
            response.put("emailSent", true);
            response.put("emailRecipient", updatedOrder.getCustomerEmail());

            // ✅ INCRÉMENTATION AUTO DU COUPON SI UTILISÉ
            if (updatedOrder.getAppliedCouponCode() != null && !updatedOrder.getAppliedCouponCode().isEmpty()) {
                String couponCode = updatedOrder.getAppliedCouponCode();

                // Incrémenter le compteur du coupon
                Map<String, Object> couponResult = incrementCouponUsage(couponCode, id, updatedOrder.getTotalAmount());

                // Ajouter les infos du coupon dans la réponse
                response.put("couponUtilise", couponResult.get("success"));
                response.put("couponCode", couponCode);

                if ((boolean) couponResult.get("success")) {
                    response.put("couponAncienCompteur", couponResult.get("oldCount"));
                    response.put("couponNouveauCompteur", couponResult.get("newCount"));

                    if (couponResult.containsKey("warning")) {
                        response.put("couponWarning", couponResult.get("warning"));
                    }

                    if (couponResult.containsKey("epuise") && (boolean) couponResult.get("epuise")) {
                        response.put("couponEpuise", true);
                        response.put("messageCoupon", "Ce coupon a atteint son nombre maximum d'utilisations");
                    }
                } else {
                    response.put("couponWarning", couponResult.get("message"));
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la confirmation de la commande " + id + ": " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la confirmation: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Méthode utilitaire pour incrémenter le compteur d'un coupon
     */
    private Map<String, Object> incrementCouponUsage(String couponCode, Long orderId, Double orderAmount) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);

        try {
            Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);

            if (!couponOpt.isPresent()) {
                result.put("message", "Coupon non trouvé: " + couponCode);
                System.out.println("⚠️ Coupon non trouvé: " + couponCode);
                return result;
            }

            Coupon coupon = couponOpt.get();

            // Vérifier si le coupon peut être incrémenté
            if (!coupon.getIsActive()) {
                result.put("message", "Coupon désactivé");
                System.out.println("⚠️ Tentative d'utilisation d'un coupon désactivé: " + couponCode);
                return result;
            }

            if (coupon.isExpired()) {
                result.put("message", "Coupon expiré depuis le " +
                        coupon.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                System.out.println("⚠️ Tentative d'utilisation d'un coupon expiré: " + couponCode);
                return result;
            }

            if (coupon.hasReachedMaxUses()) {
                result.put("message", "Coupon a atteint sa limite d'utilisations");
                result.put("epuise", true);
                System.out.println("⚠️ Tentative d'utilisation d'un coupon épuisé: " + couponCode);
                return result;
            }

            // Vérifier le montant minimum de commande si applicable
            if (orderAmount != null && coupon.getMinOrderAmount() != null &&
                    orderAmount < coupon.getMinOrderAmount()) {
                result.put("message", "Montant minimum de commande non atteint");
                System.out.println("⚠️ Montant minimum non atteint pour le coupon: " + couponCode);
                return result;
            }

            // ✅ INCRÉMENTER LE COMPTEUR
            int oldCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            int newCount = oldCount + 1;

            coupon.setUsedCount(newCount);
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon updatedCoupon = couponRepository.save(coupon);

            // Journaliser l'utilisation
            logCouponUsage(updatedCoupon, "ORDER-" + orderId, null,
                    orderAmount != null ? orderAmount.toString() : null);

            System.out.println("✅ COUPON INCRÉMENTÉ AUTOMATIQUEMENT:");
            System.out.println("   - Commande ID: " + orderId);
            System.out.println("   - Code: " + couponCode);
            System.out.println("   - Ancien compteur: " + oldCount);
            System.out.println("   - Nouveau compteur: " + newCount);

            result.put("success", true);
            result.put("oldCount", oldCount);
            result.put("newCount", newCount);

            // Vérifier si le coupon est maintenant épuisé
            if (coupon.getMaxUses() != null && newCount >= coupon.getMaxUses()) {
                result.put("epuise", true);
                result.put("warning", "Ce coupon a atteint son nombre maximum d'utilisations");
            } else if (coupon.getMaxUses() != null) {
                int remaining = coupon.getMaxUses() - newCount;
                if (remaining <= 3) {
                    result.put("warning", "Attention: Il ne reste que " + remaining + " utilisation(s) pour ce coupon");
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'incrémentation du coupon " + couponCode + ": " + e.getMessage());
            result.put("message", "Erreur technique: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> createSafeOrderDTO(CustomerOrder order) {
        Map<String, Object> orderDTO = new HashMap<>();

        orderDTO.put("id", order.getId());
        orderDTO.put("orderNumber", order.getOrderNumber());
        orderDTO.put("customerId", order.getCustomerId());
        orderDTO.put("customerFirstName", order.getCustomerFirstName());
        orderDTO.put("customerLastName", order.getCustomerLastName());
        orderDTO.put("customerEmail", order.getCustomerEmail());
        orderDTO.put("customerPhone", order.getCustomerPhone());
        orderDTO.put("deliveryAddress", order.getDeliveryAddress());
        orderDTO.put("orderDate", order.getOrderDate());
        orderDTO.put("status", order.getStatus());
        orderDTO.put("paymentStatus", order.getPaymentStatus());
        orderDTO.put("paymentMethod", order.getPaymentMethod());
        orderDTO.put("shippingMethod", order.getShippingMethod());
        orderDTO.put("trackingNumber", order.getTrackingNumber());
        orderDTO.put("subtotal", order.getSubtotal());
        orderDTO.put("shippingCost", order.getShippingCost());
        orderDTO.put("taxAmount", order.getTaxAmount());
        orderDTO.put("discountAmount", order.getDiscountAmount());
        orderDTO.put("totalAmount", order.getTotalAmount());
        orderDTO.put("notes", order.getNotes());
        orderDTO.put("createdAt", order.getCreatedAt());
        orderDTO.put("updatedAt", order.getUpdatedAt());
        orderDTO.put("customerName", order.getCustomerName());

        // ✅ Ajout du coupon utilisé
        orderDTO.put("appliedCouponCode", order.getAppliedCouponCode());

        // Gérer les orderItems
        List<Map<String, Object>> orderItemsDTO = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item != null) {
                    Map<String, Object> itemDTO = new HashMap<>();
                    itemDTO.put("id", item.getId());
                    itemDTO.put("productId", item.getProductId());
                    itemDTO.put("productName", item.getProductName());
                    itemDTO.put("productSku", item.getProductSku());
                    itemDTO.put("productCategory", item.getProductCategory());
                    itemDTO.put("price", item.getPrice());
                    itemDTO.put("quantity", item.getQuantity());
                    itemDTO.put("total", item.getTotal());
                    orderItemsDTO.add(itemDTO);
                }
            }
        }
        orderDTO.put("orderItems", orderItemsDTO);
        orderDTO.put("itemsCount", orderItemsDTO.size());

        return orderDTO;
    }

    private List<Map<String, Object>> createSafeOrderDTOs(List<CustomerOrder> orders) {
        List<Map<String, Object>> orderDTOs = new ArrayList<>();
        for (CustomerOrder order : orders) {
            orderDTOs.add(createSafeOrderDTO(order));
        }
        return orderDTOs;
    }

    // ==================== GESTION DES PRODUITS ====================

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> getAllProducts() {
        try {
            List<Product> products = productRepository.findAllByOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/active")
    public ResponseEntity<Map<String, Object>> getActiveProducts() {
        try {
            List<Product> products = productRepository.findByIsActiveTrueOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockProducts() {
        try {
            List<Product> products = productRepository.findLowStockProducts(10);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits en stock faible: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/out-of-stock")
    public ResponseEntity<Map<String, Object>> getOutOfStockProducts() {
        try {
            List<Product> products = productRepository.findOutOfStockProducts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits en rupture de stock: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProductById(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("product", productOpt.get());
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du produit: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> productData) {
        try {
            System.out.println("=== TENTATIVE DE CRÉATION DE PRODUIT ===");
            System.out.println("Données reçues: " + productData);

            // Validation des champs obligatoires
            if (productData.get("name") == null || ((String) productData.get("name")).trim().isEmpty()) {
                return createErrorResponse("Le nom du produit est requis", HttpStatus.BAD_REQUEST);
            }

            // Créer un nouveau produit
            Product product = new Product();

            // Champs obligatoires
            product.setName((String) productData.get("name"));
            product.setDescription((String) productData.get("description"));

            // Gérer le prix avec valeur par défaut
            if (productData.get("price") != null) {
                try {
                    product.setPrice(Double.parseDouble(productData.get("price").toString()));
                } catch (NumberFormatException e) {
                    product.setPrice(0.0);
                }
            } else {
                product.setPrice(0.0);
            }

            // Gérer la quantité en stock avec valeur par défaut
            if (productData.get("stockQuantity") != null) {
                try {
                    product.setStockQuantity(Integer.parseInt(productData.get("stockQuantity").toString()));
                } catch (NumberFormatException e) {
                    product.setStockQuantity(0);
                }
            } else {
                product.setStockQuantity(0);
            }

            // Gérer la catégorie avec valeur par défaut
            if (productData.get("category") != null) {
                product.setCategory((String) productData.get("category"));
            } else {
                product.setCategory("Non catégorisé");
            }

            // Champs optionnels
            product.setImageUrl((String) productData.get("imageUrl"));
            product.setBrand((String) productData.get("brand"));

            // Gérer le poids
            if (productData.get("weight") != null) {
                try {
                    product.setWeight(Double.parseDouble(productData.get("weight").toString()));
                } catch (NumberFormatException e) {
                    product.setWeight(0.0);
                }
            }

            // Gérer le statut actif
            if (productData.get("isActive") != null) {
                product.setIsActive(Boolean.parseBoolean(productData.get("isActive").toString()));
            } else {
                product.setIsActive(true);
            }

            // Initialiser les compteurs de vente
            product.setSalesCount(0);

            // Dates de création et mise à jour
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            System.out.println("Produit à sauvegarder: " + product);

            // Sauvegarder le produit
            Product savedProduct = productRepository.save(product);

            System.out.println("Produit créé avec succès, ID: " + savedProduct.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produit créé avec succès");
            response.put("product", savedProduct);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE lors de la création du produit: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la création du produit: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> productData) {
        try {
            System.out.println("=== TENTATIVE DE MISE À JOUR DE PRODUIT ===");
            System.out.println("ID: " + id + ", Données: " + productData);

            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Mettre à jour les champs avec validation
                if (productData.get("name") != null) {
                    product.setName((String) productData.get("name"));
                }
                if (productData.get("description") != null) {
                    product.setDescription((String) productData.get("description"));
                }
                if (productData.get("price") != null) {
                    try {
                        product.setPrice(Double.parseDouble(productData.get("price").toString()));
                    } catch (NumberFormatException e) {
                        System.err.println("Format de prix invalide: " + productData.get("price"));
                    }
                }
                if (productData.get("stockQuantity") != null) {
                    try {
                        product.setStockQuantity(Integer.parseInt(productData.get("stockQuantity").toString()));
                    } catch (NumberFormatException e) {
                        System.err.println("Format de quantité invalide: " + productData.get("stockQuantity"));
                    }
                }
                if (productData.get("category") != null) {
                    product.setCategory((String) productData.get("category"));
                }
                if (productData.get("imageUrl") != null) {
                    product.setImageUrl((String) productData.get("imageUrl"));
                }
                if (productData.get("isActive") != null) {
                    product.setIsActive(Boolean.parseBoolean(productData.get("isActive").toString()));
                }
                if (productData.get("brand") != null) {
                    product.setBrand((String) productData.get("brand"));
                }
                if (productData.get("weight") != null) {
                    try {
                        product.setWeight(Double.parseDouble(productData.get("weight").toString()));
                    } catch (NumberFormatException e) {
                        System.err.println("Format de poids invalide: " + productData.get("weight"));
                    }
                }

                product.setUpdatedAt(LocalDateTime.now());

                System.out.println("Produit après mise à jour: " + product);

                Product updatedProduct = productRepository.save(product);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Produit modifié avec succès");
                response.put("product", updatedProduct);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE lors de la modification du produit: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la modification du produit: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/products/{id}/stock")
    public ResponseEntity<Map<String, Object>> updateProductStock(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> stockUpdate) {

        try {
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                Integer newStock = stockUpdate.get("stockQuantity");

                if (newStock != null && newStock >= 0) {
                    product.setStockQuantity(newStock);
                    product.setUpdatedAt(LocalDateTime.now());

                    Product updatedProduct = productRepository.save(product);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Stock mis à jour: " + newStock + " unités");
                    response.put("product", updatedProduct);

                    return ResponseEntity.ok(response);
                } else {
                    return createErrorResponse("Quantité de stock invalide", HttpStatus.BAD_REQUEST);
                }
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour du stock: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/products/{id}/status")
    public ResponseEntity<Map<String, Object>> updateProductStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> statusUpdate) {

        try {
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                Boolean isActive = statusUpdate.get("isActive");

                if (isActive != null) {
                    product.setIsActive(isActive);
                    product.setUpdatedAt(LocalDateTime.now());

                    Product updatedProduct = productRepository.save(product);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Statut du produit mis à jour: " + (isActive ? "Activé" : "Désactivé"));
                    response.put("product", updatedProduct);

                    return ResponseEntity.ok(response);
                } else {
                    return createErrorResponse("Statut invalide", HttpStatus.BAD_REQUEST);
                }
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour du statut: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== SUPPRESSION DÉFINITIVE PRODUITS ====================

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                // Vérifier d'abord s'il y a des commandes liées à ce produit
                List<OrderItem> relatedItems = orderItemRepository.findByProductId(id);

                if (!relatedItems.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Impossible de supprimer ce produit : " +
                            relatedItems.size() + " commande(s) y font référence. Désactivez-le plutôt.");
                    response.put("relatedOrdersCount", relatedItems.size());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // SUPPRESSION DÉFINITIVE
                productRepository.deleteById(id);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Produit supprimé définitivement avec succès");
                response.put("deletedProductId", id);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression définitive: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/products/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteProducts(@RequestBody Map<String, List<Long>> deleteData) {
        try {
            List<Long> productIds = deleteData.get("productIds");

            if (productIds == null || productIds.isEmpty()) {
                return createErrorResponse("Aucun produit sélectionné", HttpStatus.BAD_REQUEST);
            }

            int deletedCount = 0;
            List<Long> cannotDeleteIds = new ArrayList<>();

            for (Long id : productIds) {
                if (productRepository.existsById(id)) {
                    // Vérifier s'il y a des commandes liées
                    List<OrderItem> relatedItems = orderItemRepository.findByProductId(id);
                    if (relatedItems.isEmpty()) {
                        productRepository.deleteById(id);
                        deletedCount++;
                    } else {
                        cannotDeleteIds.add(id);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletedCount + " produit(s) supprimé(s) définitivement");
            response.put("deletedCount", deletedCount);
            response.put("cannotDeleteCount", cannotDeleteIds.size());
            response.put("cannotDeleteIds", cannotDeleteIds);

            if (!cannotDeleteIds.isEmpty()) {
                response.put("warning", "Certains produits n'ont pas pu être supprimés car liés à des commandes");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression en masse: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/{id}/can-delete")
    public ResponseEntity<Map<String, Object>> canDeleteProduct(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);

            if (productOpt.isPresent()) {
                // Vérifier les dépendances
                List<OrderItem> relatedOrderItems = orderItemRepository.findByProductId(id);
                boolean canDelete = relatedOrderItems.isEmpty();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("canDelete", canDelete);
                response.put("dependenciesCount", relatedOrderItems.size());
                response.put("message", canDelete ?
                        "Le produit peut être supprimé définitivement" :
                        "Le produit ne peut pas être supprimé (" + relatedOrderItems.size() + " commande(s) en dépendent)");

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la vérification: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== GESTION DES CLIENTS ====================

    @GetMapping("/customers")
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        try {
            List<Customer> customers = customerRepository.findAllByOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("customers", customers);
            response.put("count", customers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des clients: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Long id) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(id);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                List<CustomerOrder> customerOrders = customerOrderRepository.findByCustomerEmailOrderByOrderDateDesc(customer.getEmail());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("customer", customer);
                response.put("orders", customerOrders);
                response.put("ordersCount", customerOrders.size());
                response.put("totalSpent", customerOrders.stream().mapToDouble(CustomerOrder::getTotalAmount).sum());

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Client non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/customers")
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody Customer customer) {
        try {
            if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
                return createErrorResponse("L'email du client est requis", HttpStatus.BAD_REQUEST);
            }

            if (customerRepository.existsByEmail(customer.getEmail())) {
                return createErrorResponse("Un client avec cet email existe déjà", HttpStatus.BAD_REQUEST);
            }

            customer.setRegistrationDate(LocalDateTime.now());
            customer.setCreatedAt(LocalDateTime.now());
            Customer savedCustomer = customerRepository.save(customer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Client créé avec succès");
            response.put("customer", savedCustomer);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la création du client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomer(@PathVariable Long id, @RequestBody Customer customerDetails) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(id);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();

                if (customerDetails.getFirstName() != null) customer.setFirstName(customerDetails.getFirstName());
                if (customerDetails.getLastName() != null) customer.setLastName(customerDetails.getLastName());
                if (customerDetails.getEmail() != null) customer.setEmail(customerDetails.getEmail());
                if (customerDetails.getPhoneNumber() != null) customer.setPhoneNumber(customerDetails.getPhoneNumber());
                if (customerDetails.getAddress() != null) customer.setAddress(customerDetails.getAddress());
                if (customerDetails.getCity() != null) customer.setCity(customerDetails.getCity());
                if (customerDetails.getGovernorate() != null) customer.setGovernorate(customerDetails.getGovernorate());

                Customer updatedCustomer = customerRepository.save(customer);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Client modifié avec succès");
                response.put("customer", updatedCustomer);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Client non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la modification du client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== SUPPRESSION DÉFINITIVE CLIENTS ====================

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Long id) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(id);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();

                // Vérifier si le client a des commandes
                List<CustomerOrder> customerOrders = customerOrderRepository.findByCustomerEmail(customer.getEmail());

                if (!customerOrders.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Impossible de supprimer ce client : " +
                            customerOrders.size() + " commande(s) associée(s).");
                    response.put("ordersCount", customerOrders.size());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }

                // SUPPRESSION DÉFINITIVE
                customerRepository.deleteById(id);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Client supprimé définitivement avec succès");
                response.put("deletedCustomerId", id);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Client non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression du client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/customers/bulk-delete")
    public ResponseEntity<Map<String, Object>> bulkDeleteCustomers(@RequestBody Map<String, List<Long>> deleteData) {
        try {
            List<Long> customerIds = deleteData.get("customerIds");

            if (customerIds == null || customerIds.isEmpty()) {
                return createErrorResponse("Aucun client sélectionné", HttpStatus.BAD_REQUEST);
            }

            int deletedCount = 0;
            List<Long> cannotDeleteIds = new ArrayList<>();

            for (Long id : customerIds) {
                Optional<Customer> customerOpt = customerRepository.findById(id);
                if (customerOpt.isPresent()) {
                    Customer customer = customerOpt.get();
                    List<CustomerOrder> customerOrders = customerOrderRepository.findByCustomerEmail(customer.getEmail());

                    if (customerOrders.isEmpty()) {
                        customerRepository.deleteById(id);
                        deletedCount++;
                    } else {
                        cannotDeleteIds.add(id);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", deletedCount + " client(s) supprimé(s) définitivement");
            response.put("deletedCount", deletedCount);
            response.put("cannotDeleteCount", cannotDeleteIds.size());
            response.put("cannotDeleteIds", cannotDeleteIds);

            if (!cannotDeleteIds.isEmpty()) {
                response.put("warning", "Certains clients n'ont pas pu être supprimés car ils ont des commandes");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression en masse: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== CLIENTS PAR COMMANDE ====================

    @GetMapping("/orders/{orderId}/customer")
    public ResponseEntity<Map<String, Object>> getCustomerByOrder(@PathVariable Long orderId) {
        try {
            Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                CustomerOrder order = orderOpt.get();
                String customerEmail = order.getCustomerEmail();

                // Trouver le client par email
                Optional<Customer> customerOpt = customerRepository.findByEmail(customerEmail);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("orderId", orderId);
                response.put("orderNumber", order.getOrderNumber());

                if (customerOpt.isPresent()) {
                    Customer customer = customerOpt.get();
                    response.put("customer", customer);
                    response.put("customerFound", true);
                } else {
                    response.put("customerFound", false);
                    response.put("customerEmail", customerEmail);
                    response.put("message", "Client non trouvé dans la base de données");
                }

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Commande non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/orders/{orderId}/customer-complete")
    public ResponseEntity<Map<String, Object>> getCustomerDetailsByOrder(@PathVariable Long orderId) {
        try {
            Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);

            if (orderOpt.isPresent()) {
                CustomerOrder order = orderOpt.get();
                String customerEmail = order.getCustomerEmail();

                // Trouver le client par email
                Optional<Customer> customerOpt = customerRepository.findByEmail(customerEmail);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("order", createSafeOrderDTO(order));

                if (customerOpt.isPresent()) {
                    Customer customer = customerOpt.get();

                    // Récupérer toutes les commandes de ce client
                    List<CustomerOrder> customerOrders = customerOrderRepository.findByCustomerEmailOrderByOrderDateDesc(customerEmail);

                    Map<String, Object> customerDetails = new HashMap<>();
                    customerDetails.put("id", customer.getId());
                    customerDetails.put("firstName", customer.getFirstName());
                    customerDetails.put("lastName", customer.getLastName());
                    customerDetails.put("email", customer.getEmail());
                    customerDetails.put("phoneNumber", customer.getPhoneNumber());
                    customerDetails.put("address", customer.getAddress());
                    customerDetails.put("city", customer.getCity());
                    customerDetails.put("governorate", customer.getGovernorate());
                    customerDetails.put("registrationDate", customer.getRegistrationDate());
                    customerDetails.put("totalOrders", customerOrders.size());
                    customerDetails.put("totalSpent", customerOrders.stream()
                            .mapToDouble(CustomerOrder::getTotalAmount)
                            .sum());
                    customerDetails.put("orderHistory", createSafeOrderDTOs(customerOrders));

                    response.put("customer", customerDetails);
                } else {
                    response.put("customer", null);
                    response.put("customerEmail", customerEmail);
                }

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Commande non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des détails client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/customers/{customerId}/orders")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(@PathVariable Long customerId) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(customerId);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                List<CustomerOrder> orders = customerOrderRepository.findByCustomerEmailOrderByOrderDateDesc(customer.getEmail());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("customer", customer);
                response.put("orders", createSafeOrderDTOs(orders));
                response.put("ordersCount", orders.size());
                response.put("totalSpent", orders.stream()
                        .mapToDouble(CustomerOrder::getTotalAmount)
                        .sum());
                response.put("lastOrderDate", orders.isEmpty() ? null : orders.get(0).getOrderDate());

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Client non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des commandes client: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== GESTION DES CATÉGORIES ====================

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des catégories: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/categories/active")
    public ResponseEntity<Map<String, Object>> getActiveCategories() {
        try {
            List<Category> categories = categoryRepository.findByIsActiveTrueOrderByNameAsc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des catégories actives: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(id);

            if (categoryOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("category", categoryOpt.get());
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Catégorie non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération de la catégorie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Category category) {
        try {
            if (category.getName() == null || category.getName().trim().isEmpty()) {
                return createErrorResponse("Le nom de la catégorie est requis", HttpStatus.BAD_REQUEST);
            }

            if (category.getIsActive() == null) category.setIsActive(true);

            category.setCreatedAt(LocalDateTime.now());
            Category savedCategory = categoryRepository.save(category);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Catégorie créée avec succès");
            response.put("category", savedCategory);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la création de la catégorie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(id);

            if (categoryOpt.isPresent()) {
                Category category = categoryOpt.get();

                if (categoryDetails.getName() != null) category.setName(categoryDetails.getName());
                if (categoryDetails.getDescription() != null) category.setDescription(categoryDetails.getDescription());
                if (categoryDetails.getImageUrl() != null) category.setImageUrl(categoryDetails.getImageUrl());
                if (categoryDetails.getIsActive() != null) category.setIsActive(categoryDetails.getIsActive());
                if (categoryDetails.getDisplayOrder() != null) category.setDisplayOrder(categoryDetails.getDisplayOrder());

                Category updatedCategory = categoryRepository.save(category);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Catégorie modifiée avec succès");
                response.put("category", updatedCategory);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Catégorie non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la modification de la catégorie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(id);

            if (categoryOpt.isPresent()) {
                categoryRepository.deleteById(id);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Catégorie supprimée avec succès");

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Catégorie non trouvée", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression de la catégorie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== GESTION DES COUPONS ====================

    @GetMapping("/coupons/active")
    public ResponseEntity<Map<String, Object>> getActiveCoupons() {
        try {
            List<Coupon> coupons = couponRepository.findByIsActiveTrueOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des coupons actifs: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/expired")
    public ResponseEntity<Map<String, Object>> getExpiredCoupons() {
        try {
            List<Coupon> coupons = couponRepository.findExpiredCoupons();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/fully-used")
    public ResponseEntity<Map<String, Object>> getFullyUsedCoupons() {
        try {
            List<Coupon> coupons = couponRepository.findFullyUsedCoupons();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/unused")
    public ResponseEntity<Map<String, Object>> getUnusedCoupons() {
        try {
            List<Coupon> coupons = couponRepository.findUnusedCoupons();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/most-used")
    public ResponseEntity<Map<String, Object>> getMostUsedCoupons(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Coupon> coupons = couponRepository.findMostUsedCoupons().stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());
            response.put("limit", limit);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/report")
    public ResponseEntity<Map<String, Object>> getCouponReport() {
        try {
            Map<String, Object> report = new HashMap<>();

            report.put("totalCoupons", couponRepository.count());
            report.put("activeCoupons", couponRepository.countByIsActiveTrue());
            report.put("expiredCoupons", couponRepository.findExpiredCoupons().size());
            report.put("fullyUsedCoupons", couponRepository.findFullyUsedCoupons().size());
            report.put("unusedCoupons", couponRepository.findUnusedCoupons().size());

            Double averageUsage = couponRepository.getAverageUsageRate();
            report.put("averageUsageRate", averageUsage != null ? averageUsage : 0.0);

            List<Coupon> mostUsed = couponRepository.findMostUsedCoupons().stream()
                    .limit(5)
                    .collect(Collectors.toList());
            report.put("mostUsedCoupons", mostUsed);

            List<Coupon> expiringSoon = couponRepository.findCouponsExpiringSoon();
            report.put("expiringSoon", expiringSoon);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("report", report);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la génération du rapport: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/summary")
    public ResponseEntity<Map<String, Object>> getCouponsSummary() {
        try {
            List<Coupon> coupons = couponRepository.findAll();

            List<Map<String, Object>> couponSummaries = coupons.stream()
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", c.getId());
                        map.put("code", c.getCode());
                        map.put("usedCount", c.getUsedCount());
                        map.put("maxUses", c.getMaxUses());
                        map.put("usageRate", c.getMaxUses() != null ?
                                (double) c.getUsedCount() / c.getMaxUses() * 100 : null);
                        map.put("status", getCouponStatus(c));
                        map.put("discount", formatDiscount(c));
                        map.put("expiryDate", c.getExpiryDate());
                        map.put("isActive", c.getIsActive());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> globalStats = new HashMap<>();
            globalStats.put("totalCoupons", coupons.size());
            globalStats.put("activeCoupons", coupons.stream().filter(Coupon::getIsActive).count());
            globalStats.put("inactiveCoupons", coupons.stream().filter(c -> !c.getIsActive()).count());
            globalStats.put("expiredCoupons", coupons.stream().filter(Coupon::isExpired).count());
            globalStats.put("validCoupons", coupons.stream().filter(Coupon::isValid).count());
            globalStats.put("totalUsage", coupons.stream()
                    .mapToInt(c -> c.getUsedCount() != null ? c.getUsedCount() : 0)
                    .sum());

            List<Map<String, Object>> popularCoupons = coupons.stream()
                    .filter(c -> c.getUsedCount() != null && c.getUsedCount() > 5)
                    .sorted((c1, c2) -> c2.getUsedCount().compareTo(c1.getUsedCount()))
                    .limit(5)
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("code", c.getCode());
                        map.put("usedCount", c.getUsedCount());
                        map.put("discount", formatDiscount(c));
                        return map;
                    })
                    .collect(Collectors.toList());

            globalStats.put("popularCoupons", popularCoupons);

            List<Map<String, Object>> expiringSoon = coupons.stream()
                    .filter(c -> c.getIsActive() && !c.isExpired())
                    .filter(c -> java.time.Duration.between(LocalDateTime.now(), c.getExpiryDate()).toDays() <= 7)
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("code", c.getCode());
                        map.put("daysLeft", java.time.Duration.between(LocalDateTime.now(), c.getExpiryDate()).toDays());
                        map.put("discount", formatDiscount(c));
                        return map;
                    })
                    .collect(Collectors.toList());

            globalStats.put("expiringSoon", expiringSoon);

            List<Map<String, Object>> almostExhausted = coupons.stream()
                    .filter(c -> c.getIsActive() && c.getMaxUses() != null)
                    .filter(c -> c.getMaxUses() - c.getUsedCount() <= 3)
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("code", c.getCode());
                        map.put("remainingUses", c.getMaxUses() - c.getUsedCount());
                        map.put("discount", formatDiscount(c));
                        return map;
                    })
                    .collect(Collectors.toList());

            globalStats.put("almostExhausted", almostExhausted);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", couponSummaries);
            response.put("globalStats", globalStats);
            response.put("count", coupons.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du résumé: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/{id}/stats")
    public ResponseEntity<Map<String, Object>> getCouponStats(@PathVariable Long id) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            Map<String, Object> stats = new HashMap<>();
            stats.put("id", coupon.getId());
            stats.put("code", coupon.getCode());
            stats.put("usedCount", coupon.getUsedCount() != null ? coupon.getUsedCount() : 0);
            stats.put("maxUses", coupon.getMaxUses());
            stats.put("remainingUses", coupon.getRemainingUses());
            stats.put("usagePercentage", coupon.getMaxUses() != null ?
                    ((double) (coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) / coupon.getMaxUses()) * 100 : 0);
            stats.put("isActive", coupon.getIsActive());
            stats.put("isExpired", coupon.isExpired());
            stats.put("daysUntilExpiry",
                    java.time.Duration.between(LocalDateTime.now(), coupon.getExpiryDate()).toDays());
            stats.put("createdAt", coupon.getCreatedAt());
            stats.put("expiryDate", coupon.getExpiryDate());
            stats.put("minOrderAmount", coupon.getMinOrderAmount());
            stats.put("discountType", coupon.getDiscountType());
            stats.put("discountValue", coupon.getDiscountValue());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des statistiques: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/{id}/detailed-stats")
    public ResponseEntity<Map<String, Object>> getCouponDetailedStats(@PathVariable Long id) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            Map<String, Object> stats = new HashMap<>();

            // Informations de base
            stats.put("id", coupon.getId());
            stats.put("code", coupon.getCode());
            stats.put("description", coupon.getDescription());

            // Statistiques d'utilisation
            stats.put("usedCount", coupon.getUsedCount());
            stats.put("maxUses", coupon.getMaxUses());
            stats.put("remainingUses", coupon.getMaxUses() != null ?
                    coupon.getMaxUses() - coupon.getUsedCount() : "Illimité");
            stats.put("usageRate", coupon.getMaxUses() != null ?
                    (double) coupon.getUsedCount() / coupon.getMaxUses() * 100 :
                    (coupon.getUsedCount() > 0 ? 100.0 : 0.0));

            // Statut
            stats.put("isActive", coupon.getIsActive());
            stats.put("isExpired", coupon.isExpired());
            stats.put("status", getCouponStatus(coupon));

            // Dates
            stats.put("createdAt", coupon.getCreatedAt());
            stats.put("expiryDate", coupon.getExpiryDate());

            // Calcul des jours restants
            if (!coupon.isExpired()) {
                long daysUntilExpiry = java.time.Duration.between(
                        LocalDateTime.now(), coupon.getExpiryDate()).toDays();
                stats.put("daysUntilExpiry", daysUntilExpiry);

                if (daysUntilExpiry <= 7) {
                    stats.put("expiryWarning", "Ce coupon expire dans " + daysUntilExpiry + " jours");
                }
            } else {
                stats.put("daysUntilExpiry", 0);
                stats.put("expiryWarning", "Ce coupon a expiré");
            }

            // Informations sur la réduction
            stats.put("discountType", coupon.getDiscountType());
            stats.put("discountValue", coupon.getDiscountValue());
            stats.put("discountDisplay", formatDiscount(coupon));
            stats.put("minOrderAmount", coupon.getMinOrderAmount());

            // Estimation de l'impact financier
            double estimatedTotalDiscount = coupon.getUsedCount() *
                    (coupon.getDiscountType().equals("PERCENTAGE") ? 100 : coupon.getDiscountValue());
            stats.put("estimatedTotalDiscount", estimatedTotalDiscount);

            // Prévisions
            if (coupon.getMaxUses() != null) {
                double projectedUsage = coupon.getUsedCount() /
                        (double) (coupon.getMaxUses());
                stats.put("projectedUsagePercentage", projectedUsage * 100);

                double remainingValue = (coupon.getMaxUses() - coupon.getUsedCount()) *
                        (coupon.getDiscountType().equals("PERCENTAGE") ? 100 : coupon.getDiscountValue());
                stats.put("remainingPotentialValue", remainingValue);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des statistiques: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/check-usable/{code}")
    public ResponseEntity<Map<String, Object>> checkCouponUsable(@PathVariable String code) {
        try {
            boolean isUsable = couponRepository.isUsableCoupon(code);
            Integer remainingUses = couponRepository.getRemainingUses(code);
            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("code", code);
            response.put("isUsable", isUsable);
            response.put("remainingUses", remainingUses);

            if (couponOpt.isPresent()) {
                response.put("coupon", couponOpt.get());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la vérification: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/validate-code")
    public ResponseEntity<Map<String, Object>> validateCouponCode(@RequestBody Map<String, String> data) {
        try {
            String code = data.get("code");

            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("Le code coupon est requis", HttpStatus.BAD_REQUEST);
            }

            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                boolean isValid = coupon.isValid();
                String statusMessage;

                if (!coupon.getIsActive()) {
                    statusMessage = "Coupon désactivé";
                } else if (coupon.isExpired()) {
                    statusMessage = "Coupon expiré";
                } else if (coupon.hasReachedMaxUses()) {
                    statusMessage = "Coupon utilisé au maximum";
                } else {
                    statusMessage = "Coupon valide";
                }

                response.put("exists", true);
                response.put("valid", isValid);
                response.put("status", statusMessage);
                response.put("coupon", coupon);
            } else {
                response.put("exists", false);
                response.put("valid", false);
                response.put("status", "Coupon non trouvé");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la validation du code: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/validate-with-details")
    public ResponseEntity<Map<String, Object>> validateCouponWithDetails(
            @RequestBody Map<String, Object> validationData) {
        try {
            String code = (String) validationData.get("code");
            Double orderAmount = null;
            String customerEmail = (String) validationData.get("customerEmail");

            if (validationData.get("orderAmount") != null) {
                try {
                    orderAmount = Double.parseDouble(validationData.get("orderAmount").toString());
                } catch (NumberFormatException e) {
                    orderAmount = 0.0;
                }
            }

            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("Le code coupon est requis", HttpStatus.BAD_REQUEST);
            }

            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);

            if (!couponOpt.isPresent()) {
                Map<String, Object> validationResult = new HashMap<>();
                validationResult.put("valid", false);
                validationResult.put("exists", false);
                validationResult.put("message", "Coupon non trouvé");
                validationResult.put("errors", Arrays.asList("Le code coupon n'existe pas dans notre système"));

                response.put("validation", validationResult);
                return ResponseEntity.ok(response);
            }

            Coupon coupon = couponOpt.get();
            Map<String, Object> validationResult = new HashMap<>();
            validationResult.put("valid", false);
            validationResult.put("exists", true);
            validationResult.put("coupon", coupon);

            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            if (!coupon.getIsActive()) {
                errors.add("Le coupon est désactivé");
            }

            if (coupon.isExpired()) {
                errors.add("Le coupon a expiré le " +
                        coupon.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }

            if (coupon.hasReachedMaxUses()) {
                errors.add("Le coupon a atteint son nombre maximum d'utilisations (" +
                        coupon.getMaxUses() + " sur " + coupon.getMaxUses() + ")");
            } else if (coupon.getMaxUses() != null) {
                int remainingUses = coupon.getMaxUses() - coupon.getUsedCount();
                validationResult.put("remainingUses", remainingUses);
                if (remainingUses <= 5) {
                    warnings.add("Attention: Il ne reste que " + remainingUses + " utilisation(s) pour ce coupon");
                }
            }

            if (orderAmount != null && orderAmount > 0) {
                if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
                    errors.add("Montant minimum de commande non atteint. Minimum requis: " +
                            String.format("%.3f", coupon.getMinOrderAmount()) + " DT");
                }
            }

            if (errors.isEmpty()) {
                validationResult.put("valid", true);
                validationResult.put("message", "Coupon valide");

                if (orderAmount != null && orderAmount > 0) {
                    double discount = calculateDiscountAmount(coupon, orderAmount);
                    double finalAmount = orderAmount - discount;

                    validationResult.put("originalAmount", orderAmount);
                    validationResult.put("discountAmount", discount);
                    validationResult.put("finalAmount", finalAmount);
                    validationResult.put("discountType", coupon.getDiscountType());
                    validationResult.put("discountValue", coupon.getDiscountValue());
                    validationResult.put("savingsPercentage", orderAmount > 0 ?
                            (discount / orderAmount) * 100 : 0);

                    if ("FREE_SHIPPING".equalsIgnoreCase(coupon.getDiscountType())) {
                        validationResult.put("freeShipping", true);
                    }
                }
            } else {
                validationResult.put("message", errors.get(0));
            }

            validationResult.put("errors", errors);
            validationResult.put("warnings", warnings);

            response.put("validation", validationResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la validation: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/simulate")
    public ResponseEntity<Map<String, Object>> simulateCoupon(
            @RequestBody Map<String, Object> simulationData) {

        try {
            System.out.println("=== SIMULATION D'APPLICATION DE COUPON ===");
            System.out.println("Données reçues: " + simulationData);

            String code = (String) simulationData.get("code");
            Double orderAmount = null;

            try {
                if (simulationData.get("orderAmount") != null) {
                    orderAmount = Double.parseDouble(simulationData.get("orderAmount").toString());
                }
            } catch (NumberFormatException e) {
                return createErrorResponse("Montant de commande invalide", HttpStatus.BAD_REQUEST);
            }

            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("Le code coupon est requis", HttpStatus.BAD_REQUEST);
            }

            if (orderAmount == null || orderAmount <= 0) {
                return createErrorResponse("Le montant de commande est invalide", HttpStatus.BAD_REQUEST);
            }

            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            if (!coupon.isValid()) {
                String errorMessage = "Coupon invalide. Raison: ";

                if (!coupon.getIsActive()) {
                    errorMessage += "désactivé";
                } else if (coupon.isExpired()) {
                    errorMessage += "expiré";
                } else if (coupon.hasReachedMaxUses()) {
                    errorMessage += "nombre maximum d'utilisations atteint";
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", errorMessage);
                response.put("valid", false);

                return ResponseEntity.ok(response);
            }

            if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Montant minimum de commande non atteint. Minimum requis: " + coupon.getMinOrderAmount());
                response.put("valid", false);
                response.put("minOrderAmount", coupon.getMinOrderAmount());

                return ResponseEntity.ok(response);
            }

            double discountAmount = 0.0;
            String discountType = coupon.getDiscountType();
            boolean freeShipping = false;

            if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
                discountAmount = orderAmount * (coupon.getDiscountValue() / 100);
            } else if ("FIXED".equalsIgnoreCase(discountType)) {
                discountAmount = Math.min(orderAmount, coupon.getDiscountValue());
            } else if ("FREE_SHIPPING".equalsIgnoreCase(discountType)) {
                freeShipping = true;
            }

            double finalAmount = orderAmount - discountAmount;

            Map<String, Object> simulationResult = new HashMap<>();
            simulationResult.put("success", true);
            simulationResult.put("valid", true);
            simulationResult.put("coupon", coupon);
            simulationResult.put("originalAmount", orderAmount);
            simulationResult.put("discountAmount", discountAmount);
            simulationResult.put("discountType", discountType);
            simulationResult.put("discountValue", coupon.getDiscountValue());
            simulationResult.put("finalAmount", finalAmount);
            simulationResult.put("freeShipping", freeShipping);
            simulationResult.put("savingsPercentage", (discountAmount / orderAmount) * 100);
            simulationResult.put("remainingUses", coupon.getRemainingUses());

            System.out.println("Simulation réussie: " + simulationResult);

            return ResponseEntity.ok(simulationResult);

        } catch (Exception e) {
            System.err.println("Erreur lors de la simulation: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la simulation: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons")
    public ResponseEntity<Map<String, Object>> getAllCoupons() {
        try {
            // Récupérer tous les coupons
            List<Coupon> coupons = couponRepository.findAll();

            // Pour chaque coupon, le usedCount est déjà stocké en base
            // Pas besoin de calcul supplémentaire

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("coupons", coupons);
            response.put("count", coupons.size());

            // Log pour vérifier
            System.out.println("📊 Envoi des coupons avec compteurs:");
            coupons.forEach(c ->
                    System.out.println("   - " + c.getCode() + ": " + c.getUsedCount() + "/" + c.getMaxUses())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/use-enhanced")
    @Transactional
    public ResponseEntity<Map<String, Object>> useCouponEnhanced(@RequestBody Map<String, Object> data) {
        try {
            String code = (String) data.get("code");
            String orderNumber = (String) data.get("orderNumber");
            Double orderAmount = data.get("orderAmount") != null ?
                    Double.parseDouble(data.get("orderAmount").toString()) : 0.0;

            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("Le code coupon est requis", HttpStatus.BAD_REQUEST);
            }

            if (orderNumber == null || orderNumber.trim().isEmpty()) {
                return createErrorResponse("Le numéro de commande est requis", HttpStatus.BAD_REQUEST);
            }

            Optional<Coupon> couponOpt = couponRepository.findByCodeForUpdate(code);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            List<String> validationErrors = new ArrayList<>();

            if (!coupon.getIsActive()) {
                validationErrors.add("Coupon désactivé");
            }

            if (coupon.isExpired()) {
                validationErrors.add("Coupon expiré depuis le " +
                        coupon.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }

            if (coupon.hasReachedMaxUses()) {
                validationErrors.add("Coupon a atteint son nombre maximum d'utilisations (" +
                        coupon.getUsedCount() + "/" + coupon.getMaxUses() + ")");
            }

            if (coupon.getMinOrderAmount() != null && orderAmount < coupon.getMinOrderAmount()) {
                validationErrors.add("Montant minimum de commande non atteint. Requis: " +
                        String.format("%.3f", coupon.getMinOrderAmount()) + " DT");
            }

            if (!validationErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", validationErrors.get(0));
                errorResponse.put("errors", validationErrors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            double discountAmount = calculateDiscountAmount(coupon, orderAmount);

            coupon.incrementUsedCount();
            couponRepository.save(coupon);

            int remainingUses = coupon.getMaxUses() != null ?
                    coupon.getMaxUses() - coupon.getUsedCount() : -1;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon utilisé avec succès");

            Map<String, Object> couponData = new HashMap<>();
            couponData.put("id", coupon.getId());
            couponData.put("code", coupon.getCode());
            couponData.put("usedCount", coupon.getUsedCount());
            couponData.put("maxUses", coupon.getMaxUses());
            couponData.put("remainingUses", remainingUses >= 0 ? remainingUses : "Illimité");
            couponData.put("discountType", coupon.getDiscountType());
            couponData.put("discountValue", coupon.getDiscountValue());

            Map<String, Object> discountInfo = new HashMap<>();
            discountInfo.put("originalAmount", orderAmount);
            discountInfo.put("discountAmount", discountAmount);
            discountInfo.put("finalAmount", orderAmount - discountAmount);
            discountInfo.put("discountType", coupon.getDiscountType());
            discountInfo.put("freeShipping", "FREE_SHIPPING".equalsIgnoreCase(coupon.getDiscountType()));

            response.put("coupon", couponData);
            response.put("discount", discountInfo);
            response.put("orderNumber", orderNumber);

            if (remainingUses >= 0 && remainingUses <= 3) {
                response.put("warning", "Attention: Il ne reste que " + remainingUses +
                        " utilisation(s) pour ce coupon");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'utilisation du coupon: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/{id}/increment-usage")
    public ResponseEntity<Map<String, Object>> incrementCouponUsage(@PathVariable Long id) {
        try {
            System.out.println("=== INCRÉMENTATION DU COMPTEUR COUPON ===");
            System.out.println("ID du coupon: " + id);

            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
                return createErrorResponse("Ce coupon a atteint son nombre maximum d'utilisations",
                        HttpStatus.BAD_REQUEST);
            }

            int currentCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            coupon.setUsedCount(currentCount + 1);
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon updatedCoupon = couponRepository.save(coupon);

            System.out.println("✅ Compteur incrémenté: " + currentCount + " → " + updatedCoupon.getUsedCount());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compteur d'utilisations mis à jour avec succès");
            response.put("coupon", updatedCoupon);
            response.put("oldCount", currentCount);
            response.put("newCount", updatedCoupon.getUsedCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'incrémentation du compteur: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de l'incrémentation du compteur: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/increment-usage-by-code")
    public ResponseEntity<Map<String, Object>> incrementCouponUsageByCode(@RequestBody Map<String, String> request) {
        try {
            String couponCode = request.get("code");

            if (couponCode == null || couponCode.trim().isEmpty()) {
                return createErrorResponse("Le code du coupon est requis", HttpStatus.BAD_REQUEST);
            }

            System.out.println("=== INCRÉMENTATION DU COMPTEUR PAR CODE ===");
            System.out.println("Code du coupon: " + couponCode);

            Optional<Coupon> couponOpt = couponRepository.findByCode(couponCode);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé: " + couponCode, HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            if (coupon.getMaxUses() != null && coupon.getUsedCount() >= coupon.getMaxUses()) {
                return createErrorResponse("Ce coupon a atteint son nombre maximum d'utilisations",
                        HttpStatus.BAD_REQUEST);
            }

            int currentCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            coupon.setUsedCount(currentCount + 1);
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon updatedCoupon = couponRepository.save(coupon);

            System.out.println("✅ Compteur incrémenté: " + currentCount + " → " + updatedCoupon.getUsedCount());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compteur d'utilisations mis à jour avec succès");
            response.put("coupon", updatedCoupon);
            response.put("oldCount", currentCount);
            response.put("newCount", updatedCoupon.getUsedCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'incrémentation du compteur: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de l'incrémentation du compteur: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/{id}/reset-usage")
    public ResponseEntity<Map<String, Object>> resetCouponUsage(@PathVariable Long id) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            int oldCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            coupon.setUsedCount(0);
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon updatedCoupon = couponRepository.save(coupon);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compteur réinitialisé avec succès");
            response.put("coupon", updatedCoupon);
            response.put("oldCount", oldCount);
            response.put("newCount", 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la réinitialisation: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/coupons/{id}/reset-counter")
    public ResponseEntity<Map<String, Object>> resetCouponCounter(@PathVariable Long id) {
        return resetCouponUsage(id); // Redirige vers la méthode unifiée
    }

    // ==================== GÉNÉRATION DE CODE COUPON ====================

    @PostMapping("/coupons/generate-code")
    public ResponseEntity<Map<String, Object>> generateCouponCode(
            @RequestParam(required = false) String prefix,
            @RequestBody(required = false) Map<String, String> body) {

        try {
            System.out.println("=== GÉNÉRATION DE CODE COUPON ===");
            System.out.println("Prefix reçu: " + prefix);

            if (prefix == null && body != null && body.containsKey("prefix")) {
                prefix = body.get("prefix");
                System.out.println("Prefix extrait du body: " + prefix);
            }

            if (prefix == null || prefix.trim().isEmpty()) {
                prefix = "PROMO";
            }

            String code;
            int attempts = 0;

            do {
                String randomPart = generateRandomString(8).toUpperCase();
                code = prefix + "-" + randomPart;
                attempts++;

                if (attempts > 10) {
                    return createErrorResponse("Impossible de générer un code unique", HttpStatus.INTERNAL_SERVER_ERROR);
                }

            } while (couponRepository.existsByCode(code));

            System.out.println("Code généré: " + code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("code", code);
            response.put("prefix", prefix);
            response.put("message", "Code coupon généré avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du code coupon: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la génération du code coupon: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== CRUD COUPONS ====================

    @PostMapping("/coupons")
    public ResponseEntity<Map<String, Object>> createCoupon(@RequestBody Coupon coupon) {
        try {
            System.out.println("=== TENTATIVE DE CRÉATION DE COUPON ===");
            System.out.println("Données reçues: " + coupon);

            if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
                return createErrorResponse("Le code du coupon est requis", HttpStatus.BAD_REQUEST);
            }

            if (couponRepository.existsByCode(coupon.getCode())) {
                return createErrorResponse("Un coupon avec ce code existe déjà", HttpStatus.BAD_REQUEST);
            }

            if (coupon.getDiscountValue() == null || coupon.getDiscountValue() <= 0) {
                return createErrorResponse("La valeur de réduction doit être positive", HttpStatus.BAD_REQUEST);
            }

            String discountType = coupon.getDiscountType();
            if (discountType == null ||
                    (!"PERCENTAGE".equalsIgnoreCase(discountType) &&
                            !"FIXED".equalsIgnoreCase(discountType) &&
                            !"FREE_SHIPPING".equalsIgnoreCase(discountType))) {
                return createErrorResponse("Type de réduction invalide. Options: PERCENTAGE, FIXED, FREE_SHIPPING", HttpStatus.BAD_REQUEST);
            }

            if (coupon.getExpiryDate() == null) {
                return createErrorResponse("La date d'expiration est requise", HttpStatus.BAD_REQUEST);
            }

            if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
                return createErrorResponse("La date d'expiration ne peut pas être dans le passé", HttpStatus.BAD_REQUEST);
            }

            if (coupon.getIsActive() == null) {
                coupon.setIsActive(true);
            }

            if (coupon.getUsedCount() == null) {
                coupon.setUsedCount(0);
            }

            coupon.setCreatedAt(LocalDateTime.now());
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon savedCoupon = couponRepository.save(coupon);

            System.out.println("Coupon créé avec succès, ID: " + savedCoupon.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon créé avec succès");
            response.put("coupon", savedCoupon);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE lors de la création du coupon: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de la création du coupon: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<Map<String, Object>> updateCoupon(@PathVariable Long id, @RequestBody Coupon couponDetails) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                if (couponDetails.getCode() != null) coupon.setCode(couponDetails.getCode());
                if (couponDetails.getDiscountValue() != null) coupon.setDiscountValue(couponDetails.getDiscountValue());
                if (couponDetails.getDiscountType() != null) coupon.setDiscountType(couponDetails.getDiscountType());
                if (couponDetails.getExpiryDate() != null) coupon.setExpiryDate(couponDetails.getExpiryDate());
                if (couponDetails.getIsActive() != null) coupon.setIsActive(couponDetails.getIsActive());
                if (couponDetails.getDescription() != null) coupon.setDescription(couponDetails.getDescription());
                if (couponDetails.getMaxUses() != null) coupon.setMaxUses(couponDetails.getMaxUses());
                if (couponDetails.getMinOrderAmount() != null) coupon.setMinOrderAmount(couponDetails.getMinOrderAmount());

                coupon.setUpdatedAt(LocalDateTime.now());

                Coupon updatedCoupon = couponRepository.save(coupon);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Coupon modifié avec succès");
                response.put("coupon", updatedCoupon);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la modification du coupon: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Map<String, Object>> deleteCoupon(@PathVariable Long id) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (couponOpt.isPresent()) {
                couponRepository.deleteById(id);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Coupon supprimé avec succès");

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la suppression du coupon: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/coupons/use")
    public ResponseEntity<Map<String, Object>> useCoupon(@RequestBody Map<String, String> data) {
        try {
            String code = data.get("code");
            String orderNumber = data.get("orderNumber");
            String customerEmail = data.get("customerEmail");

            System.out.println("🎫 UTILISATION DU COUPON: " + code);

            if (code == null || code.trim().isEmpty()) {
                return createErrorResponse("Code coupon requis", HttpStatus.BAD_REQUEST);
            }

            Optional<Coupon> couponOpt = couponRepository.findByCode(code);

            if (!couponOpt.isPresent()) {
                return createErrorResponse("Coupon non trouvé: " + code, HttpStatus.NOT_FOUND);
            }

            Coupon coupon = couponOpt.get();

            // Vérifier si le coupon est valide
            if (!coupon.isValid()) {
                String reason = "";
                if (!coupon.getIsActive()) reason = "désactivé";
                else if (coupon.isExpired()) reason = "expiré";
                else if (coupon.hasReachedMaxUses()) reason = "limite atteinte";

                return createErrorResponse("Coupon non valide: " + reason, HttpStatus.BAD_REQUEST);
            }

            // ✅ INCRÉMENTER LE COMPTEUR
            int oldCount = coupon.getUsedCount() != null ? coupon.getUsedCount() : 0;
            int newCount = oldCount + 1;

            coupon.setUsedCount(newCount);
            coupon.setUpdatedAt(LocalDateTime.now());

            Coupon updatedCoupon = couponRepository.save(coupon);

            System.out.println("✅ Compteur mis à jour: " + oldCount + " → " + newCount);
            System.out.println("   Coupon: " + updatedCoupon.getCode());

            // Journaliser l'utilisation
            logCouponUsage(updatedCoupon, orderNumber, customerEmail, null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon utilisé avec succès");
            response.put("code", updatedCoupon.getCode());
            response.put("oldCount", oldCount);
            response.put("newCount", newCount);
            response.put("coupon", updatedCoupon);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<Map<String, Object>> getCouponById(@PathVariable Long id) {
        try {
            Optional<Coupon> couponOpt = couponRepository.findById(id);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("coupon", coupon);

                // Ajouter des infos supplémentaires
                Map<String, Object> stats = new HashMap<>();
                stats.put("usedCount", coupon.getUsedCount());
                stats.put("maxUses", coupon.getMaxUses());
                stats.put("remainingUses", coupon.getRemainingUses());
                stats.put("usageRate", coupon.getMaxUses() != null ?
                        (double) coupon.getUsedCount() / coupon.getMaxUses() * 100 : 0);

                response.put("stats", stats);

                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Coupon non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== GESTION DES ORDER ITEMS ====================

    @GetMapping("/order-items")
    public ResponseEntity<Map<String, Object>> getAllOrderItems() {
        try {
            List<OrderItem> orderItems = orderItemRepository.findAllByOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderItems", orderItems);
            response.put("count", orderItems.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des order items: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/order-items/order/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderItemsByOrder(@PathVariable Long orderId) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderItems", orderItems);
            response.put("count", orderItems.size());
            response.put("orderId", orderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des order items: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== STATISTIQUES AVANCÉES ====================

    @GetMapping("/stats/advanced")
    public ResponseEntity<Map<String, Object>> getAdvancedStats() {
        try {
            Map<String, Object> advancedStats = new HashMap<>();

            Double totalRevenue = customerOrderRepository.getTotalRevenue();
            Double averageOrderValue = customerOrderRepository.getAverageOrderValue();
            List<Object[]> monthlyStats = customerOrderRepository.getMonthlyStats(
                    LocalDateTime.now().minusMonths(6),
                    LocalDateTime.now()
            );

            List<Product> topProductsList = productRepository.findTop5ByOrderBySalesCountDesc();
            List<Object[]> topProducts = topProductsList.stream()
                    .map(p -> new Object[]{
                            p.getName() != null ? p.getName() : "Sans nom",
                            p.getSalesCount() != null ? p.getSalesCount() : 0,
                            p.getPrice() != null ? p.getPrice() : 0.0
                    })
                    .collect(Collectors.toList());

            List<Object[]> topCustomers = customerOrderRepository.getTopCustomers();

            // Statistiques coupons avancées
            List<Coupon> mostUsedCoupons = couponRepository.findMostUsedCoupons().stream()
                    .limit(10)
                    .collect(Collectors.toList());
            List<Coupon> fullyUsedCoupons = couponRepository.findFullyUsedCoupons();
            List<Coupon> unusedCoupons = couponRepository.findUnusedCoupons();
            List<Coupon> expiredCoupons = couponRepository.findExpiredCoupons();
            Double averageUsageRate = couponRepository.getAverageUsageRate();

            advancedStats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            advancedStats.put("averageOrderValue", averageOrderValue != null ? averageOrderValue : 0.0);
            advancedStats.put("monthlyStats", monthlyStats);
            advancedStats.put("topProducts", topProducts);
            advancedStats.put("topCustomers", topCustomers);
            advancedStats.put("paymentMethodStats", customerOrderRepository.getPaymentMethodStats());
            advancedStats.put("shippingMethodStats", customerOrderRepository.getShippingMethodStats());

            advancedStats.put("mostUsedCoupons", mostUsedCoupons);
            advancedStats.put("fullyUsedCoupons", fullyUsedCoupons);
            advancedStats.put("unusedCoupons", unusedCoupons);
            advancedStats.put("expiredCoupons", expiredCoupons);
            advancedStats.put("averageCouponUsageRate", averageUsageRate != null ? averageUsageRate : 0.0);
            advancedStats.put("totalCoupons", couponRepository.count());
            advancedStats.put("activeCoupons", couponRepository.countByIsActiveTrue());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("advancedStats", advancedStats);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des statistiques avancées: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== UTILITAIRES ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "byomas-admin");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        response.put("authentication", "DISABLED");
        response.put("entities", Map.of(
                "products", productRepository.count(),
                "customers", customerRepository.count(),
                "messages", messageRepository.count(),
                "orders", customerOrderRepository.count(),
                "orderItems", orderItemRepository.count(),
                "categories", categoryRepository.count(),
                "coupons", couponRepository.count(),
                "adminUsers", adminUserRepository.count()
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("userName", System.getProperty("user.name"));
        systemInfo.put("serverTime", LocalDateTime.now());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
        systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("systemInfo", systemInfo);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/products/debug")
    public ResponseEntity<Map<String, Object>> debugProducts() {
        try {
            List<Product> products = productRepository.findAll();

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalProducts", products.size());
            debugInfo.put("products", products.stream().limit(5).collect(Collectors.toList()));
            debugInfo.put("databaseStatus", "OK");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("debug", debugInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Erreur base de données: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/products/test")
    public ResponseEntity<Map<String, Object>> testCreateProduct() {
        try {
            Product testProduct = new Product();
            testProduct.setName("Produit Test " + System.currentTimeMillis());
            testProduct.setDescription("Description de test");
            testProduct.setPrice(19.99);
            testProduct.setStockQuantity(10);
            testProduct.setCategory("Test");
            testProduct.setIsActive(true);
            testProduct.setCreatedAt(LocalDateTime.now());
            testProduct.setUpdatedAt(LocalDateTime.now());

            Product savedProduct = productRepository.save(testProduct);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produit test créé avec succès");
            response.put("productId", savedProduct.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Échec du test: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== GESTION DE STOCK SIMPLIFIÉE ====================

    @GetMapping("/stock/movements")
    public ResponseEntity<Map<String, Object>> getStockMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            List<Map<String, Object>> movements = new ArrayList<>();

            Map<String, Object> movement1 = new HashMap<>();
            movement1.put("id", 1);
            movement1.put("productId", 101);
            movement1.put("productName", "Savon Lavande");
            movement1.put("type", "INCREMENT");
            movement1.put("quantity", 50);
            movement1.put("previousStock", 10);
            movement1.put("newStock", 60);
            movement1.put("reason", "Réapprovisionnement");
            movement1.put("performedBy", "Admin");
            movement1.put("performedAt", LocalDateTime.now().minusDays(1).toString());
            movements.add(movement1);

            Map<String, Object> movement2 = new HashMap<>();
            movement2.put("id", 2);
            movement2.put("productId", 102);
            movement2.put("productName", "Huile Essentielle Menthe");
            movement2.put("type", "DECREMENT");
            movement2.put("quantity", 5);
            movement2.put("previousStock", 25);
            movement2.put("newStock", 20);
            movement2.put("reason", "Vente");
            movement2.put("performedBy", "Système");
            movement2.put("performedAt", LocalDateTime.now().minusHours(3).toString());
            movements.add(movement2);

            if (productId != null) {
                movements = movements.stream()
                        .filter(m -> productId.equals(m.get("productId")))
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", movements);
            response.put("count", movements.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des mouvements: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stock/alerts")
    public ResponseEntity<Map<String, Object>> getStockAlerts() {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();

            List<Product> lowStockProducts = productRepository.findLowStockProducts(10);

            for (Product product : lowStockProducts) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("id", product.getId());
                alert.put("productId", product.getId());
                alert.put("productName", product.getName());
                alert.put("currentStock", product.getStockQuantity());
                alert.put("threshold", 10);

                if (product.getStockQuantity() == 0) {
                    alert.put("status", "OUT_OF_STOCK");
                } else if (product.getStockQuantity() <= 5) {
                    alert.put("status", "CRITICAL");
                } else {
                    alert.put("status", "LOW");
                }

                alert.put("lastAlertDate", LocalDateTime.now().toString());
                alert.put("alertCount", 1);
                alert.put("notified", false);

                alerts.add(alert);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("count", alerts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des alertes: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stock/inventory-report")
    public ResponseEntity<Map<String, Object>> getInventoryReport() {
        try {
            Map<String, Object> report = new HashMap<>();

            report.put("totalProducts", productRepository.count());
            report.put("activeProducts", productRepository.countByIsActiveTrue());
            report.put("outOfStockProducts", productRepository.countByStockQuantity(0));
            report.put("lowStockProducts", productRepository.countByStockQuantityLessThan(10));

            List<Product> allProducts = productRepository.findAll();
            double totalStockValue = allProducts.stream()
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum();
            report.put("totalStockValue", totalStockValue);

            double totalSales = allProducts.stream()
                    .mapToInt(p -> p.getSalesCount() != null ? p.getSalesCount() : 0)
                    .sum();
            double totalStock = allProducts.stream()
                    .mapToInt(p -> p.getStockQuantity())
                    .sum();
            double turnoverRate = totalStock > 0 ? totalSales / totalStock : 0;
            report.put("stockTurnoverRate", turnoverRate);

            List<Product> bestSelling = productRepository.findTop5ByOrderBySalesCountDesc();
            report.put("bestSellingProducts", bestSelling);

            List<Product> lowStock = productRepository.findLowStockProducts(10);
            report.put("lowStockProductsList", lowStock);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la génération du rapport: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/stock/adjust/{productId}")
    public ResponseEntity<Map<String, Object>> adjustStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> adjustData) {

        try {
            Optional<Product> productOpt = productRepository.findById(productId);

            if (!productOpt.isPresent()) {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

            Product product = productOpt.get();
            Integer quantity = (Integer) adjustData.get("quantity");
            String reason = (String) adjustData.get("reason");

            if (quantity == null) {
                return createErrorResponse("Quantité requise", HttpStatus.BAD_REQUEST);
            }

            product.setStockQuantity(quantity);
            product.setUpdatedAt(LocalDateTime.now());
            Product updatedProduct = productRepository.save(product);

            System.out.println("Mouvement de stock - Produit: " + product.getName() +
                    ", Type: ADJUSTMENT, Quantité: " + quantity +
                    ", Raison: " + reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock ajusté avec succès");
            response.put("data", updatedProduct);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'ajustement du stock: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/stock/increment/{productId}")
    public ResponseEntity<Map<String, Object>> incrementStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> incrementData) {

        try {
            Optional<Product> productOpt = productRepository.findById(productId);

            if (!productOpt.isPresent()) {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

            Product product = productOpt.get();
            Integer quantity = (Integer) incrementData.get("quantity");
            String reason = (String) incrementData.get("reason");

            if (quantity == null || quantity <= 0) {
                return createErrorResponse("Quantité invalide", HttpStatus.BAD_REQUEST);
            }

            int newStock = product.getStockQuantity() + quantity;
            product.setStockQuantity(newStock);
            product.setUpdatedAt(LocalDateTime.now());
            Product updatedProduct = productRepository.save(product);

            System.out.println("Mouvement de stock - Produit: " + product.getName() +
                    ", Type: INCREMENT, Quantité: +" + quantity +
                    ", Nouveau stock: " + newStock + ", Raison: " + reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock augmenté de " + quantity + " unités");
            response.put("data", updatedProduct);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de l'incrémentation du stock: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/stock/decrement/{productId}")
    public ResponseEntity<Map<String, Object>> decrementStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> decrementData) {

        try {
            Optional<Product> productOpt = productRepository.findById(productId);

            if (!productOpt.isPresent()) {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

            Product product = productOpt.get();
            Integer quantity = (Integer) decrementData.get("quantity");
            String reason = (String) decrementData.get("reason");

            if (quantity == null || quantity <= 0) {
                return createErrorResponse("Quantité invalide", HttpStatus.BAD_REQUEST);
            }

            if (product.getStockQuantity() < quantity) {
                return createErrorResponse("Stock insuffisant. Stock disponible: " + product.getStockQuantity(),
                        HttpStatus.BAD_REQUEST);
            }

            int newStock = product.getStockQuantity() - quantity;
            product.setStockQuantity(newStock);
            product.setUpdatedAt(LocalDateTime.now());
            Product updatedProduct = productRepository.save(product);

            System.out.println("Mouvement de stock - Produit: " + product.getName() +
                    ", Type: DECREMENT, Quantité: -" + quantity +
                    ", Nouveau stock: " + newStock + ", Raison: " + reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stock diminué de " + quantity + " unités");
            response.put("data", updatedProduct);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la diminution du stock: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/stock/threshold/{productId}")
    public ResponseEntity<Map<String, Object>> setStockThreshold(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> thresholdData) {

        try {
            Optional<Product> productOpt = productRepository.findById(productId);

            if (!productOpt.isPresent()) {
                return createErrorResponse("Produit non trouvé", HttpStatus.NOT_FOUND);
            }

            Integer threshold = (Integer) thresholdData.get("threshold");

            if (threshold == null || threshold < 0) {
                return createErrorResponse("Seuil invalide", HttpStatus.BAD_REQUEST);
            }

            System.out.println("Seuil de stock défini pour produit " + productId + ": " + threshold);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Seuil de stock défini à " + threshold + " unités");
            response.put("threshold", threshold);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la définition du seuil: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/stock/bulk-update")
    public ResponseEntity<Map<String, Object>> bulkStockUpdate(
            @RequestBody Map<String, Object> bulkData) {

        try {
            List<Map<String, Object>> updates = (List<Map<String, Object>>) bulkData.get("updates");
            String reason = (String) bulkData.get("reason");

            if (updates == null || updates.isEmpty()) {
                return createErrorResponse("Aucune mise à jour fournie", HttpStatus.BAD_REQUEST);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int errorCount = 0;

            for (Map<String, Object> update : updates) {
                try {
                    Long productId = Long.parseLong(update.get("productId").toString());
                    Integer quantity = Integer.parseInt(update.get("quantity").toString());

                    Optional<Product> productOpt = productRepository.findById(productId);

                    if (productOpt.isPresent()) {
                        Product product = productOpt.get();
                        int currentStock = product.getStockQuantity();
                        int newStock = currentStock + quantity;

                        newStock = Math.max(0, newStock);

                        product.setStockQuantity(newStock);
                        product.setUpdatedAt(LocalDateTime.now());
                        productRepository.save(product);

                        Map<String, Object> result = new HashMap<>();
                        result.put("productId", productId);
                        result.put("productName", product.getName());
                        result.put("success", true);
                        result.put("newStock", newStock);
                        result.put("change", quantity);

                        successCount++;
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("productId", productId);
                        result.put("success", false);
                        result.put("error", "Produit non trouvé");

                        errorCount++;
                    }

                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("productId", update.get("productId"));
                    result.put("success", false);
                    result.put("error", e.getMessage());

                    errorCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Mise à jour groupée terminée: %d réussites, %d échecs",
                    successCount, errorCount));
            response.put("results", results);
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour groupée: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stock/history")
    public ResponseEntity<Map<String, Object>> getStockHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        try {
            List<Map<String, Object>> history = new ArrayList<>();

            for (int i = 1; i <= 10; i++) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", i);
                entry.put("productId", 100 + i);
                entry.put("productName", "Produit " + i);
                entry.put("type", i % 2 == 0 ? "INCREMENT" : "DECREMENT");
                entry.put("quantity", i * 10);
                entry.put("reason", i % 2 == 0 ? "Réapprovisionnement" : "Vente");
                entry.put("performedBy", "Admin");
                entry.put("performedAt", LocalDateTime.now().minusDays(i).toString());
                history.add(entry);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            response.put("count", history.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération de l'historique: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/low-stock/list")
    public ResponseEntity<Map<String, Object>> getLowStockProductsList() {
        try {
            List<Product> products = productRepository.findLowStockProducts(10);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits en stock faible: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/products/out-of-stock/list")
    public ResponseEntity<Map<String, Object>> getOutOfStockProductsList() {
        try {
            List<Product> products = productRepository.findOutOfStockProducts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("products", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des produits en rupture: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/stock/summary")
    public ResponseEntity<Map<String, Object>> getStockSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();

            summary.put("totalProducts", productRepository.count());
            summary.put("activeProducts", productRepository.countByIsActiveTrue());
            summary.put("outOfStock", productRepository.countByStockQuantity(0));
            summary.put("lowStock", productRepository.countByStockQuantityLessThan(10));
            summary.put("sufficientStock", productRepository.countByStockQuantityGreaterThanEqual(10));

            List<Product> allProducts = productRepository.findAll();
            double totalValue = allProducts.stream()
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum();
            summary.put("totalValue", totalValue);

            List<Product> needsAttention = productRepository.findLowStockProducts(15);
            summary.put("needsAttention", needsAttention);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du résumé: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/stock/generate-orders")
    public ResponseEntity<Map<String, Object>> generateStockOrders() {
        try {
            List<Product> lowStockProducts = productRepository.findLowStockProducts(10);
            List<Product> outOfStockProducts = productRepository.findOutOfStockProducts();

            List<Map<String, Object>> suggestedOrders = new ArrayList<>();

            for (Product product : outOfStockProducts) {
                Map<String, Object> order = new HashMap<>();
                order.put("productId", product.getId());
                order.put("productName", product.getName());
                order.put("currentStock", product.getStockQuantity());
                order.put("suggestedQuantity", 50);
                order.put("estimatedCost", product.getPrice() * 50);
                order.put("priority", "HIGH");
                suggestedOrders.add(order);
            }

            for (Product product : lowStockProducts) {
                if (product.getStockQuantity() > 0) {
                    Map<String, Object> order = new HashMap<>();
                    order.put("productId", product.getId());
                    order.put("productName", product.getName());
                    order.put("currentStock", product.getStockQuantity());
                    order.put("suggestedQuantity", 20);
                    order.put("estimatedCost", product.getPrice() * 20);
                    order.put("priority", "MEDIUM");
                    suggestedOrders.add(order);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Suggestions de commandes générées");
            response.put("suggestedOrders", suggestedOrders);
            response.put("count", suggestedOrders.size());
            response.put("outOfStockCount", outOfStockProducts.size());
            response.put("lowStockCount", lowStockProducts.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la génération des commandes: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("status", status.value());
        response.put("timestamp", System.currentTimeMillis());
        response.put("debugInfo", "Vérifiez les logs serveur pour plus de détails");
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex) {
        System.err.println("Erreur globale: " + ex.getMessage());
        ex.printStackTrace();
        return createErrorResponse("Erreur interne du serveur: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    private void logCouponUsage(Coupon coupon, String orderNumber, String customerEmail, String orderAmount) {
        try {
            System.out.println("📝 JOURNAL D'UTILISATION:");
            System.out.println("   - Date: " + LocalDateTime.now().format(DATE_FORMATTER));
            System.out.println("   - Coupon ID: " + coupon.getId());
            System.out.println("   - Coupon Code: " + coupon.getCode());
            System.out.println("   - Commande: " + (orderNumber != null ? orderNumber : "N/A"));
            System.out.println("   - Client: " + (customerEmail != null ? customerEmail : "N/A"));
            System.out.println("   - Montant: " + (orderAmount != null ? orderAmount : "N/A"));
            System.out.println("   - Total utilisations: " + coupon.getUsedCount());
            System.out.println("   - Restant: " + coupon.getRemainingUses());

        } catch (Exception e) {
            System.err.println("Erreur journalisation: " + e.getMessage());
        }
    }

    private double calculateDiscountAmount(Coupon coupon, double orderAmount) {
        if (orderAmount <= 0) return 0.0;

        String discountType = coupon.getDiscountType();
        double discountValue = coupon.getDiscountValue();

        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            return orderAmount * (discountValue / 100);
        } else if ("FIXED".equalsIgnoreCase(discountType)) {
            return Math.min(orderAmount, discountValue);
        } else if ("FREE_SHIPPING".equalsIgnoreCase(discountType)) {
            return 0;
        }
        return 0;
    }

    private String getCouponStatus(Coupon coupon) {
        if (!coupon.getIsActive()) {
            return "INACTIF";
        }
        if (coupon.isExpired()) {
            return "EXPIRÉ";
        }
        if (coupon.hasReachedMaxUses()) {
            return "ÉPUISÉ";
        }
        if (coupon.getMaxUses() != null &&
                (coupon.getMaxUses() - coupon.getUsedCount()) <= 3) {
            return "PRESQUE ÉPUISÉ";
        }
        return "ACTIF";
    }

    private String formatDiscount(Coupon coupon) {
        String discountType = coupon.getDiscountType();
        double discountValue = coupon.getDiscountValue();

        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            return String.format("%.0f%%", discountValue);
        } else if ("FIXED".equalsIgnoreCase(discountType)) {
            return String.format("%.3f DT", discountValue);
        } else if ("FREE_SHIPPING".equalsIgnoreCase(discountType)) {
            return "Livraison gratuite";
        }
        return String.valueOf(discountValue);
    }
}