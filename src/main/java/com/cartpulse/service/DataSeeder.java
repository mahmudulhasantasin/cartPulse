package com.cartpulse.service;

import com.cartpulse.model.*;
import com.cartpulse.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds initial catalog, demo accounts, and promo codes only on an empty database.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            PromoCodeRepository promoCodeRepository,
            InventoryLogRepository inventoryLogRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User customer = userRepository.save(new User(
                "Tasin Ahmed",
                "customer@cartpulse.dev",
                passwordEncoder.encode("customer123"),
                Role.CUSTOMER
        ));
        cartRepository.save(new Cart(customer));

        User seller1 = userRepository.save(new User(
                "TechCraft Studio",
                "seller@cartpulse.dev",
                passwordEncoder.encode("seller123"),
                Role.SELLER
        ));
        User seller2 = userRepository.save(new User(
                "Nordic Desk Co.",
                "nordic@cartpulse.dev",
                passwordEncoder.encode("seller123"),
                Role.SELLER
        ));
        userRepository.save(new User(
                "Platform Admin",
                "admin@cartpulse.dev",
                passwordEncoder.encode("admin123"),
                Role.ADMIN
        ));

        Category electronics = categoryRepository.save(new Category("Electronics", "electronics", "Hubs, adapters, and smart devices"));
        Category workspace = categoryRepository.save(new Category("Workspace & Desk", "workspace", "Keyboards, stands, and ergonomics"));
        Category audio = categoryRepository.save(new Category("Audio & Gear", "audio", "Headphones and studio accessories"));
        Category books = categoryRepository.save(new Category("Books & Learning", "books", "Engineering and CS literature"));
        Category accessories = categoryRepository.save(new Category("Accessories", "accessories", "Desk mats and everyday carry"));

        List<Product> seeded = List.of(
                buildProduct(seller1, workspace, "Keychron K2 Wireless Mechanical Keyboard",
                        "Compact 75% tactile mechanical keyboard with dual Bluetooth 5.1 & USB-C connectivity, aluminum frame, and white LED backlight.",
                        "89.99", 18, 5, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80"),
                buildProduct(seller1, audio, "Sony WH-1000XM5 Noise Cancelling Headphones",
                        "Industry-leading active noise cancellation, 30-hour battery life, multipoint connection, and crystal-clear call quality.",
                        "329.00", 4, 5, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80"),
                buildProduct(seller2, workspace, "ErgoLift Aluminum Laptop Stand",
                        "CNC-machined anodized aluminum stand elevating laptops up to 17 inches for healthier posture and better airflow.",
                        "44.50", 27, 6, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=800&q=80"),
                buildProduct(seller2, books, "Designing Data-Intensive Applications (Paperback)",
                        "Essential distributed systems and database architecture reference by Martin Kleppmann for software engineers and CS students.",
                        "38.00", 12, 5, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=800&q=80"),
                buildProduct(seller1, electronics, "USB-C 8-in-1 4K Multiport Hub",
                        "4K HDMI 60Hz, 100W PD pass-through charging, Gigabit Ethernet, SD/TF card reader, and dual USB 3.2 ports.",
                        "54.00", 8, 5, "https://images.unsplash.com/photo-1625842268584-8f3296236761?auto=format&fit=crop&w=800&q=80"),
                buildProduct(seller2, accessories, "Minimalist Wool Felt Desk Mat",
                        "Soft merino-blend felt desk pad (900x400mm) with anti-slip backing to organize keyboard, mouse, and notebook.",
                        "26.00", 3, 5, "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?auto=format&fit=crop&w=800&q=80")
        );

        for (Product p : seeded) {
            Product saved = productRepository.save(p);
            inventoryLogRepository.save(new InventoryLog(saved, saved.getStockQuantity(), InventoryReason.INITIAL, null));
        }

        PromoCode welcome = new PromoCode();
        welcome.setCode("WELCOME10");
        welcome.setDiscountType(DiscountType.PERCENT);
        welcome.setDiscountValue(new BigDecimal("10.00"));
        welcome.setMinOrderAmount(new BigDecimal("30.00"));
        welcome.setMaxUses(500);
        welcome.setExpiresAt(LocalDate.now().plusYears(1));
        promoCodeRepository.save(welcome);

        PromoCode save15 = new PromoCode();
        save15.setCode("SAVE15");
        save15.setDiscountType(DiscountType.FIXED);
        save15.setDiscountValue(new BigDecimal("15.00"));
        save15.setMinOrderAmount(new BigDecimal("100.00"));
        save15.setMaxUses(200);
        save15.setExpiresAt(LocalDate.now().plusYears(1));
        promoCodeRepository.save(save15);
    }

    private Product buildProduct(
            User seller,
            Category category,
            String name,
            String desc,
            String price,
            int stock,
            int threshold,
            String img
    ) {
        Product p = new Product();
        p.setSeller(seller);
        p.setCategory(category);
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        p.setLowStockThreshold(threshold);
        p.setImageUrl(img);
        return p;
    }
}
