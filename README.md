# 🛒 RevShop – Full Stack E-Commerce Web Application

RevShop is a full-stack e-commerce web application developed using *Spring Boot, Spring Security, JWT Authentication, Thymeleaf, Hibernate, and Oracle Database*.
The application provides secure role-based access for *Buyer, Seller, and Admin*, with complete shopping workflow including product management, cart, order processing, payment, and review system.

This project demonstrates real-world backend architecture with layered design, security configuration, and database integration.

---

## 🚀 Project Highlights

✔ Role-based authentication using JWT
✔ Spring Security integration
✔ Buyer & Seller modules
✔ Product management system
✔ Cart & Order workflow
✔ Payment module
✔ Review & Rating system
✔ Upload product images
✔ Log system using Log4j2
✔ Exception handling
✔ Unit testing
✔ ER Diagram & Class Diagram included

---

## 🧱 Architecture

Layered Architecture used:

Controller → Service → Repository → Database

Features:

* REST + MVC Controllers
* Service Interfaces + Implementation
* JPA Repository
* DTO Layer
* Security Layer
* Exception Handling Layer

---

## 🔐 Authentication & Security

* Spring Security
* JWT Token Authentication
* Custom UserDetailsService
* Role-based authorization
* Password encryption

Roles:

* ADMIN
* SELLER
* BUYER

---

## 🛍 Buyer Module

* View products
* Add to cart
* Remove from cart
* Checkout
* View orders
* Wishlist / Favorites
* Notifications
* Invoice generation
* Product reviews

Pages:

* products.html
* cart.html
* checkout.html
* favorites.html
* orders.html
* invoice.html

---

## 🏪 Seller Module

* Seller dashboard
* Add product
* Edit product
* Delete product
* Upload images
* Manage inventory
* View orders

Pages:

* dashboard.html
* inventory.html
* product-form.html
* seller-orders.html

---

## 💳 Payment Module

* Payment Service
* Order creation
* Invoice generation
* Payment status

Interfaces:

* IPaymentService
* IOrderService

---

## ⭐ Review & Rating Module

* Add review
* View review
* Product rating
* Review service

Interfaces:

* IReviewService

---

## 📦 Product Module

* Product CRUD
* Category support
* Image upload
* Product search

Interfaces:

* IProductService

---

## 👤 User Module

* Register
* Login
* Forgot password
* Role assignment
* Address management

Interfaces:

* IUserService

---

## 🛠 Tech Stack

| Technology      | Description          |
| --------------- | -------------------- |
| Java 17         | Programming language |
| Spring Boot     | Backend framework    |
| Spring Security | Authentication       |
| JWT             | Token security       |
| Maven           | Build tool           |
| Hibernate / JPA | ORM                  |
| Oracle DB       | Database             |
| Thymeleaf       | Frontend             |
| HTML / CSS      | UI                   |
| Log4j2          | Logging              |
| JUnit           | Testing              |

---

## 📁 Project Structure


src/main/java/com/rev/app
 ├── controller
 ├── service
 │    ├── Interface
 │    └── Impl
 ├── repository
 ├── entity
 ├── config
 ├── security
 ├── dto
 ├── exception
 └── util

resources
 ├── templates
 ├── static
 ├── application.properties
 ├── schema.sql
 └── log4j2.xml

test
 ├── controller
 ├── service
 └── repository


---

## ⚙️ How to Run Project

### 1 Clone repo


git clone https://github.com/hemanthkunda/P2-Revshop.git


### 2 Open in IntelliJ

Open as Maven Project

### 3 Configure Database

Edit:


application.properties


Example:


spring.datasource.url=jdbc:oracle:thin:@localhost:1521:xe
spring.datasource.username=revshop2
spring.datasource.password=12345


### 4 Run Application

Run:


RevShopP2Application.java


### 5 Open Browser


http://localhost:9090


---

## 📊 Diagrams Included

* ER Diagram
* Class Diagram
* Project Documentation

Files:

* EER Diagram.pdf
* revshop_class_diagram.png
* revshop_er_diagram.png

---

## 🧪 Testing

JUnit tests included for:

* Controller
* Service
* Repository

---

## 📝 Logs

Log4j2 used

Logs folder:


logs/


---

## 👨‍💻 Author
Java Full Stack Developer

Skills:

* Java
* Spring Boot
* SQL / Oracle
* Hibernate
* JWT



---

## ✅ Project Status

✔ Completed
✔ Tested
✔ Ready for submission
✔ Portfolio ready
✔ Interview ready
