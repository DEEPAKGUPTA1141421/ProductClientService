# ProductClientService

**The central product catalog and commerce backend for a multi-seller marketplace platform.**

---

## What We Built

ProductClientService is a production-grade Spring Boot microservice that forms the core of a multi-vendor e-commerce system. It handles everything from seller onboarding and product catalog management to buyer-facing search, cart, wishlist, reviews, and personalized recommendations — all served via a clean REST API and driven by an event-driven architecture.

The service acts as the **single source of truth** for products, sellers, buyers, and shopping activity. It communicates with companion microservices (Order/Payment, Delivery/Inventory) via Kafka events and Feign HTTP clients.

---

## System Architecture

```
                        ┌────────────────────────────────────────────────────────┐
                        │               ProductClientService                      │
                        │                                                        │
  Flutter App  ─────►  │  REST API (Spring Boot 3.3.4)                          │
  Admin Panel  ─────►  │  ├─ Auth (JWT RS256)                                   │
  Rider App    ─────►  │  ├─ Product Catalog & Search                           │
                        │  ├─ Cart, Wishlist, Coupons                            │
                        │  ├─ Seller Onboarding & Listings                      │
                        │  ├─ Reviews & Ratings                                  │
                        │  ├─ Recommendations                                    │
                        │  └─ Admin Management                                   │
                        └──────────┬─────────────────────┬──────────────────────┘
                                   │                     │
          ┌───────────────────┬────┘                     └──────────────────┐
          │                   │                                              │
    PostgreSQL          Elasticsearch                                      Redis
   (primary DB)      (product search)                                   (cache)
          │                   │                                              │
          └───────────────────┴──────────────────────────────────────────────┘
                                   │
                               Apache Kafka
                    (event streaming to companion services)
                                   │
            ┌──────────────────────┼──────────────────────┐
            │                      │                       │
    OrderPaymentService   DeliveryInventoryService   (SMS/Email consumers)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language & Runtime** | Java 17 |
| **Framework** | Spring Boot 3.3.4 |
| **Primary Database** | PostgreSQL (with PostGIS for geospatial) |
| **Search Engine** | Elasticsearch 8.14.1 |
| **Cache** | Redis |
| **Message Broker** | Apache Kafka |
| **Auth** | JWT (RS256 asymmetric) via JJWT 0.11.5 |
| **ORM** | Spring Data JPA / Hibernate |
| **HTTP Clients** | Spring Cloud OpenFeign |
| **File Storage** | AWS S3 + Cloudinary |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |
| **Maps / Geocoding** | Google Maps API |
| **QR Codes** | ZXing |
| **Monitoring** | Spring Actuator + Prometheus |
| **Build Tool** | Maven |

---

## Key Domains

### Authentication
Phone-based OTP login for both buyers and sellers. Issues short-lived JWTs (RS256, ~2.5h) and long-lived refresh tokens. Roles: `ROLE_USER`, `ROLE_SELLER`, `ROLE_ADMIN`.

### Product Catalog
Multi-level hierarchical categories (`SUPER_CATEGORY → CATEGORY → SUBCATEGORY → SUBSUBCATEGORY`). Products have variants (SKU, price, stock, combination like `{Color: Red, Size: M}`), attributes, media, and tags. Products can optionally link to a **standard catalog** managed by admins.

### Elasticsearch Search
Products are indexed into Elasticsearch when they go live. Search supports full-text queries, category/brand/price filters, sorting, and pagination. Results cached in Redis by query hash. Autocomplete powered by pre-generated **search intents**.

### Cart & Coupons
Per-user active cart supporting item quantity changes, coupon application (item-level and cart-level), tax (GST 18%), delivery charges (free above ₹500), and service charges. All monetary values stored as strings to avoid floating-point drift.

### Wishlist
Users can wishlist products, subscribe to price-drop alerts (triggers when price drops ≥20%), move items to cart, and share wishlists via a time-limited token.

### Reviews & Ratings
Only verified purchasers can submit reviews (checked against `user.purchasedProductIds`). Reviews support image uploads (Cloudinary), helpful votes, and are ranked by: verified purchase → helpful votes → recency. Product's `averageRating` and `ratingCount` are denormalized for fast reads.

### Seller Onboarding
Multi-stage KYC flow: `REGISTER → LOCATION → BASIC_INFO → BUSINESS_INFO → BANK_ACCOUNT → AADHAAR → DOCUMENT_VERIFICATION_PENDING → DOCUMENT_VERIFIED`. Sellers cannot list products until verified. Supports Aadhaar identity verification.

### Seller Product Listing
Step-by-step product creation flow: name → attributes → variants → images → brand/tags → publish. Publishing a product fires a `product.live` Kafka event which triggers Elasticsearch indexing.

### Recommendations
Context-aware recommendations (`HOME`, `PDP`, `CART`) via `RecoOrchestrator`. Backed by user interaction history, purchases, and wishlist data. Feedback (clicks, cart-adds) logged to Kafka for model improvement.

### Admin Panel API
Admins manage categories, attributes, brands, seller approvals, filters, returns, and wallets. Standard catalog products are admin-managed.

### Internal APIs
Service-to-service endpoints (protected by `X-Internal-Api-Key` header) for purchase recording, cart sync, FCM dispatch, and recommendation cache pre-warming.

---

## Event-Driven Flows (Kafka Topics)

| Topic | Published When |
|---|---|
| `product.live` | Product published by seller |
| `product.viewed` | Buyer views product detail |
| `product.cart_added` | Item added to cart |
| `product.wishlisted` | Item added to wishlist |
| `order.completed` | Order confirmed |
| `order.returned` | Return initiated |
| `review.submit.requested` | Review submission started |
| `review.submitted` | Review published |
| `review.helpful` | Helpful vote cast |
| `user.interaction` | Click / view event |
| `seller.live` | Seller goes live |

---

## Caching Strategy (Redis)

| Key Pattern | Content | Invalidated On |
|---|---|---|
| `cart:{userId}` | Active cart state | Every cart mutation |
| `products:{query_hash}` | Search results page | Admin flush or category change |
| `category:{id}` | Category subtree | Category update |
| `trending:searches` | Top 10 keywords | Hourly refresh |
| `wishlist:{userId}` | Wishlist items | Wishlist mutation |
| `reco:{userId}:{context}:{k}` | Recommendations | Purchase / view event |

---

## API Surface (Summary)

| Domain | Endpoints |
|---|---|
| Auth | Login (OTP), verify, refresh, logout |
| Product & Search | Search, autocomplete, trending, category tree, product detail, ratings |
| Cart | Add/update/remove items, apply/remove coupons, clear cart |
| Wishlist | Add/remove, move-to-cart, share, price-drop alerts |
| Reviews | Submit, list, helpful vote, delete |
| User Profile | Get/update profile, avatar, addresses |
| Seller Portal | Create/update/delete products, business settings, notification prefs |
| Recommendations | Get recommendations, submit feedback |
| Admin | Category/attribute/brand management, seller approval, filter control |
| Internal | Purchase recording, cart sync, FCM, reco prewarm |

Total: **80+ REST endpoints** across public, seller, admin, and internal tiers.

---

## Project Structure

```
src/main/java/com/ProductClientService/ProductClientService/
├── Configuration/         # Spring Security, S3, Redis, Cloudinary beans
├── Controller/            # REST controllers (buyer, seller/, admin/, internal/)
├── Service/               # Business logic (cart/, seller/, admin/, reco/, kafka/)
├── Repository/            # Spring Data JPA + custom Elasticsearch repos
├── Model/                 # JPA entities (Product, User, Seller, Cart, Coupon…)
├── DTO/                   # Request/response objects
├── Utils/                 # Rate limiter, validators, converters, cron jobs
├── filter/                # JwtAuthenticationFilter, InternalApiKeyFilter
└── network/               # Feign clients for OrderPaymentService, DeliveryInventoryService

