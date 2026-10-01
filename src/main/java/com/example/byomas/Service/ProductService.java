package com.example.byomas.Service;

import com.example.byomas.Entity.Product;
import com.example.byomas.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // MÉTHODES DE LECTURE - readOnly = true pour optimisation
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        System.out.println("📦 Produits récupérés depuis DB: " + products.size());
        return products;
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.searchProducts(query.trim());
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts(10);
    }

    @Transactional(readOnly = true)
    public List<Product> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts();
    }

    @Transactional(readOnly = true)
    public List<Product> getRecentProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> getBestSellingProducts() {
        return productRepository.findTopSellingProducts();
    }

    @Transactional(readOnly = true)
    public long getTotalProducts() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveProductsCount() {
        return productRepository.countByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public boolean isReferenceUnique(String reference) {
        return productRepository.findByReference(reference).isEmpty();
    }

    // MÉTHODES D'ÉCRITURE - Transactions complètes
    @Transactional
    public Product createProduct(Product product) {
        try {
            System.out.println("🔄 Début création produit: " + product.getName());

            // Validation des champs obligatoires
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                throw new RuntimeException("Le nom du produit est obligatoire");
            }
            if (product.getPrice() == null || product.getPrice() < 0) {
                throw new RuntimeException("Le prix doit être supérieur ou égal à 0");
            }

            // Valeurs par défaut
            if (product.getStockQuantity() == null) {
                product.setStockQuantity(0);
            }
            if (product.getSalesCount() == null) {
                product.setSalesCount(0);
            }
            if (product.getIsActive() == null) {
                product.setIsActive(true);
            }
            if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
                product.setCategory("Non catégorisé");
            }

            // Générer une référence si vide
            if (product.getReference() == null || product.getReference().trim().isEmpty()) {
                product.setReference("REF-" + System.currentTimeMillis());
            }

            // Dates
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            // SAUVEGARDE AVEC FLUSH
            Product savedProduct = productRepository.save(product);
            productRepository.flush(); // ← FORCE L'ÉCRITURE IMMÉDIATE EN BASE

            System.out.println("✅ Produit créé avec ID: " + savedProduct.getId());

            // VÉRIFICATION IMMÉDIATE
            Optional<Product> verified = productRepository.findById(savedProduct.getId());
            if (verified.isPresent()) {
                System.out.println("🔍 Vérification OK - Produit présent en base");
            } else {
                System.err.println("❌ Vérification ÉCHEC - Produit non trouvé après création");
            }

            return savedProduct;

        } catch (Exception e) {
            System.err.println("💥 Erreur création produit: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création du produit: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        try {
            System.out.println("🔄 Début mise à jour produit ID: " + id);

            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

            // Mettre à jour uniquement les champs non nuls
            if (productDetails.getName() != null) {
                product.setName(productDetails.getName());
            }
            if (productDetails.getDescription() != null) {
                product.setDescription(productDetails.getDescription());
            }
            if (productDetails.getPrice() != null) {
                product.setPrice(productDetails.getPrice());
            }
            if (productDetails.getStockQuantity() != null) {
                product.setStockQuantity(productDetails.getStockQuantity());
            }
            if (productDetails.getCategory() != null) {
                product.setCategory(productDetails.getCategory());
            }
            if (productDetails.getImageUrl() != null) {
                product.setImageUrl(productDetails.getImageUrl());
            }
            if (productDetails.getReference() != null) {
                product.setReference(productDetails.getReference());
            }
            if (productDetails.getBadge() != null) {
                product.setBadge(productDetails.getBadge());
            }
            if (productDetails.getWeight() != null) {
                product.setWeight(productDetails.getWeight());
            }
            if (productDetails.getDimensions() != null) {
                product.setDimensions(productDetails.getDimensions());
            }
            if (productDetails.getBrand() != null) {
                product.setBrand(productDetails.getBrand());
            }
            if (productDetails.getSpecialOffer() != null) {
                product.setSpecialOffer(productDetails.getSpecialOffer());
            }
            if (productDetails.getRating() != null) {
                product.setRating(productDetails.getRating());
            }
            if (productDetails.getRatingCount() != null) {
                product.setRatingCount(productDetails.getRatingCount());
            }
            if (productDetails.getSalesCount() != null) {
                product.setSalesCount(productDetails.getSalesCount());
            }
            if (productDetails.getIsActive() != null) {
                product.setIsActive(productDetails.getIsActive());
            }

            product.setUpdatedAt(LocalDateTime.now());

            Product updatedProduct = productRepository.save(product);
            productRepository.flush(); // ← FORCE L'ÉCRITURE

            System.out.println("✅ Produit mis à jour ID: " + updatedProduct.getId());
            return updatedProduct;

        } catch (Exception e) {
            System.err.println("💥 Erreur mise à jour produit: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour du produit: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Product updateStock(Long id, Integer newStock) {
        try {
            if (newStock == null || newStock < 0) {
                throw new RuntimeException("Le stock ne peut pas être négatif");
            }

            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

            product.setStockQuantity(newStock);
            product.setUpdatedAt(LocalDateTime.now());

            Product updated = productRepository.save(product);
            productRepository.flush();

            System.out.println("📊 Stock mis à jour - ID: " + id + ", Nouveau stock: " + newStock);
            return updated;

        } catch (Exception e) {
            System.err.println("💥 Erreur mise à jour stock: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Product updatePrice(Long id, Double newPrice) {
        try {
            if (newPrice == null || newPrice < 0) {
                throw new RuntimeException("Le prix doit être supérieur ou égal à 0");
            }

            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

            product.setPrice(newPrice);
            product.setUpdatedAt(LocalDateTime.now());

            Product updated = productRepository.save(product);
            productRepository.flush();

            System.out.println("💰 Prix mis à jour - ID: " + id + ", Nouveau prix: " + newPrice);
            return updated;

        } catch (Exception e) {
            System.err.println("💥 Erreur mise à jour prix: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

            // Soft delete - désactiver le produit
            product.setIsActive(false);
            product.setUpdatedAt(LocalDateTime.now());

            productRepository.save(product);
            productRepository.flush();

            System.out.println("🗑️ Produit désactivé (soft delete) ID: " + id);

        } catch (Exception e) {
            System.err.println("💥 Erreur suppression produit: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void hardDeleteProduct(Long id) {
        try {
            if (!productRepository.existsById(id)) {
                throw new RuntimeException("Produit non trouvé avec l'id: " + id);
            }

            productRepository.deleteById(id);
            productRepository.flush();

            System.out.println("🔥 Produit supprimé définitivement ID: " + id);

        } catch (Exception e) {
            System.err.println("💥 Erreur suppression définitive: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Product restoreProduct(Long id) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + id));

            product.setIsActive(true);
            product.setUpdatedAt(LocalDateTime.now());

            Product restored = productRepository.save(product);
            productRepository.flush();

            System.out.println("♻️ Produit restauré ID: " + id);
            return restored;

        } catch (Exception e) {
            System.err.println("💥 Erreur restauration produit: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la restauration du produit: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Product incrementSalesCount(Long productId, int quantity) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

            Integer currentSales = product.getSalesCount() != null ? product.getSalesCount() : 0;
            product.setSalesCount(currentSales + quantity);
            product.setUpdatedAt(LocalDateTime.now());

            Product updated = productRepository.save(product);
            productRepository.flush();

            System.out.println("📈 Ventes incrémentées - ID: " + productId + ", Quantité: " + quantity);
            return updated;

        } catch (Exception e) {
            System.err.println("💥 Erreur incrémentation ventes: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public Product addRating(Long productId, Double newRating) {
        try {
            if (newRating == null || newRating < 0 || newRating > 5) {
                throw new RuntimeException("La note doit être entre 0 et 5");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'id: " + productId));

            Double currentRating = product.getRating() != null ? product.getRating() : 0.0;
            Integer currentRatingCount = product.getRatingCount() != null ? product.getRatingCount() : 0;

            // Calculer la nouvelle moyenne
            Double newAverageRating = ((currentRating * currentRatingCount) + newRating) / (currentRatingCount + 1);

            product.setRating(newAverageRating);
            product.setRatingCount(currentRatingCount + 1);
            product.setUpdatedAt(LocalDateTime.now());

            Product updated = productRepository.save(product);
            productRepository.flush();

            System.out.println("⭐ Note ajoutée - ID: " + productId + ", Nouvelle moyenne: " + newAverageRating);
            return updated;

        } catch (Exception e) {
            System.err.println("💥 Erreur ajout note: " + e.getMessage());
            throw e;
        }
    }

    // MÉTHODES SUPPLEMENTAIRES
    @Transactional(readOnly = true)
    public List<Product> getProductsWithLowStock(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
                .map(Product::getCategory)
                .distinct()
                .filter(category -> category != null && !category.trim().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public long countProductsByCategory(String category) {
        return productRepository.countByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsWithSpecialOffers() {
        return productRepository.findProductsWithSpecialOffer();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        if (minPrice == null) minPrice = 0.0;
        if (maxPrice == null) maxPrice = Double.MAX_VALUE;
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    @Transactional(readOnly = true)
    public List<Product> getTopRatedProducts() {
        return productRepository.findAllByOrderByRatingDesc();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsNeedingRestock() {
        return productRepository.findProductsNeedingRestock();
    }

    @Transactional(readOnly = true)
    public Double getTotalStockValue() {
        return productRepository.getTotalStockValue();
    }

    @Transactional(readOnly = true)
    public Double getAverageProductPrice() {
        return productRepository.getAverageProductPrice();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getCategoryStatistics() {
        return productRepository.getCategoryStatistics();
    }

    // NOUVELLE MÉTHODE : Vérifier l'état de la base
    @Transactional(readOnly = true)
    public Map<String, Object> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            status.put("totalProducts", productRepository.count());
            status.put("activeProducts", productRepository.countByIsActiveTrue());
            status.put("outOfStockProducts", productRepository.countOutOfStockProducts());
            status.put("lowStockProducts", productRepository.countLowStockProducts(10));
            status.put("totalStockValue", getTotalStockValue());
            status.put("averagePrice", getAverageProductPrice());
            status.put("databaseConnection", "OK");
            status.put("timestamp", LocalDateTime.now());
        } catch (Exception e) {
            status.put("databaseConnection", "ERROR: " + e.getMessage());
            status.put("errorDetails", e.toString());
        }
        return status;
    }

    // MÉTHODE : Statistiques détaillées des produits
    @Transactional(readOnly = true)
    public Map<String, Object> getProductStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalProducts", productRepository.count());
            stats.put("activeProducts", productRepository.countByIsActiveTrue());
            stats.put("inactiveProducts", productRepository.countByIsActiveFalse());
            stats.put("outOfStockProducts", productRepository.countOutOfStockProducts());
            stats.put("lowStockProducts", productRepository.countLowStockProducts(10));
            stats.put("totalStockValue", getTotalStockValue());
            stats.put("averagePrice", getAverageProductPrice());
            stats.put("categoriesCount", getAllCategories().size());
            stats.put("productsWithImages", productRepository.findProductsWithImages().size());
            stats.put("productsWithoutImages", productRepository.findProductsWithoutImages().size());
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            stats.put("error", "Erreur lors du calcul des statistiques: " + e.getMessage());
        }

        return stats;
    }

    // MÉTHODE : Recherche avancée avec filtres
    @Transactional(readOnly = true)
    public List<Product> advancedSearch(String query, String category, Double minPrice,
                                        Double maxPrice, Integer minStock, Boolean isActive) {
        return productRepository.findProductsByFilters(category, minPrice, maxPrice, minStock, isActive);
    }

    // MÉTHODE : Mettre à jour plusieurs produits en lot
    @Transactional
    public List<Product> updateProductsInBatch(List<Product> products) {
        try {
            for (Product product : products) {
                product.setUpdatedAt(LocalDateTime.now());
            }

            List<Product> updatedProducts = productRepository.saveAll(products);
            productRepository.flush();

            System.out.println("🔄 Lot de " + updatedProducts.size() + " produits mis à jour");
            return updatedProducts;

        } catch (Exception e) {
            System.err.println("💥 Erreur mise à jour lot: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour en lot: " + e.getMessage(), e);
        }
    }
}