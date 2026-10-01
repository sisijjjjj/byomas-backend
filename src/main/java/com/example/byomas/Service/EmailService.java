package com.example.byomas.Service;

import com.example.byomas.Entity.ContactMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public EmailService(JavaMailSender javaMailSender,
                        TemplateEngine templateEngine,
                        @Value("${spring.mail.username:sirinemimouni97@gmail.com}") String username) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = username;
        logger.info("🚀 EmailService initialisé avec fromEmail: {}", fromEmail);
    }

    // ==================== MÉTHODE PRINCIPALE D'ENVOI ====================

    /**
     * Envoie un email simple
     */
    public boolean sendSimpleEmail(String toEmail, String subject, String text) {
        try {
            logger.info("📧 Début envoi email - De: {}, À: {}, Sujet: {}", fromEmail, toEmail, subject);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);

            javaMailSender.send(mailMessage);

            logger.info("✅ Email envoyé avec succès - De: {}, À: {}", fromEmail, toEmail);
            return true;

        } catch (Exception e) {
            logger.error("❌ Échec envoi email - De: {}, À: {}, Erreur: {}", fromEmail, toEmail, e.getMessage());
            logger.debug("Stack trace:", e);
            return false;
        }
    }

    /**
     * Envoie un email avec CC et BCC
     */
    public boolean sendEmailWithCC(String toEmail, String ccEmail, String subject, String text) {
        try {
            logger.info("📧 Envoi email avec CC - De: {}, À: {}, CC: {}", fromEmail, toEmail, ccEmail);

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setCc(ccEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);

            javaMailSender.send(mailMessage);

            logger.info("✅ Email avec CC envoyé avec succès");
            return true;

        } catch (Exception e) {
            logger.error("❌ Échec envoi email avec CC: {}", e.getMessage());
            return false;
        }
    }

    // ==================== EMAILS DE COMMANDES ====================

    /**
     * Envoie un email de confirmation de commande
     */
    public boolean sendOrderConfirmation(String toEmail, String customerName, String orderNumber,
                                         Double totalAmount, String status) {
        try {
            logger.info("📦 Envoi confirmation commande - Commande: {}, Client: {}", orderNumber, customerName);

            String subject = "Confirmation de votre commande #" + orderNumber;
            String text = String.format(
                    """
                    Bonjour %s,
                    
                    Votre commande #%s a été %s.
                    
                    📋 **Détails de la commande:**
                    Numéro de commande: %s
                    Montant total: %.2f TND
                    Statut: %s
                    Date: %s
                    
                    Nous vous tiendrons informé de l'avancement de votre commande.
                    
                    Merci pour votre confiance !
                    L'équipe Byomas
                    
                    ---
                    Pour toute question, contactez-nous à: %s
                    Date d'envoi: %s
                    """,
                    customerName, orderNumber, getStatusText(status), orderNumber,
                    totalAmount, getStatusFrench(status),
                    LocalDateTime.now().format(formatter),
                    fromEmail,
                    LocalDateTime.now().format(formatter)
            );

            boolean sent = sendSimpleEmail(toEmail, subject, text);

            if (sent) {
                logger.info("✅ Confirmation commande #{} envoyée à {}", orderNumber, toEmail);
            } else {
                logger.warn("⚠️ Échec envoi confirmation commande #{} à {}", orderNumber, toEmail);
            }

            return sent;

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la confirmation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie un récapitulatif de commande détaillé
     */
    public boolean sendOrderSummary(String toEmail, String customerName, String orderNumber,
                                    String orderDate, String status, Double totalAmount) {
        try {
            logger.info("📋 Envoi récapitulatif commande: {}", orderNumber);

            String subject = "Récapitulatif de votre commande #" + orderNumber;
            String text = String.format(
                    """
                    Bonjour %s,
                    
                    Voici le récapitulatif de votre commande #%s:
                    
                    📦 **DÉTAILS DE LA COMMANDE:**
                    ====================
                    Numéro de commande: %s
                    Date: %s
                    Statut: %s
                    
                    💰 **RÉSUMÉ FINANCIER:**
                    ================
                    TOTAL: %.2f TND
                    
                    Merci pour votre commande !
                    L'équipe Byomas
                    
                    ---
                    Pour suivre votre commande: http://localhost:4200/track-order/%s
                    Contact: %s
                    Date: %s
                    """,
                    customerName, orderNumber, orderNumber, orderDate,
                    getStatusFrench(status), totalAmount, orderNumber,
                    fromEmail,
                    LocalDateTime.now().format(formatter)
            );

            return sendSimpleEmail(toEmail, subject, text);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi du récapitulatif: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie un email d'expédition avec numéro de suivi
     */
    public boolean sendShippingNotification(String toEmail, String customerName, String orderNumber,
                                            String trackingNumber, String shippingMethod) {
        try {
            logger.info("🚚 Envoi notification expédition - Commande: {}, Tracking: {}", orderNumber, trackingNumber);

            String subject = "Votre commande #" + orderNumber + " a été expédiée";
            String text = String.format(
                    """
                    Bonjour %s,
                    
                    Votre commande #%s a été expédiée !
                    
                    📦 **Informations d'expédition:**
                    Numéro de commande: %s
                    Numéro de suivi: %s
                    Méthode de livraison: %s
                    Date d'expédition: %s
                    
                    Vous pouvez suivre votre colis avec le numéro de suivi ci-dessus.
                    
                    Livraison estimée: 2-3 jours ouvrables
                    
                    Merci pour votre patience !
                    L'équipe Byomas
                    
                    ---
                    Contact: %s
                    Date: %s
                    """,
                    customerName, orderNumber, orderNumber, trackingNumber,
                    shippingMethod, LocalDateTime.now().format(formatter),
                    fromEmail,
                    LocalDateTime.now().format(formatter)
            );

            return sendSimpleEmail(toEmail, subject, text);

        } catch (Exception e) {
            logger.error("❌ Erreur envoi notification expédition: {}", e.getMessage());
            return false;
        }
    }

    // ==================== EMAILS DE CONTACT ====================

    /**
     * Envoie une notification à l'admin pour un nouveau message de contact
     */
    public boolean sendNewContactNotification(String fromEmail, String fromName, String subject,
                                              String message, String phone) {
        try {
            logger.info("📩 Notification contact reçu de: {} ({})", fromName, fromEmail);

            String adminSubject = "Nouveau message de contact - " + subject;
            String adminText = String.format(
                    """
                    Nouveau message de contact reçu:
                    
                    👤 **Informations client:**
                    Nom: %s
                    Email: %s
                    Téléphone: %s
                    
                    📝 **Message:**
                    Sujet: %s
                    
                    %s
                    
                    🔗 **Actions:**
                    Connectez-vous à l'admin pour répondre: http://localhost:4200/admin/messages
                    
                    📅 **Date de réception:** %s
                    """,
                    fromName, fromEmail,
                    (phone != null && !phone.isEmpty() ? phone : "Non fourni"),
                    subject, message,
                    LocalDateTime.now().format(formatter)
            );

            boolean sent = sendSimpleEmail(this.fromEmail, adminSubject, adminText);

            if (sent) {
                logger.info("✅ Notification admin envoyée pour message de {}", fromEmail);
            }

            return sent;

        } catch (Exception e) {
            logger.error("❌ Erreur envoi notification admin: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une réponse au client
     */
    public boolean sendMessageResponse(String toEmail, String customerName, String originalSubject,
                                       String responseText) {
        try {
            logger.info("📤 Envoi réponse à: {} ({})", customerName, toEmail);

            String subject = "Re: " + originalSubject;
            String text = String.format(
                    """
                    Bonjour %s,
                    
                    Merci d'avoir contacté Byomas. Voici notre réponse à votre message:
                    
                    %s
                    
                    Cordialement,
                    L'équipe Byomas
                    
                    ---
                    Cet email est envoyé en réponse à votre demande de contact.
                    Date: %s
                    """,
                    customerName, responseText, LocalDateTime.now().format(formatter)
            );

            boolean sent = sendSimpleEmail(toEmail, subject, text);

            if (sent) {
                logger.info("✅ Réponse envoyée à {}", toEmail);
            }

            return sent;

        } catch (Exception e) {
            logger.error("❌ Erreur envoi réponse: {}", e.getMessage());
            return false;
        }
    }

    // ==================== MÉTHODES DE TEST ET DIAGNOSTIC ====================

    /**
     * Test de la configuration email
     */
    public Map<String, Object> testEmailConfiguration() {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            logger.info("🔧 Test de configuration email en cours...");

            String testSubject = "✅ Test de configuration email Byomas";
            String testText = String.format(
                    """
                    Ceci est un email de test pour vérifier la configuration SMTP.
                    
                    📋 **Informations de configuration:**
                    Serveur: smtp.gmail.com:587
                    Email expéditeur: %s
                    Statut: EN COURS DE TEST
                    
                    Si vous recevez cet email, la configuration SMTP fonctionne correctement !
                    
                    📅 **Date du test:** %s
                    """,
                    fromEmail,
                    LocalDateTime.now().format(formatter)
            );

            boolean sent = sendSimpleEmail(fromEmail, testSubject, testText);

            result.put("success", sent);
            result.put("fromEmail", fromEmail);
            result.put("testEmail", fromEmail);
            result.put("timestamp", LocalDateTime.now().format(formatter));
            result.put("message", sent ?
                    "Test d'email envoyé avec succès" :
                    "Échec de l'envoi du test d'email");

            if (sent) {
                logger.info("✅ Test de configuration réussi");
            } else {
                logger.warn("⚠️ Test de configuration échoué");
            }

        } catch (Exception e) {
            logger.error("❌ Erreur lors du test de configuration: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("fromEmail", fromEmail);
        }

        return result;
    }

    /**
     * Envoie une notification de contact
     */
    public boolean sendContactNotification(ContactMessage contactMessage) {
        try {
            if (contactMessage == null) {
                logger.error("❌ Message de contact null");
                return false;
            }

            logger.info("📨 Traitement notification contact ID: {}", contactMessage.getId());

            return sendNewContactNotification(
                    contactMessage.getEmail(),
                    contactMessage.getName(),
                    contactMessage.getSubject(),
                    contactMessage.getMessage(),
                    contactMessage.getPhone()
            );

        } catch (Exception e) {
            logger.error("❌ Erreur notification contact: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si la configuration email est valide
     */
    public Map<String, Object> validateEmailConfiguration() {
        Map<String, Object> validation = new java.util.HashMap<>();

        validation.put("fromEmail", fromEmail);
        validation.put("isValidEmail", isValidEmail(fromEmail));
        validation.put("timestamp", LocalDateTime.now().format(formatter));

        // Test de connexion simple
        try {
            // Vérifier si le JavaMailSender est configuré
            validation.put("javaMailSenderConfigured", javaMailSender != null);

            // Tester un ping au serveur SMTP
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(fromEmail);
            testMessage.setTo(fromEmail);
            testMessage.setSubject("Validation SMTP");
            testMessage.setText("Validation en cours...");

            javaMailSender.send(testMessage);
            validation.put("smtpConnection", "SUCCESS");
            validation.put("overallStatus", "VALID");

        } catch (Exception e) {
            validation.put("smtpConnection", "FAILED: " + e.getMessage());
            validation.put("overallStatus", "INVALID");
            validation.put("error", e.getMessage());
        }

        return validation;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private String getStatusText(String status) {
        if (status == null) return "traitée";

        switch (status.toUpperCase()) {
            case "PENDING": return "reçue et est en attente de traitement";
            case "CONFIRMED": return "confirmée";
            case "PROCESSING": return "est en cours de préparation";
            case "SHIPPED": return "a été expédiée";
            case "DELIVERED": return "a été livrée";
            case "CANCELLED": return "a été annulée";
            default: return "traitée";
        }
    }

    private String getStatusFrench(String status) {
        if (status == null) return "Inconnu";

        switch (status.toUpperCase()) {
            case "PENDING": return "En attente";
            case "CONFIRMED": return "Confirmée";
            case "PROCESSING": return "En traitement";
            case "SHIPPED": return "Expédiée";
            case "DELIVERED": return "Livrée";
            case "CANCELLED": return "Annulée";
            default: return status;
        }
    }

    /**
     * Vérifie si l'email est valide
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Envoie un email de bienvenue
     */
    public boolean sendWelcomeEmail(String toEmail, String customerName) {
        try {
            logger.info("🎉 Envoi email de bienvenue à: {}", customerName);

            String subject = "Bienvenue chez Byomas !";
            String text = String.format(
                    """
                    Bonjour %s,
                    
                    Bienvenue dans la communauté Byomas !
                    
                    Nous sommes ravis de vous compter parmi nos clients.
                    Découvrez nos produits naturels et bénéficiez d'une réduction de 10%% 
                    sur votre première commande avec le code: BIENVENUE10
                    
                    À très bientôt sur notre boutique !
                    
                    Cordialement,
                    L'équipe Byomas
                    
                    ---
                    Site: http://localhost:4200
                    Email: %s
                    Date: %s
                    """,
                    customerName, fromEmail, LocalDateTime.now().format(formatter)
            );

            return sendSimpleEmail(toEmail, subject, text);

        } catch (Exception e) {
            logger.error("❌ Erreur envoi email de bienvenue: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtenir l'email expéditeur configuré
     */
    public String getFromEmail() {
        return fromEmail;
    }

    /**
     * Log les informations de configuration
     */
    public void logConfiguration() {
        logger.info("📧 Configuration EmailService:");
        logger.info("  - From Email: {}", fromEmail);
        logger.info("  - JavaMailSender: {}", javaMailSender != null ? "Configured" : "NULL");
        logger.info("  - TemplateEngine: {}", templateEngine != null ? "Configured" : "NULL");
    }
}