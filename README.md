# 🛒 E-Commerce Core REST API

Production-ready, clean architecture principles, and secure E-Commerce Backend API built with Spring Boot.

---

## 🇬🇧 English

### 🚀 Overview
A robust, scalable, and enterprise-grade backend REST API designed for e-commerce applications. Built following industry standards, clean DTO mappings, transactional service layers, and global exception handling.

### 🛠 Tech Stack
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Security**: Spring Security & Stateless JWT Authentication (`@AuthenticationPrincipal`)
- **Persistence**: Spring Data JPA / Hibernate
- **Database**: PostgreSQL / MySQL
- **Utilities**: Lombok

### ✨ Architectural Highlights
- **Stateless JWT Security**: End-to-end authentication. Authenticated user details contextually bound via `@AuthenticationPrincipal`.
- **Decoupled DTO Architecture**: Custom response DTOs (`CartResponse`, `OrderResponse`, etc.) eliminating Hibernate proxy circular dependencies and serialization issues.
- **Transactional Order & Inventory Control**: Real-time stock verification, automated stock deduction, and atomic cart clearing upon order placement.
- **Global Exception Handling**: Centralized `@RestControllerAdvice` capturing runtime exceptions and mapping them to standardized `ErrorResponse` JSON payloads.

### 📌 API Endpoints

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

---

## 🇹🇷 Türkçe

### 🚀 Genel Bakış
Spring Boot 3, Spring Security (JWT) ve temiz mimari prensipleri kullanılarak geliştirilmiş, üretime hazır (production-ready) E-Ticaret Backend REST API projesi.

### 🛠 Teknolojiler
- **Dil**: Java 17+
- **Framework**: Spring Boot 3.x
- **Güvenlik**: Spring Security & Stateless JWT Kimlik Doğrulama (`@AuthenticationPrincipal`)
- **Veri Katmanı**: Spring Data JPA / Hibernate
- **Veritabanı**: PostgreSQL / MySQL
- **Araçlar**: Lombok

### ✨ Öne Çıkan Mimari Özellikler
- **Stateless JWT Güvenlik Mimarisi**: Uçtan uca kimlik doğrulama. Oturum açmış kullanıcı bilgileri `@AuthenticationPrincipal` ile controller katmanına aktarılır.
- **Temiz DTO Katmanı**: Hibernate proxy ve dairesel bağımlılık (circular reference) hatalarını engelleyen özel DTO (`CartResponse`, `OrderResponse` vb.) yapısı.
- **Sipariş & Stok Yönetimi**: Sipariş verme sırasında anlık stok kontrolü, otomatik stok düşümü ve sepet sıfırlama işlemlerinin `@Transactional` bütünlükle yürütülmesi.
- **Merkezi Hata Yönetimi**: `@RestControllerAdvice` ile sistemdeki istisnaların yakalanıp zaman damgalı standart `ErrorResponse` JSON çıktılarına dönüştürülmesi.

### 📌 API Endpoint Özeti

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