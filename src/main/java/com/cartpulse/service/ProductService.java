package com.cartpulse.service;

import com.cartpulse.dto.ProductDtos.InventoryLogResponse;
import com.cartpulse.dto.ProductDtos.ProductRequest;
import com.cartpulse.dto.ProductDtos.ProductResponse;
import com.cartpulse.exception.ForbiddenActionException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.*;
import com.cartpulse.repository.CategoryRepository;
import com.cartpulse.repository.InventoryLogRepository;
import com.cartpulse.repository.ProductRepository;
import com.cartpulse.repository.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final String DEFAULT_PRODUCT_IMAGE =
            "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            InventoryLogRepository inventoryLogRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchCatalog(
            String query,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            boolean inStockOnly,
            String sortKey,
            int page,
            int size
    ) {
        Sort sort = switch (sortKey == null ? "featured" : sortKey) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating-desc" -> Sort.by(Sort.Direction.DESC, "avgRating").and(Sort.by(Sort.Direction.DESC, "reviewCount"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(48, Math.max(1, size)), sort);

        Specification<Product> spec = (root, criteriaQuery, cb) -> {
            if (criteriaQuery != null && Product.class.equals(criteriaQuery.getResultType())) {
                root.fetch("category", JoinType.INNER);
                root.fetch("seller", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minRating != null && minRating.compareTo(BigDecimal.ZERO) > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("avgRating"), minRating));
            }
            if (inStockOnly) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(Long sellerId) {
        return productRepository.findAllBySellerId(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Long sellerId) {
        return productRepository.findLowStockBySeller(sellerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductRequest req) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found."));
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        Product product = new Product();
        product.setSeller(seller);
        product.setCategory(category);
        product.setName(req.name().trim());
        product.setDescription(req.description().trim());
        product.setPrice(req.price());
        product.setStockQuantity(req.stockQuantity());
        product.setLowStockThreshold(req.lowStockThreshold() != null ? req.lowStockThreshold() : 5);
        product.setImageUrl(req.imageUrl() != null && !req.imageUrl().isBlank()
                ? req.imageUrl().trim()
                : DEFAULT_PRODUCT_IMAGE);

        Product saved = productRepository.save(product);
        if (saved.getStockQuantity() > 0) {
            inventoryLogRepository.save(new InventoryLog(saved, saved.getStockQuantity(), InventoryReason.INITIAL, null));
        }
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, Long actorId, boolean isAdmin, ProductRequest req) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        verifyOwnership(product, actorId, isAdmin);

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        int previousStock = product.getStockQuantity();
        int newStock = req.stockQuantity();

        product.setCategory(category);
        product.setName(req.name().trim());
        product.setDescription(req.description().trim());
        product.setPrice(req.price());
        product.setStockQuantity(newStock);
        if (req.lowStockThreshold() != null) {
            product.setLowStockThreshold(req.lowStockThreshold());
        }
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) {
            product.setImageUrl(req.imageUrl().trim());
        }

        Product saved = productRepository.save(product);
        if (newStock != previousStock) {
            inventoryLogRepository.save(new InventoryLog(saved, newStock - previousStock, InventoryReason.MANUAL_ADJUST, null));
        }
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse restockProduct(Long productId, Long actorId, boolean isAdmin, int amount) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        verifyOwnership(product, actorId, isAdmin);

        product.setStockQuantity(product.getStockQuantity() + amount);
        Product saved = productRepository.save(product);
        inventoryLogRepository.save(new InventoryLog(saved, amount, InventoryReason.RESTOCK, null));
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse toggleActive(Long productId, Long actorId, boolean isAdmin) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        verifyOwnership(product, actorId, isAdmin);

        product.setActive(!product.isActive());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<InventoryLogResponse> getInventoryLogs(Long productId, Long actorId, boolean isAdmin) {
        Product product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        verifyOwnership(product, actorId, isAdmin);
        return inventoryLogRepository.findTop30ByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(InventoryLogResponse::from)
                .toList();
    }

    private void verifyOwnership(Product product, Long actorId, boolean isAdmin) {
        if (!isAdmin && !product.getSeller().getId().equals(actorId)) {
            throw new ForbiddenActionException("You can only manage your own products.");
        }
    }
}
