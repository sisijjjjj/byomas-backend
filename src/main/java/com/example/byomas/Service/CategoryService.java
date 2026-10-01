package com.example.byomas.Service;


import com.example.byomas.Entity.Category;
import com.example.byomas.Repository.CategoryRepository;
import com.example.byomas.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // === CRUD METHODS ===

    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<Category> getAllCategoriesWithCounts() {
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

        // Mettre à jour les compteurs de produits
        List<Object[]> counts = productRepository.countProductsByCategory();

        for (Category category : categories) {
            long count = counts.stream()
                    .filter(obj -> category.getName().equals(obj[0]))
                    .mapToLong(obj -> (Long) obj[1])
                    .findFirst()
                    .orElse(0L);
            category.setProductCount((int) count);
        }

        return categories;
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category createCategory(Category category) {
        // Vérifier si le nom existe déjà
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà");
        }

        return categoryRepository.save(category);
    }

    public Category updateCategory(Long id, Category categoryDetails) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    // Vérifier si le nom a changé et s'il est unique
                    if (!existingCategory.getName().equals(categoryDetails.getName()) &&
                            categoryRepository.existsByName(categoryDetails.getName())) {
                        throw new RuntimeException("Une catégorie avec ce nom existe déjà");
                    }

                    if (categoryDetails.getName() != null) {
                        existingCategory.setName(categoryDetails.getName());
                    }
                    if (categoryDetails.getDescription() != null) {
                        existingCategory.setDescription(categoryDetails.getDescription());
                    }
                    if (categoryDetails.getImageUrl() != null) {
                        existingCategory.setImageUrl(categoryDetails.getImageUrl());
                    }
                    if (categoryDetails.getDisplayOrder() != null) {
                        existingCategory.setDisplayOrder(categoryDetails.getDisplayOrder());
                    }
                    if (categoryDetails.getIsActive() != null) {
                        existingCategory.setIsActive(categoryDetails.getIsActive());
                    }

                    return categoryRepository.save(existingCategory);
                })
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));

        // Vérifier s'il y a des produits dans cette catégorie
        long productCount = productRepository.findByCategory(category.getName()).size();
        if (productCount > 0) {
            throw new RuntimeException(
                    "Impossible de supprimer la catégorie. " + productCount + " produit(s) y sont associés."
            );
        }

        categoryRepository.deleteById(id);
    }

    public void softDeleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée avec l'id: " + id));

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    // === BUSINESS METHODS ===

    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    public boolean categoryExists(String name) {
        return categoryRepository.existsByName(name);
    }

    public void updateProductCounts() {
        List<Object[]> counts = productRepository.countProductsByCategory();
        List<Category> categories = categoryRepository.findAll();

        for (Category category : categories) {
            long count = counts.stream()
                    .filter(obj -> category.getName().equals(obj[0]))
                    .mapToLong(obj -> (Long) obj[1])
                    .findFirst()
                    .orElse(0L);
            category.setProductCount((int) count);
            categoryRepository.save(category);
        }
    }

    public long getTotalCategories() {
        return categoryRepository.count();
    }

    public long getActiveCategoriesCount() {
        return categoryRepository.findByIsActiveTrue().size();
    }
}