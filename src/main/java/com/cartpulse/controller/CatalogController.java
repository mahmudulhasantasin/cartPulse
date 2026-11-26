package com.cartpulse.controller;

import com.cartpulse.dto.AdminDtos.CategoryResponse;
import com.cartpulse.dto.ProductDtos.ProductResponse;
import com.cartpulse.service.AdminService;
import com.cartpulse.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final ProductService productService;
    private final AdminService adminService;

    public CatalogController(ProductService productService, AdminService adminService) {
        this.productService = productService;
        this.adminService = adminService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> getCategories() {
        return adminService.listCategories();
    }

    @GetMapping("/products")
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "false") boolean inStockOnly,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return productService.searchCatalog(q, categoryId, minPrice, maxPrice, minRating, inStockOnly, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }
}
