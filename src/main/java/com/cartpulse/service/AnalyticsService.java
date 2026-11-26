package com.cartpulse.service;

import com.cartpulse.dto.AdminDtos.AdminAnalyticsResponse;
import com.cartpulse.dto.AdminDtos.ChartSeries;
import com.cartpulse.dto.AdminDtos.SellerAnalyticsResponse;
import com.cartpulse.model.Order;
import com.cartpulse.model.OrderItem;
import com.cartpulse.model.OrderStatus;
import com.cartpulse.model.Product;
import com.cartpulse.repository.OrderRepository;
import com.cartpulse.repository.ProductRepository;
import com.cartpulse.repository.PromoCodeRepository;
import com.cartpulse.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final UserRepository userRepository;

    public AnalyticsService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            PromoCodeRepository promoCodeRepository,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SellerAnalyticsResponse getSellerAnalytics(Long sellerId) {
        List<Order> activeOrders = orderRepository.findActiveOrdersForSellerAnalytics(sellerId, OrderStatus.CANCELLED);
        List<Product> sellerProducts = productRepository.findAllBySellerId(sellerId);

        BigDecimal grossRevenue = activeOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeProducts = sellerProducts.stream().filter(Product::isActive).count();
        long lowStockCount = sellerProducts.stream()
                .filter(p -> p.isActive() && p.getStockQuantity() <= p.getLowStockThreshold())
                .count();

        // Last 7 days revenue
        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Order o : activeOrders) {
            LocalDate orderDate = o.getCreatedAt().toLocalDate();
            if (dailyMap.containsKey(orderDate)) {
                dailyMap.put(orderDate, dailyMap.get(orderDate).add(o.getTotalAmount()));
            }
        }

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM dd");
        List<String> dayLabels = dailyMap.keySet().stream().map(d -> d.format(dayFmt)).toList();
        List<BigDecimal> dayValues = new ArrayList<>(dailyMap.values());

        // Sales by category
        Map<String, BigDecimal> categorySales = new LinkedHashMap<>();
        for (Order o : activeOrders) {
            for (OrderItem item : o.getItems()) {
                String catName = item.getProduct().getCategory().getName();
                categorySales.merge(catName, item.getLineTotal(), BigDecimal::add);
            }
        }

        return new SellerAnalyticsResponse(
                grossRevenue,
                activeOrders.size(),
                activeProducts,
                lowStockCount,
                new ChartSeries(dayLabels, dayValues),
                categorySales
        );
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminAnalytics() {
        List<Order> allOrders = orderRepository.findAllDetailedForAdminAnalytics();

        BigDecimal platformGmv = allOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activePromos = promoCodeRepository.findAll().stream()
                .filter(p -> p.isActive() && !p.getExpiresAt().isBefore(LocalDate.now()))
                .count();

        long totalUsers = userRepository.count();

        // Last 6 months volume
        YearMonth currentMonth = YearMonth.now();
        Map<YearMonth, BigDecimal> monthlyMap = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            monthlyMap.put(currentMonth.minusMonths(i), BigDecimal.ZERO);
        }
        for (Order o : allOrders) {
            if (o.getStatus() == OrderStatus.CANCELLED) continue;
            YearMonth ym = YearMonth.from(o.getCreatedAt());
            if (monthlyMap.containsKey(ym)) {
                monthlyMap.put(ym, monthlyMap.get(ym).add(o.getTotalAmount()));
            }
        }

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yy");
        List<String> monthLabels = monthlyMap.keySet().stream().map(m -> m.format(monthFmt)).toList();
        List<BigDecimal> monthValues = new ArrayList<>(monthlyMap.values());

        // Orders by status
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus s : OrderStatus.values()) {
            statusCounts.put(s.name(), 0L);
        }
        for (Order o : allOrders) {
            statusCounts.merge(o.getStatus().name(), 1L, Long::sum);
        }

        return new AdminAnalyticsResponse(
                platformGmv,
                allOrders.size(),
                activePromos,
                totalUsers,
                new ChartSeries(monthLabels, monthValues),
                statusCounts
        );
    }
}