src/main/resources/
├── application.properties # All config (DB, Redis, Kafka, S3, ES…)
└── keys/                  # RSA private/public key pair for JWT signing
```

---

## Security Model

- **JWT RS256** — asymmetric signing; public key can be shared with other services for verification without sharing the secret
- **OTP brute-force protection** — `RateLimiter` utility blocks repeated attempts
- **Role-based access control** — `@PreAuthorize` guards on seller and admin endpoints
- **Internal API key** — separate filter (`InternalApiKeyFilter`) for service-to-service calls
- **Verified-purchase gate** — reviews only allowed if `productId` is in buyer's `purchasedProductIds`

---

## Companion Microservices

| Service | Role | Communication |
|---|---|---|
| **OrderPaymentNotificationService** | Order placement, payment processing, notifications | Feign HTTP + Kafka |
| **DeliveryInventoryService** | Inventory tracking, delivery assignment, rider management | Feign HTTP + Kafka |

---

## Feature Flags (`application.properties`)

| Flag | Default | Purpose |
|---|---|---|
| `reco.serving.enabled` | `false` | Toggle ML recommendation endpoint |
| `reco.degradedMode` | `false` | Serve precomputed recos during peak load |
| `reco.similarity.vectorsEnabled` | `false` | Switch to vector similarity (ML model) |
| `testOtpMode` | `false` | Use fixed OTP `6666` in dev/test |
| `aadhaar.verification.bypass` | `true` | Skip Aadhaar KYC for development |

---

## Running Locally

**Prerequisites:** Java 17, PostgreSQL, Elasticsearch, Redis, Kafka

```bash
# 1. Set environment variables (see .env.example)
# 2. Start dependencies (Docker recommended)
docker-compose up -d

# 3. Run the service
./mvnw spring-boot:run
```

**Health check:** `GET http://localhost:8080/actuator/health`
**Metrics:** `GET http://localhost:8080/actuator/prometheus`
