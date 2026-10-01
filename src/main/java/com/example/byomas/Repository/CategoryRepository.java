package com.example.byomas.Repository;


import com.example.byomas.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Trouver par nom
    Optional<Category> findByName(String name);

    // Catégories actives
    List<Category> findByIsActiveTrue();

    // Catégories triées par ordre d'affichage
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();

    // Vérifier si le nom existe (pour éviter les doublons)
    boolean existsByName(String name);

    // Compter les produits par catégorie
    @Query("SELECT c.name, COUNT(p) FROM Category c LEFT JOIN Product p ON p.category = c.name AND p.isActive = true GROUP BY c.id, c.name")
    List<Object[]> getProductCountsByCategory();

    List<Category> findAllByOrderByNameAsc();

    List<Category> findByIsActiveTrueOrderByNameAsc();
}