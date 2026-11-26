# CartPulse — Smart Multi-Vendor E-Commerce & Inventory Analytics Platform

![Java](https://img.shields.io/badge/Java-27%20(LTS%2B)-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.3-brightgreen)
![MySQL](https://img.shields.io/badge/Database-MySQL%208.0%20(FreeDB)-blue)
![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20CSS3%20%7C%20Vanilla%20JS-yellow)

**CartPulse** is a full-stack multi-vendor e-commerce web application built for an **Object-Oriented Programming (OOP) in Java** university course project. Unlike basic CRUD demos, CartPulse solves real-world commerce engineering challenges including **atomic inventory locking (preventing overselling)**, a **state-machine order lifecycle**, a **Strategy-pattern promo engine**, **verified-buyer-only reviews**, and **live Seller/Admin analytics dashboards**.

---

## Table of Contents
1. [OOP in Java Concepts Demonstrated](#oop-in-java-concepts-demonstrated)
2. [Key Features & User Roles](#key-features--user-roles)
3. [System Architecture & Tech Stack](#system-architecture--tech-stack)
4. [Database Schema (MySQL 8.0)](#database-schema-mysql-80)
5. [Installation & Execution Guide](#installation--execution-guide)
6. [Seeded Demo Accounts](#seeded-demo-accounts)
7. [5-Minute Project Demonstration Guide](#5-minute-project-demonstration-guide)
8. [Viva Questions & Answers (OOP in Java)](#viva-questions--answers-oop-in-java)

---

## OOP in Java Concepts Demonstrated

| OOP Concept / Pattern | How It Is Applied in CartPulse | Key Source Files |
| :--- | :--- | :--- |
| **Encapsulation** | Entity classes keep fields `private` with controlled getters/setters and invariants; Request/Response DTOs prevent exposing sensitive fields like `passwordHash`. | `model/User.java`, `model/Product.java`, `dto/AuthDtos.java` |
| **Abstraction** | `DiscountStrategy` interface and `AbstractDiscountStrategy` abstract class (Template Method) hide discount math; `JpaRepository` abstracts SQL operations. | `discount/DiscountStrategy.java`, `discount/AbstractDiscountStrategy.java`, `repository/*.java` |
| **Inheritance** | `PercentageDiscountStrategy` and `FixedAmountDiscountStrategy` extend `AbstractDiscountStrategy` (using `protected` methods); custom exceptions extend `RuntimeException`; `CustomUserDetails` implements `UserDetails`. | `discount/*.java`, `exception/*.java`, `security/CustomUserDetails.java` |
| **Polymorphism** | `PercentageDiscountStrategy` and `FixedAmountDiscountStrategy` implement `DiscountStrategy` and are dispatched dynamically at runtime via `DiscountStrategyFactory`. `@RestControllerAdvice` handles exception subtypes polymorphically. | `discount/DiscountStrategyFactory.java`, `exception/GlobalExceptionHandler.java` |
| **Java Generics (`<T>`)** | `PageResponse<T>` wraps paginated results of any entity/DTO type cleanly; `JpaRepository<T, ID>` and `Specification<Product>` provide type-safe data access. | `dto/PageResponse.java`, `service/ProductService.java` |
| **Composition & Aggregation** | `Order` owns a `List<OrderItem>` (`cascade = ALL, orphanRemoval = true`), `Cart` owns `List<CartItem>`, and `Product` composes references to `User` (seller) and `Category`. | `model/Order.java`, `model/Cart.java`, `model/Product.java` |
| **Enum State Machine** | `OrderStatus` encapsulates legal state transitions (`canTransitionTo`) and pre-shipment cancellation rules (`isCancellable`) directly inside the enum. | `model/OrderStatus.java` |
| **Strategy + Factory Patterns** | Extensible promotional pricing engine supporting percentage and fixed discounts without `if/else` spaghetti in checkout logic. | `discount/*.java`, `service/PromoService.java` |

---

## Key Features & User Roles

| Role | Capabilities |
| :--- | :--- |
| **Guest** | Browse catalog, live keyword search, filter by category/price/rating/in-stock, sort, paginate, view product details & verified reviews, register or sign in. |
| **Customer** | Persistent DB shopping cart, live promo code validation (`WELCOME10`, `SAVE15`), **atomic multi-seller split checkout**, 5-step order tracking timeline, pre-shipment order cancellation (with automatic stock restoration), and **verified-purchase-only reviews** (requires `DELIVERED` order). |
| **Seller** | Store KPIs & Chart.js visualizations (7-day revenue + category breakdown), low-stock alert queue with 1-click restock, full Product CRUD, **Inventory Audit Trail** modal (`inventory_logs`), and state-validated order fulfillment (`PENDING` → `CONFIRMED` → `PACKED` → `SHIPPED` → `DELIVERED`). |
| **Admin** | Platform GMV & order status Chart.js analytics, promo code creation & toggling, category management, and user/seller account activation or suspension. |

---

## System Architecture & Tech Stack

- **Backend:** Java 27, Spring Boot 4.1.3 (`Spring Web MVC`, `Spring Data JPA`, `Spring Security`, `Bean Validation`)
- **Database:** MySQL 8.0 (configured for **FreeDB** remote MySQL or local MySQL)
- **Frontend:** Pure HTML5, CSS3 (Light White & Volt Yellow Neo-Editorial design system), Vanilla ES6 JavaScript (`fetch` API), Chart.js (via CDN)
- **Security:** BCrypt password hashing + `HttpOnly` session cookies (`SameSite=Lax`) + role-based authorization

```text
Browser (HTML / CSS / Vanilla JS)
   │  fetch() + JSON (Same-Origin Session Cookie)
   ▼
Spring Security Filter Chain (BCrypt + RBAC)
   │
   ▼
REST Controllers (@RestController + @Valid)
   │
   ▼
Service Layer (@Service + @Transactional + Strategy Pattern + State Machine)
   │
   ▼
Spring Data JPA Repositories (Hibernate ORM + Pessimistic Write Locks)
   │
   ▼
MySQL 8.0 Database (FreeDB / Local)
```

---

## Database Schema (MySQL 8.0)

10 normalized tables automatically managed via Hibernate/JPA:
1. `users` (`id` PK, `full_name`, `email` UNIQUE, `password_hash`, `role`, `is_active`, `created_at`)
2. `categories` (`id` PK, `name` UNIQUE, `slug` UNIQUE, `description`)
3. `products` (`id` PK, `seller_id` FK → `users`, `category_id` FK → `categories`, `price`, `stock_quantity`, `low_stock_threshold`, `avg_rating`, `review_count`, `is_active`)
4. `carts` (`id` PK, `customer_id` UNIQUE FK → `users`)
5. `cart_items` (`id` PK, `cart_id` FK → `carts`, `product_id` FK → `products`, `quantity`)
6. `promo_codes` (`id` PK, `code` UNIQUE, `discount_type`, `discount_value`, `min_order_amount`, `max_uses`, `used_count`, `expires_at`, `is_active`)
7. `orders` (`id` PK, `order_number` UNIQUE, `customer_id` FK, `seller_id` FK, `promo_code_id` FK, `subtotal_amount`, `discount_amount`, `total_amount`, `status`, `shipping_address`, `payment_method`)
8. `order_items` (`id` PK, `order_id` FK → `orders`, `product_id` FK → `products`, `product_name_snapshot`, `unit_price_snapshot`, `quantity`, `line_total`)
9. `reviews` (`id` PK, `product_id` FK, `customer_id` FK, `order_id` FK, `rating`, `comment`, UNIQUE `(product_id, customer_id)`)
10. `inventory_logs` (`id` PK, `product_id` FK, `change_amount`, `reason`, `reference_id`, `created_at`)

---

## Installation & Execution Guide

### Prerequisites
- **Java JDK 21 or 27** (`java -version`)
- **MySQL 8.0** (either a free [FreeDB](https://freedb.tech/) database or local MySQL Server)
- *(No global Maven installation required — the included Maven Wrapper `mvnw.cmd` handles everything.)*

### 1. Clone the Repository
```bash
git clone https://github.com/mahmudulhasantasin/cartPulse.git
cd cartPulse
```

### 2. Configure `.env`
Copy `.env.example` to `.env` and fill in your MySQL credentials:
```bash
cp .env.example .env
```
Example `.env` for FreeDB MySQL:
```properties
DB_CONNECTION=mysql
DB_HOST=sql.freedb.tech
DB_PORT=3306
DB_DATABASE=your_freedb_database
DB_USERNAME=your_freedb_username
DB_PASSWORD=your_freedb_password
```
> **Offline Viva Fallback:** If internet/FreeDB is slow in the classroom, set `DB_HOST=localhost`, `DB_DATABASE=cartpulse`, `DB_USERNAME=root`, and your local MySQL password in `.env`.

### 3. Run Automated Tests
```powershell
.\mvnw.cmd test
```

### 4. Start the Application Locally
```powershell
.\mvnw.cmd spring-boot:run
```
Open **`http://localhost:8081`** in your browser. On first boot against an empty database, `DataSeeder` automatically creates initial categories, products, promo codes, and demo accounts.

---

## Cloud Deployment (Backend on Render + Frontend on Vercel)

CartPulse is structured for clean **separated frontend/backend cloud hosting**:

### Step 1: Deploy Java Backend on Render
1. Create a **New Web Service** on [Render](https://render.com) connected to this GitHub repository.
2. Select **Runtime: Docker** (uses the root `Dockerfile`) and **Free** plan.
3. Add these **Environment Variables** in Render:
   - `DB_HOST` = `sql.freedb.tech`
   - `DB_PORT` = `3306`
   - `DB_DATABASE` = `<your freedb database>`
   - `DB_USERNAME` = `<your freedb username>`
   - `DB_PASSWORD` = `<your freedb password>`
   - `JWT_SECRET` = `<any 32+ character random secret>`
   - `ALLOWED_ORIGINS` = `https://*.vercel.app,http://localhost:*`
4. Wait for Render to build and note your live backend URL (e.g. `https://cartpulse-api.onrender.com`). Verify `/api/health` returns `{"status":"UP"}`.

### Step 2: Deploy Frontend on Vercel
1. Set `RENDER_API_URL` in `frontend/js/config.js` to your Render backend URL and push to GitHub.
2. Import this GitHub repository into [Vercel](https://vercel.com).
3. Set **Root Directory** to `frontend` and **Framework Preset** to `Other` (no build step required), then click **Deploy**.

---

## Seeded Demo Accounts

| Role | Email | Password |
| :--- | :--- | :--- |
| **Customer** | `customer@cartpulse.dev` | `customer123` |
| **Seller 1** | `seller@cartpulse.dev` | `seller123` |
| **Seller 2** | `nordic@cartpulse.dev` | `seller123` |
| **Admin** | `admin@cartpulse.dev` | `admin123` |

**Active Promo Codes:** `WELCOME10` (10% off orders ≥ $30), `SAVE15` ($15 off orders ≥ $100)

---

## 5-Minute Project Demonstration Guide

1. **Storefront & Filtering (`index.html`):** Demonstrate live search, category chips, price/in-stock filters, sorting, and stock badges (`In Stock`, `Only 4 left`).
2. **Customer Checkout + Strategy Pattern (`cart.html`):** Sign in with the **Customer** demo button, add items from two different sellers, apply `WELCOME10`, and place an order. Explain how checkout atomically splits into **one Order per Seller** while locking rows to prevent overselling.
3. **Order Lifecycle & Cancellation (`orders.html`):** Show the 5-step progress stepper (`PENDING` → `DELIVERED`), cancel an eligible order, and highlight automatic stock restoration.
4. **Seller Operations & Inventory Audit (`seller-dashboard.html`):** Sign in as **Seller**, inspect the live Chart.js revenue graphs, advance an order status, open **Logs** on a product to show `ORDER_DEDUCTION` / `ORDER_CANCEL_RESTORE` records in MySQL, and test `+10 Restock` on a low-stock item.
5. **Admin Governance (`admin-dashboard.html`):** Sign in as **Admin**, show platform GMV charts, create a promo code live, and verify user/seller controls.

---

## Viva Questions & Answers (OOP in Java)

1. **How does your project demonstrate Encapsulation?**
   All JPA entities keep fields `private` and expose controlled getters/setters. Sensitive fields like `User.passwordHash` are never serialized to JSON because controllers return dedicated DTO records (`UserResponse`) instead of raw database entities.
2. **Where did you use Abstraction and Interfaces?**
   `DiscountStrategy` is an interface declaring `calculateDiscount(BigDecimal subtotal, BigDecimal discountValue)`. `PromoService` works through that abstraction without knowing whether the discount is percentage-based or fixed. Our repositories also extend Spring Data's `JpaRepository` interface.
3. **Where is Inheritance used in your codebase?**
   Custom exceptions (`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, `ForbiddenActionException`) inherit from `RuntimeException`. Additionally, `CustomUserDetails` implements `UserDetails`, and repositories inherit CRUD + query methods from `JpaRepository`.
4. **How is Polymorphism demonstrated?**
   In the `discount/` package, `DiscountStrategyFactory` returns either `PercentageDiscountStrategy` or `FixedAmountDiscountStrategy` behind the `DiscountStrategy` reference, so `calculateDiscount(...)` executes different behavior at runtime. Similarly, `GlobalExceptionHandler` handles different exception subclasses with specialized HTTP responses.
5. **How did you use Java Generics?**
   `PageResponse<T>` is a generic record wrapping paginated results (`List<T>`) for any DTO type cleanly. We also use `JpaRepository<Product, Long>` and `Specification<Product>`.
6. **Why did you use an Enum for `OrderStatus` instead of plain Strings?**
   A String allows invalid typos or illegal status jumps (like `PENDING` straight to `DELIVERED`). Embedding `canTransitionTo(OrderStatus next)` and `isCancellable()` inside the `OrderStatus` enum enforces a strict **State Machine** using OOP.
7. **What is the difference between Composition and Inheritance here?**
   `Order` *has-a* list of `OrderItem` (`Composition` with `CascadeType.ALL` and `orphanRemoval = true`), and `Product` *has-a* `Category` and `User` seller. We chose composition over inheritance because an Order *contains* items rather than *being* an item.
8. **How do you prevent overselling when two customers buy the last item simultaneously?**
   Inside `OrderService.checkout()` (`@Transactional`), products are fetched in sorted ID order (preventing deadlocks) using `productRepository.findByIdForUpdate(id)`, which executes a `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) lock in MySQL until the transaction commits or rolls back.
9. **Why does `OrderItem` store `productNameSnapshot` and `unitPriceSnapshot`?**
   If a seller later edits a product's name or increases its price, historical customer invoices and order totals must never retroactively change.
10. **Why did you use `BigDecimal` instead of `double` for prices and discounts?**
    Floating-point `double` introduces binary rounding errors (e.g., `0.1 + 0.2 != 0.3`). `BigDecimal` with `RoundingMode.HALF_UP` guarantees exact financial calculations.
11. **How do you ensure reviews come only from real buyers?**
    `ReviewService.createReview()` queries `orderRepository.findDeliveredOrdersForProduct(...)` to verify the customer has an order in `DELIVERED` status containing that product, and a database unique constraint `(product_id, customer_id)` prevents duplicate reviews.
12. **How are passwords secured?**
    Passwords are never stored in plain text. `AuthService` hashes passwords with `BCryptPasswordEncoder` (salted adaptive hash), and Spring Security compares hashes during authentication.
13. **What happens in the database when a customer cancels an order?**
    `OrderService.cancelOrder()` verifies ownership and checks `order.getStatus().isCancellable()`, marks the order `CANCELLED`, locks each product, increments `stockQuantity`, and inserts an `ORDER_CANCEL_RESTORE` entry into `inventory_logs` inside one atomic transaction.
14. **How does the frontend communicate with the Java backend?**
    The Vanilla JS layer (`api.js`) sends asynchronous `fetch()` HTTP requests with JSON payloads and same-origin `HttpOnly` session cookies to Spring Boot `@RestController` endpoints under `/api/**`.
15. **Why is `DataSeeder` safe on repeated restarts?**
    It checks `if (userRepository.count() > 0) return;` before inserting demo records, so existing user data and orders in MySQL are preserved across restarts.
