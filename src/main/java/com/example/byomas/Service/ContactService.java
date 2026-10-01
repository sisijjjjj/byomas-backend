package com.example.byomas.Service;

import com.example.byomas.Entity.ContactMessage;
import com.example.byomas.Repository.ContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Crée un nouveau message de contact
     */
    public ContactMessage createContact(ContactMessage contactMessage) {
        // Sauvegarde en base
        ContactMessage savedMessage = contactRepository.save(contactMessage);

        // Envoi de notification email
        boolean emailSent = emailService.sendContactNotification(savedMessage);

        if (emailSent) {
            System.out.println("✅ Contact créé et notification envoyée");
        } else {
            System.out.println("⚠️ Contact créé mais email non envoyé");
        }

        return savedMessage;
    }

    /**
     * Récupère tous les messages
     */
    public List<ContactMessage> getAllContacts() {
        return contactRepository.findAll();
    }

    /**
     * Récupère un message par ID
     */
    public Optional<ContactMessage> getContactById(Long id) {
        return contactRepository.findById(id);
    }

    /**
     * Supprime un message
     */
    public boolean deleteContact(Long id) {
        if (contactRepository.existsById(id)) {
            contactRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Compte le nombre total de messages
     */
    public long getContactCount() {
        return contactRepository.count();
    }

    /**
     * Récupère les messages non lus
     */
    public List<ContactMessage> getUnreadContacts() {
        return contactRepository.findByStatus("NEW");
    }

    /**
     * Marque un message comme lu
     */
    public ContactMessage markAsRead(Long id) {
        Optional<ContactMessage> contactOpt = contactRepository.findById(id);
        if (contactOpt.isPresent()) {
            ContactMessage contact = contactOpt.get();
            contact.setStatus("READ");
            return contactRepository.save(contact);
        }
        return null;
    }
}