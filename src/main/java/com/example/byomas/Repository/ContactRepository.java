package com.example.byomas.Repository;

import com.example.byomas.Entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<ContactMessage, Long> {

    // Trouver par statut
    List<ContactMessage> findByStatusOrderByCreatedAtDesc(String status);
    Page<ContactMessage> findByStatus(String status, Pageable pageable);

    // Trouver par email
    List<ContactMessage> findByEmailOrderByCreatedAtDesc(String email);
    List<ContactMessage> findByEmailAndStatus(String email, String status);

    // Trouver par priorité
    List<ContactMessage> findByPriorityOrderByCreatedAtDesc(String priority);

    // Compter par statut
    long countByStatus(String status);

    // Compter par période
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndCreatedAtBetween(String status, LocalDateTime start, LocalDateTime end);

    // Recherche avancée
    @Query("SELECT c FROM ContactMessage c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.message) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ContactMessage> searchContacts(@Param("query") String query);

    @Query("SELECT c FROM ContactMessage c WHERE " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.message) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ContactMessage> searchContacts(@Param("query") String query, Pageable pageable);

    // Trouver les messages non lus
    List<ContactMessage> findByStatusOrderByCreatedAtAsc(String status);

    // Trouver les messages récents (7 derniers jours)
    @Query("SELECT c FROM ContactMessage c WHERE c.createdAt >= :date")
    List<ContactMessage> findRecentContacts(@Param("date") LocalDateTime date);

    // Statistiques par mois
    @Query("SELECT MONTH(c.createdAt) as month, COUNT(c) as count FROM ContactMessage c WHERE YEAR(c.createdAt) = :year GROUP BY MONTH(c.createdAt)")
    List<Object[]> countByMonth(@Param("year") int year);

    // Trouver les doublons potentiels
    @Query("SELECT c FROM ContactMessage c WHERE c.email = :email AND c.createdAt >= :startDate")
    List<ContactMessage> findPotentialDuplicates(@Param("email") String email, @Param("startDate") LocalDateTime startDate);

    // Messages sans réponse
    @Query("SELECT c FROM ContactMessage c WHERE c.responseSent = false AND c.status != 'ARCHIVED'")
    List<ContactMessage> findUnansweredMessages();

    // Messages urgents
    List<ContactMessage> findByPriorityAndStatusNotOrderByCreatedAtDesc(String priority, String status);

    // Pour la pagination avec tri
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ContactMessage> findAllByOrderByPriorityDescCreatedAtDesc(Pageable pageable);

    // Messages non lus (pour notifications)
    List<ContactMessage> findByStatus(String status);

    // Derniers messages (pour dashboard)
    List<ContactMessage> findTop5ByOrderByCreatedAtDesc();

    // Messages par période
    List<ContactMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}