# 🛒 E-Commerce Core REST API

[![Java CI with Maven](https://github.com/yusufkayiss/ecommerce-core-api/actions/workflows/maven.yml/badge.svg)](https://github.com/yusufkayiss/ecommerce-core-api/actions/workflows/maven.yml)

Production-ready, clean architecture principles, and secure E-Commerce Backend API built with Spring Boot.

---

## 🌐 English

### 🚀 Overview
A robust, scalable, and enterprise-grade backend REST API designed for e-commerce applications. Built following industry standards, clean DTO mappings, transactional service layers, and global exception handling.

### 🛠 Tech Stack
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security & Stateless JWT Authentication (`@AuthenticationPrincipal`)
- **Persistence**: Spring Data JPA / Hibernate
- **Database**: PostgreSQL / MySQL
- **Documentation**: OpenAPI 3.0 / Swagger UI
- **Utilities**: Lombok

### 📐 System Architecture

```mermaid
graph TD
    Client[Client / Postman] -->|HTTP Request + Bearer JWT| Security[Spring Security Filter Chain]
    Security -->|Authenticated Principal| Controller[REST Controllers]
    Controller -->|DTO Mapping| Service[Service Layer @Transactional]
    Service -->|Business Logic & Validation| Repo[Spring Data JPA Repositories]
    Repo -->|SQL Queries| DB[(PostgreSQL Database)]
    
    GlobalHandler[@RestControllerAdvice] -.->|Captures Exceptions & Returns Error JSON| Client
```

### ✨ Architectural Highlights
- **Stateless JWT Security**: End-to-end authentication. Authenticated user details contextually bound via `@AuthenticationPrincipal`.
- **Decoupled DTO Architecture**: Custom response DTOs (`CartResponse`, `OrderResponse`, etc.) eliminating Hibernate proxy circular dependencies and serialization issues.
- **Transactional Order & Inventory Control**: Real-time stock verification, automated stock deduction, and atomic cart clearing upon order placement.
- **Global Exception Handling**: Centralized `@RestControllerAdvice` capturing runtime exceptions and mapping them to standardized `ErrorResponse` JSON payloads.

### 📖 API Documentation & Endpoints

Interactive Swagger UI is available when running the application locally:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

#### 🔐 Authentication
- `POST /api/v1/auth/register` - Register a new user
- `POST /api/v1/auth/authenticate` - Login and retrieve JWT token

#### 🛒 Cart Management
- `GET /api/v1/cart` - Fetch user's active cart
- `POST /api/v1/cart/add` - Add item to cart
- `DELETE /api/v1/cart/items/{itemId}` - Remove item from cart

#### 📦 Order Management
- `POST /api/v1/orders` - Convert active cart to an order
- `GET /api/v1/orders` - Fetch user order history

### 💻 Getting Started (Installation)

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/yusufkayiss/ecommerce-core-api.git](https://github.com/yusufkayiss/ecommerce-core-api.git)
   cd ecommerce-core-api
   ```

2. **Option A: Run via Docker Compose (Recommended)**
   ```bash
   docker compose up -d
   ```

3. **Option B: Run Locally via Maven**
   - Update database credentials in `src/main/resources/application.yml`.
   - Build and run:
     ```bash
     mvn clean install
     mvn spring-boot:run
     ```

---

## 📍 Türkçe

### 🚀 Genel Bakış
Spring Boot 3, Spring Security (JWT) ve temiz mimari prensipleri kullanılarak geliştirilmiş, üretime hazır (production-ready) E-Ticaret Backend REST API projesi.

### 🛠 Teknolojiler
- **Dil**: Java 17+
- **Framework**: Spring Boot 3.x
- **Güvenlik**: Spring Security & Stateless JWT Kimlik Doğrulama (`@AuthenticationPrincipal`)
- **Veri Katmanı**: Spring Data JPA / Hibernate
- **Veritabanı**: PostgreSQL / MySQL
- **Dokümantasyon**: OpenAPI 3.0 / Swagger UI
- **Araçlar**: Lombok

### 📐 Sistem Mimarisi

```mermaid
graph TD
    Client[İstemci / Postman] -->|HTTP İsteği + Bearer JWT| Security[Spring Security Filtre Zinciri]
    Security -->|Yetkilendirilmiş Kullanıcı| Controller[REST Denetleyicileri]
    Controller -->|DTO Dönüşümü| Service[Servis Katmanı @Transactional]
    Service -->|İş Mantığı ve Doğrulama| Repo[Spring Data JPA Depoları]
    Repo -->|SQL Sorguları| DB[(PostgreSQL Veritabanı)]
    
    GlobalHandler[@RestControllerAdvice] -.->|Hataları Yakalar ve JSON Döner| Client
```

### ✨ Öne Çıkan Mimari Özellikler
- **Stateless JWT Güvenlik Mimarisi**: Uçtan uca kimlik doğrulama. Oturum açmış kullanıcı bilgileri `@AuthenticationPrincipal` ile controller katmanına aktarılır.
- **Temiz DTO Katmanı**: Hibernate proxy ve dairesel bağımlılık (circular reference) hatalarını engelleyen özel DTO (`CartResponse`, `OrderResponse` vb.) yapısı.
- **Sipariş & Stok Yönetimi**: Sipariş verme sırasında anlık stok kontrolü, otomatik stok düşümü ve sepet sıfırlama işlemlerinin `@Transactional` bütünlükle yürütülmesi.
- **Merkezi Hata Yönetimi**: `@RestControllerAdvice` ile sistemdeki istisnaların yakalanıp zaman damgalı standart `ErrorResponse` JSON çıktılarına dönüştürülmesi.

### 📖 API Dokümantasyonu & Endpoint'ler

Uygulama yerelde çalışırken interaktif Swagger arayüzüne erişebilirsiniz:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Şeması**: `http://localhost:8080/v3/api-docs`

#### 🔐 Kimlik Doğrulama (Auth)
- `POST /api/v1/auth/register` - Yeni kullanıcı kaydı
- `POST /api/v1/auth/authenticate` - Giriş yapma ve JWT Token alımı

#### 🛒 Sepet Yönetimi (Cart)
- `GET /api/v1/cart` - Kullanıcı sepetini getirme
- `POST /api/v1/cart/add` - Sepete ürün ekleme
- `DELETE /api/v1/cart/items/{itemId}` - Sepetten ürün silme

#### 📦 Sipariş İşlemleri (Orders)
- `POST /api/v1/orders` - Sepetteki ürünlerle sipariş oluşturma
- `GET /api/v1/orders` - Kullanıcı sipariş geçmişini listeleme

### 💻 Kurulum Adımları

1. **Repoyu Klonlayın:**
   ```bash
   git clone [https://github.com/yusufkayiss/ecommerce-core-api.git](https://github.com/yusufkayiss/ecommerce-core-api.git)
   cd ecommerce-core-api
   ```

2. **Yol A: Docker Compose ile Çalıştırma (Önerilen)**
   ```bash
   docker compose up -d
   ```

3. **Yol B: Maven ile Lokal Çalıştırma**
   - `src/main/resources/application.yml` içindeki veritabanı bilgilerini güncelleyin.
   - Derleyin ve çalıştırın:
     ```bash
     mvn clean install
     mvn spring-boot:run
     ```
