package com.example.byomas.Repository;

import com.example.byomas.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ==================== MÉTHODES SIMPLES ====================

    // Trouver les messages non lus
    List<Message> findByIsReadFalse();

    // Trouver les messages lus
    List<Message> findByIsReadTrue();

    // Trouver les messages avec réponse
    List<Message> findByIsRepliedTrue();

    // Trouver les messages sans réponse
    List<Message> findByIsRepliedFalse();

    // Trouver les messages par email
    List<Message> findByEmail(String email);

    // Trouver les messages par sujet (recherche insensible à la casse)
    List<Message> findBySubjectContainingIgnoreCase(String subject);

    // Trouver les messages par nom (recherche insensible à la casse)
    List<Message> findByNameContainingIgnoreCase(String name);

    // Compter les messages non lus
    long countByIsReadFalse();

    // Compter les messages sans réponse
    long countByIsRepliedFalse();

    // Trouver les messages par statut
    List<Message> findByStatus(String status);

    // Compter les messages par statut
    long countByStatus(String status);

    // Trouver les messages par statut triés par date
    List<Message> findByStatusOrderByCreatedAtDesc(String status);

    // Trouver tous les messages triés par date de création (décroissant)
    List<Message> findAllByOrderByCreatedAtDesc();

    // Trouver les messages non lus triés par date de création (décroissant)
    List<Message> findByIsReadFalseOrderByCreatedAtDesc();

    // Trouver les messages sans réponse triés par date de création (décroissant)
    List<Message> findByIsRepliedFalseOrderByCreatedAtDesc();

    // Trouver les messages par priorité
    List<Message> findByPriorityOrderByCreatedAtDesc(String priority);

    // Compter par priorité
    long countByPriority(String priority);

    // ==================== MÉTHODES AVEC @Query CORRIGÉES ====================

    // CORRECTION : Trouver les messages d'aujourd'hui (version corrigée)
    @Query("SELECT m FROM Message m WHERE CAST(m.createdAt AS DATE) = CURRENT_DATE ORDER BY m.createdAt DESC")
    List<Message> findTodayMessages();

    // CORRECTION : Compter les messages d'aujourd'hui (version corrigée)
    @Query("SELECT COUNT(m) FROM Message m WHERE CAST(m.createdAt AS DATE) = CURRENT_DATE")
    long countTodayMessages();

    // OU alternative plus simple (recommandée) :
    // Cette méthode remplace countTodayMessages()
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Trouver les messages de cette semaine
    @Query("SELECT m FROM Message m WHERE m.createdAt >= :startOfWeek ORDER BY m.createdAt DESC")
    List<Message> findThisWeekMessages(@Param("startOfWeek") LocalDateTime startOfWeek);

    // Méthode utilitaire pour les messages de cette semaine
    default List<Message> findThisWeekMessages() {
        return findThisWeekMessages(LocalDateTime.now().minusDays(7));
    }

    // Compter les messages de cette semaine
    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :startOfWeek")
    long countThisWeekMessages(@Param("startOfWeek") LocalDateTime startOfWeek);

    // Méthode utilitaire pour compter les messages de cette semaine
    default long countThisWeekMessages() {
        return countThisWeekMessages(LocalDateTime.now().minusDays(7));
    }

    // Trouver les messages de ce mois
    @Query("SELECT m FROM Message m WHERE m.createdAt >= :startOfMonth ORDER BY m.createdAt DESC")
    List<Message> findThisMonthMessages(@Param("startOfMonth") LocalDateTime startOfMonth);

    // Méthode utilitaire pour les messages de ce mois
    default List<Message> findThisMonthMessages() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return findThisMonthMessages(startOfMonth);
    }

    // Compter les messages de ce mois
    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :startOfMonth")
    long countThisMonthMessages(@Param("startOfMonth") LocalDateTime startOfMonth);

    // Méthode utilitaire pour compter les messages de ce mois
    default long countThisMonthMessages() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return countThisMonthMessages(startOfMonth);
    }

    // ==================== AUTRES MÉTHODES EXISTANTES CORRIGÉES ====================

    // Trouver les messages récents (des 7 derniers jours)
    @Query("SELECT m FROM Message m WHERE m.createdAt >= :startDate ORDER BY m.createdAt DESC")
    List<Message> findRecentMessages(@Param("startDate") LocalDateTime startDate);

    // Méthode utilitaire pour les 7 derniers jours
    default List<Message> findRecentMessages() {
        return findRecentMessages(LocalDateTime.now().minusDays(7));
    }

    // Trouver les messages par statut de lecture et de réponse
    List<Message> findByIsReadAndIsReplied(boolean isRead, boolean isReplied);

    // Recherche de messages par contenu (CORRIGÉE)
    @Query("SELECT m FROM Message m WHERE LOWER(m.message) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Message> searchMessages(@Param("keyword") String keyword);

    // Trouver les messages non lus et sans réponse
    @Query("SELECT m FROM Message m WHERE m.isRead = false AND m.isReplied = false ORDER BY m.createdAt DESC")
    List<Message> findUnreadAndUnrepliedMessages();

    // Compter les messages par période
    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate")
    long countMessagesByPeriod(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    // Trouver les messages urgents (contenant des mots-clés urgents)
    @Query("SELECT m FROM Message m WHERE LOWER(m.subject) LIKE LOWER(CONCAT('%', :urgentKeyword, '%')) OR " +
            "LOWER(m.message) LIKE LOWER(CONCAT('%', :urgentKeyword, '%')) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findUrgentMessages(@Param("urgentKeyword") String urgentKeyword);

    // Recherche avancée avec multiple critères (CORRIGÉE)
    @Query("SELECT m FROM Message m WHERE " +
            "(:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(m.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:subject IS NULL OR LOWER(m.subject) LIKE LOWER(CONCAT('%', :subject, '%'))) AND " +
            "(:isRead IS NULL OR m.isRead = :isRead) AND " +
            "(:isReplied IS NULL OR m.isReplied = :isReplied) AND " +
            "(:status IS NULL OR m.status = :status) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findMessagesByMultipleCriteria(@Param("name") String name,
                                                 @Param("email") String email,
                                                 @Param("subject") String subject,
                                                 @Param("isRead") Boolean isRead,
                                                 @Param("isReplied") Boolean isReplied,
                                                 @Param("status") String status);

    // Trouver les messages prioritaires (NEW et non lus)
    @Query("SELECT m FROM Message m WHERE m.status = 'NEW' AND m.isRead = false ORDER BY m.createdAt DESC")
    List<Message> findPriorityMessages();

    // Trouver les messages urgents par statut
    @Query("SELECT m FROM Message m WHERE m.status = 'URGENT' ORDER BY m.createdAt DESC")
    List<Message> findUrgentMessagesByStatus();

    // Compter les messages urgents
    @Query("SELECT COUNT(m) FROM Message m WHERE m.status = 'URGENT'")
    long countUrgentMessages();

    // Trouver les messages importants
    @Query("SELECT m FROM Message m WHERE m.status = 'IMPORTANT' ORDER BY m.createdAt DESC")
    List<Message> findImportantMessages();

    // Compter les messages importants
    @Query("SELECT COUNT(m) FROM Message m WHERE m.status = 'IMPORTANT'")
    long countImportantMessages();

    // Méthodes de mise à jour avec @Modifying et @Transactional
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true, m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :id")
    void markAsRead(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isReplied = true, m.status = 'REPLIED', m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :id")
    void markAsReplied(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.status = :status, m.updatedAt = CURRENT_TIMESTAMP WHERE m.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    @Modifying
    @Transactional
    @Query("DELETE FROM Message m WHERE m.createdAt < :cutoffDate")
    void deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Méthode utilitaire pour supprimer les messages de plus d'un an
    default void deleteMessagesOlderThanOneYear() {
        deleteOldMessages(LocalDateTime.now().minusYears(1));
    }

    // Trouver les messages par plage de dates
    @Query("SELECT m FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate ORDER BY m.createdAt DESC")
    List<Message> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Statistiques avancées
    @Query("SELECT m.status, COUNT(m) FROM Message m GROUP BY m.status ORDER BY COUNT(m) DESC")
    List<Object[]> countMessagesByStatusGroup();

    // Trouver les messages avec pagination (version simplifiée)
    @Query(value = "SELECT * FROM messages ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Message> findMessagesWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    // Compter les messages non lus par statut
    @Query("SELECT COUNT(m) FROM Message m WHERE m.isRead = false AND m.status = :status")
    long countUnreadMessagesByStatus(@Param("status") String status);

    // Trouver les messages non lus par statut
    @Query("SELECT m FROM Message m WHERE m.isRead = false AND m.status = :status ORDER BY m.createdAt DESC")
    List<Message> findUnreadMessagesByStatus(@Param("status") String status);

    long countByIsRepliedTrue();

    // ==================== NOUVELLES MÉTHODES UTILES ====================

    // Recherche générique (alternative)
    @Query("SELECT m FROM Message m WHERE " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(m.message) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Message> findByEmailContainingOrNameContainingOrSubjectContainingOrMessageContaining(@Param("query") String query);

    // Méthodes pour compter par jour
    @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :startOfDay AND m.createdAt < :endOfDay")
    long countMessagesForDate(@Param("startOfDay") LocalDateTime startOfDay,
                              @Param("endOfDay") LocalDateTime endOfDay);

    // Compter les messages d'aujourd'hui (version alternative)
    default long countTodaysMessages() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return countMessagesForDate(startOfDay, endOfDay);
    }

    // Méthode utilitaire pour obtenir la date du début du jour
    default LocalDateTime getStartOfDay() {
        return LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
    }

    // Méthode utilitaire pour obtenir la date de fin du jour
    default LocalDateTime getEndOfDay() {
        return LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
    }

    List<Message> findByEmailContainingOrNameContainingOrSubjectContainingOrMessageContaining(String query, String query1, String query2, String query3);
}