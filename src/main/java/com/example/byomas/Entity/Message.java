package com.example.byomas.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isReplied = false;

    @Column(nullable = false)
    private String status = "NEW"; // NEW, READ, REPLIED, ARCHIVED

    @Column(nullable = false)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String phone;

    private String company;

    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    private LocalDateTime repliedAt;

    // Champs supplémentaires pour le suivi
    private String ipAddress;
    private String userAgent;
    private String pageSource; // Page d'où provient le message

    // Constructeurs
    public Message() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isRead = false;
        this.isReplied = false;
        this.status = "NEW";
        this.priority = "MEDIUM";
    }

    public Message(String name, String email, String subject, String message) {
        this();
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.determinePriority(); // Déterminer la priorité automatiquement
    }

    public Message(String name, String email, String phone, String subject, String message) {
        this(name, email, subject, message);
        this.phone = phone;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) {
        this.subject = subject;
        this.updatedAt = LocalDateTime.now();
        this.determinePriority(); // Recalculer la priorité si le sujet change
    }

    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
        this.updatedAt = LocalDateTime.now();
        this.determinePriority(); // Recalculer la priorité si le message change
    }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
        this.updatedAt = LocalDateTime.now();
        if (isRead && "NEW".equals(this.status)) {
            this.status = "READ";
        }
    }

    public Boolean getIsReplied() { return isReplied; }
    public void setIsReplied(Boolean isReplied) {
        this.isReplied = isReplied;
        this.updatedAt = LocalDateTime.now();
        if (isReplied) {
            this.status = "REPLIED";
            if (this.repliedAt == null) {
                this.repliedAt = LocalDateTime.now();
            }
        }
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPriority() { return priority; }
    public void setPriority(String priority) {
        if (priority != null && (priority.equals("LOW") || priority.equals("MEDIUM") ||
                priority.equals("HIGH") || priority.equals("URGENT"))) {
            this.priority = priority;
        } else {
            this.priority = "MEDIUM";
        }
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCompany() { return company; }
    public void setCompany(String company) {
        this.company = company;
        this.updatedAt = LocalDateTime.now();
    }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
        this.setIsReplied(true);
        this.updatedAt = LocalDateTime.now();
    }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getPageSource() { return pageSource; }
    public void setPageSource(String pageSource) { this.pageSource = pageSource; }

    // Méthodes utilitaires
    public void markAsRead() {
        this.setIsRead(true);
    }

    public void markAsReplied() {
        this.setIsReplied(true);
    }

    public void respond(String response) {
        this.setAdminResponse(response);
        this.setIsReplied(true);
    }

    public void respond(String response, String adminNotes) {
        this.setAdminResponse(response);
        this.setAdminNotes(adminNotes);
        this.setIsReplied(true);
    }

    public void determinePriority() {
        String content = (this.subject + " " + this.message).toLowerCase();

        if (content.contains("urgent") || content.contains("asap") ||
                content.contains("emergency") || content.contains("critical")) {
            this.priority = "URGENT";
        } else if (content.contains("important") || content.contains("high") ||
                content.contains("problème") || content.contains("problem") ||
                content.contains("erreur") || content.contains("error")) {
            this.priority = "HIGH";
        } else if (content.contains("question") || content.contains("info") ||
                content.contains("information") || content.contains("demande")) {
            this.priority = "MEDIUM";
        } else {
            this.priority = "LOW";
        }
    }

    public boolean isUrgent() {
        return "URGENT".equals(this.priority) ||
                (subject != null && subject.toLowerCase().contains("urgent")) ||
                (message != null && message.toLowerCase().contains("urgent"));
    }

    public boolean requiresImmediateAttention() {
        return "URGENT".equals(this.priority) || "HIGH".equals(this.priority);
    }

    public boolean isNew() {
        return "NEW".equals(this.status);
    }

    public boolean canBeArchived() {
        return "REPLIED".equals(this.status) ||
                ("READ".equals(this.status) && !this.isReplied && this.createdAt.isBefore(LocalDateTime.now().minusDays(30)));
    }

    public String getPriorityInFrench() {
        switch (this.priority) {
            case "LOW": return "Basse";
            case "MEDIUM": return "Moyenne";
            case "HIGH": return "Haute";
            case "URGENT": return "Urgente";
            default: return this.priority;
        }
    }

    public String getStatusInFrench() {
        switch (this.status) {
            case "NEW": return "Nouveau";
            case "READ": return "Lu";
            case "REPLIED": return "Répondu";
            case "ARCHIVED": return "Archivé";
            default: return this.status;
        }
    }

    public long getDaysSinceCreation() {
        return java.time.Duration.between(this.createdAt, LocalDateTime.now()).toDays();
    }

    public long getHoursSinceCreation() {
        return java.time.Duration.between(this.createdAt, LocalDateTime.now()).toHours();
    }

    public void escalatePriority() {
        switch (this.priority) {
            case "LOW":
                this.priority = "MEDIUM";
                break;
            case "MEDIUM":
                this.priority = "HIGH";
                break;
            case "HIGH":
            case "URGENT":
                this.priority = "URGENT";
                break;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = "ARCHIVED";
        this.updatedAt = LocalDateTime.now();
    }

    public void restore() {
        if ("ARCHIVED".equals(this.status)) {
            this.status = this.isReplied ? "REPLIED" : "READ";
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Callbacks JPA
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (isReplied == null) {
            isReplied = false;
        }
        if (status == null) {
            status = "NEW";
        }
        if (priority == null) {
            this.determinePriority();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", isRead=" + isRead +
                ", isReplied=" + isReplied +
                ", createdAt=" + createdAt +
                ", daysSinceCreation=" + getDaysSinceCreation() +
                '}';
    }

    // Méthodes equals et hashCode pour une bonne gestion des collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;

        Message message = (Message) o;

        if (getId() != null ? !getId().equals(message.getId()) : message.getId() != null) return false;
        return getEmail() != null ? getEmail().equals(message.getEmail()) : message.getEmail() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getEmail() != null ? getEmail().hashCode() : 0);
        return result;
    }
}