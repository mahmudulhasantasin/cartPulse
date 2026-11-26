package com.cartpulse.service;

import com.cartpulse.discount.DiscountStrategy;
import com.cartpulse.discount.DiscountStrategyFactory;
import com.cartpulse.dto.CartDtos.PromoPreviewResponse;
import com.cartpulse.exception.BadRequestException;
import com.cartpulse.model.PromoCode;
import com.cartpulse.repository.PromoCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class PromoService {

    private final PromoCodeRepository promoCodeRepository;
    private final DiscountStrategyFactory strategyFactory;

    public PromoService(PromoCodeRepository promoCodeRepository, DiscountStrategyFactory strategyFactory) {
        this.promoCodeRepository = promoCodeRepository;
        this.strategyFactory = strategyFactory;
    }

    @Transactional(readOnly = true)
    public PromoPreviewResponse previewPromo(String rawCode, BigDecimal subtotal) {
        PromoCode promo = validatePromo(rawCode, subtotal);
        BigDecimal discount = calculateDiscountAmount(promo, subtotal);
        BigDecimal finalTotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        return new PromoPreviewResponse(
                promo.getCode(),
                promo.getDiscountType(),
                promo.getDiscountValue(),
                discount,
                finalTotal,
                "Promo code " + promo.getCode() + " applied!"
        );
    }

    public PromoCode validatePromo(String rawCode, BigDecimal subtotal) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BadRequestException("Please enter a promo code.");
        }
        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> new BadRequestException("Invalid promo code."));

        if (!promo.isActive()) {
            throw new BadRequestException("This promo code is currently disabled.");
        }
        if (promo.getExpiresAt().isBefore(LocalDate.now())) {
            throw new BadRequestException("This promo code has expired.");
        }
        if (promo.getUsedCount() >= promo.getMaxUses()) {
            throw new BadRequestException("This promo code has reached its maximum usage limit.");
        }
        if (subtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    String.format("Minimum order amount of $%s required for %s.",
                            promo.getMinOrderAmount().toPlainString(), promo.getCode())
            );
        }
        return promo;
    }

    public BigDecimal calculateDiscountAmount(PromoCode promo, BigDecimal subtotal) {
        DiscountStrategy strategy = strategyFactory.getStrategy(promo.getDiscountType());
        return strategy.calculateDiscount(subtotal, promo.getDiscountValue());
    }
}
