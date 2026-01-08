# Fund Search API (TEFAS Fund Management)

This project is a high-performance backend service designed to parse, store, and search Investment Fund data (TEFAS) using **Java 17**, **Spring Boot 3**, **PostgreSQL**, and **Elasticsearch**.

It demonstrates **Clean Code principles**, **SOLID design**, and modern **Microservices** practices.

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?style=for-the-badge&logo=spring-boot)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-blue?style=for-the-badge&logo=elasticsearch)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker)

---

## 🚀 Features

* **Excel Data Import:** Parses TEFAS Excel reports using **Apache POI**.
* **Auto-Initialization:** Automatically loads sample data (`funds_data.xlsx`) on application startup.
* **Dual Persistence:**
    * **PostgreSQL:** Source of truth for relational data.
    * **Elasticsearch:** Indexed data for high-performance full-text search and filtering.
* **Advanced Search API:**
    * Full-text search on Fund Name and Code.
    * Filtering by Umbrella Type and Returns (1 Month, 1 Year, etc.).
    * Dynamic Sorting and Pagination.
* **Precision Handling:** Uses `BigDecimal` for all financial calculations to prevent floating-point errors.
* **Testing:** Unit tests using **JUnit 5**, **Mockito**.

---

## 🛠 Tech Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.2.x
* **Database:** PostgreSQL 15
* **Search Engine:** Elasticsearch 8.11.1
* **Containerization:** Docker & Docker Compose
* **Tools:** Lombok, Apache POI, Maven

---

## 🐳 Getting Started (The Easy Way)

The project includes a `docker-compose.yml` file to spin up the database, search engine, and the application with a single command.

### Prerequisites
* Docker & Docker Compose installed.

### Steps
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/MenekseYuncu/fund-search-api.git](https://github.com/MenekseYuncu/fund-search-api.git)
    cd fund-search-api
    ```

2.  **Build and Run:**
    ```bash
    docker-compose up -d --build
    ```

3.  **Wait for Startup:**
    * The application will automatically parse the `funds_data.xlsx` file located in `src/main/resources/data`.
    * Check logs to see: `INFO ... DataInitializer : Loading startup data...`

4.  **Access the API:**
    * Base URL: `http://localhost:8080`

---

## 📡 API Documentation

### 1. Search Funds (Advanced)
Performs a search with pagination, filtering, and sorting.

* **URL:** `POST /api/v1/funds/search`
* **Content-Type:** `application/json`

**Example Request Body:**
```json
{
    "pagination": {
        "pageNumber": 1,
        "pageSize": 10
    },
    "filter": {
        "query": "TEKNOLOJİ",
        "umbrellaType": "Hisse Senedi Şemsiye Fonu",
        "minReturn1Year": 50.0
    },
    "sorting": {
        "property": "return1Year",
        "direction": "DESC"
    }
}
```

### 2. Upload Excel File
Manually upload a TEFAS formatted Excel file.

* **URL:** `POST /api/v1/funds/upload`
* **Content-Type:** `multipart/form-data`
* **Param:** `file (Select the .xlsx file)`

---

## 🧪 Running Tests
To run the unit tests:
```bash
mvn test
```

---

## 📂 Package Structure

The project follows a standard **Layered Architecture**, ensuring separation of concerns between the API, Business Logic, and Data Access layers.

```text
src/main/java/com/menekseyuncu/fundsearchservice
├── config/                 # Configuration classes (e.g., DataInitializer)
├── controller/             # REST Controllers (API Layer)
│   └── request/            # Request DTOs & Smart Request Objects
├── model/                  # Domain Models
│   ├── document/           # Elasticsearch Documents (NoSQL)
│   └── entity/             # JPA Entities (Relational)
├── repository/             # Data Access Layer (Interfaces)
└── service/                # Business Logic & Transaction Management

src/test/java/com/menekseyuncu/fundsearchservice
└── service/                # Unit Tests for Services

