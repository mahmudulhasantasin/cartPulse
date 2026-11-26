package com.cartpulse.service;

import com.cartpulse.dto.AdminDtos.CategoryRequest;
import com.cartpulse.dto.AdminDtos.CategoryResponse;
import com.cartpulse.dto.AdminDtos.PromoRequest;
import com.cartpulse.dto.AdminDtos.PromoResponse;
import com.cartpulse.dto.AuthDtos.UserResponse;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.exception.ConflictException;
import com.cartpulse.exception.ResourceNotFoundException;
import com.cartpulse.model.Category;
import com.cartpulse.model.PromoCode;
import com.cartpulse.model.Role;
import com.cartpulse.model.User;
import com.cartpulse.repository.CategoryRepository;
import com.cartpulse.repository.PromoCodeRepository;
import com.cartpulse.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final CategoryRepository categoryRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;

    public AdminService(
            CategoryRepository categoryRepository,
            PromoCodeRepository promoCodeRepository,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Category '" + name + "' already exists.");
        }
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        Category saved = categoryRepository.save(new Category(name, slug, req.description()));
        return CategoryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PromoResponse> listPromos() {
        return promoCodeRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(PromoResponse::from)
                .toList();
    }

    @Transactional
    public PromoResponse createPromo(PromoRequest req) {
        String code = req.code().trim().toUpperCase();
        if (promoCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Promo code '" + code + "' already exists.");
        }
        PromoCode promo = new PromoCode();
        promo.setCode(code);
        promo.setDiscountType(req.discountType());
        promo.setDiscountValue(req.discountValue());
        promo.setMinOrderAmount(req.minOrderAmount());
        promo.setMaxUses(req.maxUses());
        promo.setExpiresAt(req.expiresAt());
        promo.setActive(true);
        return PromoResponse.from(promoCodeRepository.save(promo));
    }

    @Transactional
    public PromoResponse togglePromo(Long id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found."));
        promo.setActive(!promo.isActive());
        return PromoResponse.from(promoCodeRepository.save(promo));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse toggleUserActive(Long userId, Long adminId) {
        if (userId.equals(adminId)) {
            throw new BadRequestException("You cannot suspend your own admin account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be suspended via this endpoint.");
        }
        user.setActive(!user.isActive());
        return UserResponse.from(userRepository.save(user));
    }
}
