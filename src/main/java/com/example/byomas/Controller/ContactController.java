package com.example.byomas.Controller;

import com.example.byomas.Entity.Message;
import com.example.byomas.Repository.MessageRepository;
import com.example.byomas.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EmailService emailService;

    // DTO pour les données de contact
    public static class ContactFormData {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String subject;
        private String message;

        // Getters et Setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    /**
     * Endpoint pour recevoir les messages de contact
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createContactMessage(@RequestBody ContactFormData contactData) {
        try {
            // Validation des données
            if (contactData.getFirstName() == null || contactData.getFirstName().trim().isEmpty()) {
                return createErrorResponse("Le prénom est obligatoire", HttpStatus.BAD_REQUEST);
            }
            if (contactData.getLastName() == null || contactData.getLastName().trim().isEmpty()) {
                return createErrorResponse("Le nom est obligatoire", HttpStatus.BAD_REQUEST);
            }
            if (contactData.getEmail() == null || contactData.getEmail().trim().isEmpty()) {
                return createErrorResponse("L'email est obligatoire", HttpStatus.BAD_REQUEST);
            }
            if (contactData.getSubject() == null || contactData.getSubject().trim().isEmpty()) {
                return createErrorResponse("Le sujet est obligatoire", HttpStatus.BAD_REQUEST);
            }
            if (contactData.getMessage() == null || contactData.getMessage().trim().isEmpty()) {
                return createErrorResponse("Le message est obligatoire", HttpStatus.BAD_REQUEST);
            }

            // Créer et sauvegarder le message
            Message message = new Message();
            message.setName(contactData.getFirstName() + " " + contactData.getLastName());
            message.setEmail(contactData.getEmail());
            message.setPhone(contactData.getPhone() != null ? contactData.getPhone() : "");
            message.setSubject(contactData.getSubject());
            message.setMessage(contactData.getMessage());
            message.setStatus("NEW");
            message.setPriority(determinePriority(contactData.getSubject(), contactData.getMessage()));
            message.setIsRead(false);
            message.setIsReplied(false);
            message.setCreatedAt(LocalDateTime.now());
            message.setUpdatedAt(LocalDateTime.now());

            Message savedMessage = messageRepository.save(message);

            // Notifier l'admin par email (version simple)
            try {
                emailService.sendNewContactNotification(
                        savedMessage.getEmail(),
                        savedMessage.getName(),
                        savedMessage.getSubject(),
                        savedMessage.getMessage(),
                        savedMessage.getPhone()
                );
            } catch (Exception emailEx) {
                System.out.println("⚠️ Email de notification non envoyé: " + emailEx.getMessage());
                // Continuer même si l'email échoue
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message envoyé avec succès");
            response.put("contactId", savedMessage.getId());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi du message de contact: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Erreur lors de l'envoi du message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Détermine la priorité en fonction du sujet et du message
     */
    private String determinePriority(String subject, String message) {
        if (subject == null || message == null) {
            return "MEDIUM";
        }

        String lowerSubject = subject.toLowerCase();
        String lowerMessage = message.toLowerCase();

        // Mots-clés pour priorité URGENT
        if (lowerSubject.contains("urgent") || lowerSubject.contains("important") ||
                lowerMessage.contains("urgent") || lowerMessage.contains("important") ||
                lowerMessage.contains("problème") || lowerMessage.contains("erreur") ||
                lowerMessage.contains("bug") || lowerMessage.contains("ne fonctionne pas") ||
                lowerMessage.contains("cassé") || lowerMessage.contains("urgence")) {
            return "URGENT";
        }

        // Mots-clés pour priorité HIGH
        if (lowerSubject.contains("commande") || lowerSubject.contains("livraison") ||
                lowerSubject.contains("paiement") || lowerSubject.contains("réclamation") ||
                lowerSubject.contains("remboursement") || lowerSubject.contains("annulation")) {
            return "HIGH";
        }

        return "MEDIUM";
    }

    /**
     * Récupère tous les messages de contact (pour l'admin)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllContactMessages() {
        try {
            List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            response.put("unreadCount", messageRepository.countByIsReadFalse());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des messages", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Récupère un message spécifique
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getContactMessage(@PathVariable Long id) {
        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", messageOpt.get());
                return ResponseEntity.ok(response);
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération du message", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Met à jour le statut d'un message
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateMessageStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {

        try {
            Optional<Message> messageOpt = messageRepository.findById(id);

            if (messageOpt.isPresent()) {
                Message message = messageOpt.get();
                String newStatus = statusUpdate.get("status");

                if (newStatus != null &&
                        (newStatus.equals("NEW") || newStatus.equals("READ") ||
                                newStatus.equals("REPLIED") || newStatus.equals("ARCHIVED"))) {

                    message.setStatus(newStatus);
                    message.setUpdatedAt(LocalDateTime.now());

                    if (newStatus.equals("READ")) {
                        message.setIsRead(true);
                    } else if (newStatus.equals("REPLIED")) {
                        message.setIsReplied(true);
                        message.setRepliedAt(LocalDateTime.now());
                    }

                    Message updatedMessage = messageRepository.save(message);

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Statut mis à jour avec succès");
                    response.put("updatedMessage", updatedMessage);

                    return ResponseEntity.ok(response);
                } else {
                    return createErrorResponse("Statut invalide", HttpStatus.BAD_REQUEST);
                }
            } else {
                return createErrorResponse("Message non trouvé", HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la mise à jour du statut", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Supprime un message
     */
    @DeleteMapping("/{id}")
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
            return createErrorResponse("Erreur lors de la suppression du message", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Recherche de messages
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMessages(@RequestParam String query) {
        try {
            List<Message> messages = messageRepository.findByEmailContainingOrNameContainingOrSubjectContainingOrMessageContaining(
                    query, query, query, query);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            response.put("query", query);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la recherche", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Récupère les messages par statut
     */
    @GetMapping("/by-status")
    public ResponseEntity<Map<String, Object>> getMessagesByStatus(@RequestParam String status) {
        try {
            List<Message> messages = messageRepository.findByStatusOrderByCreatedAtDesc(status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            response.put("status", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des messages", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Récupère les statistiques des messages
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMessageStats() {
        try {
            long totalMessages = messageRepository.count();
            long unreadMessages = messageRepository.countByIsReadFalse();
            long repliedMessages = messageRepository.countByIsRepliedTrue();
            long urgentMessages = messageRepository.countByPriority("URGENT");

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMessages", totalMessages);
            stats.put("unreadMessages", unreadMessages);
            stats.put("repliedMessages", repliedMessages);
            stats.put("urgentMessages", urgentMessages);
            stats.put("responseRate", totalMessages > 0 ? (double) repliedMessages / totalMessages * 100 : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);
            response.put("lastUpdated", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur lors de la récupération des statistiques", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Test endpoint pour vérifier l'API
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Contact API is working");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}