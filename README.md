# FoodExpress – Food Delivery Application

FoodExpress is a full-stack food delivery web application built with Java 21, Spring Boot 3, MySQL 8, JWT authentication, and a plain HTML/CSS/JavaScript + Bootstrap frontend.

Roles: **Customer, Restaurant Owner, Delivery Partner, Admin**.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8, or Docker Desktop with Docker Compose
  

## Project layout

```text
food-delivery-app/
├── backend/                 # Spring Boot REST API
│   ├── src/main/java/       # Controllers, services, repositories, entities, security
│   └── src/main/resources/  # application.properties and Flyway migrations
├── frontend/                # HTML, CSS, JavaScript, images
├── serve-frontend.py        # Local static frontend server
├── docker-compose.yml
├── .env.example
└── README.md
```

## Notes and troubleshooting

- The backend context path is `/api`; the frontend API base is configured in `frontend/js/app.js`.
- Database schema and demo data are managed by Flyway migrations. Do not manually create or alter tables unless you also create a migration.
- If port 3306, 8080, or 3000 is already in use, stop the conflicting process or change the corresponding configuration.
- If Maven reports a dependency resolution error, verify internet access and run `mvn -U clean package`.
- If login fails after changing seed data, check that Flyway migrations completed successfully and that the demo accounts were inserted.
- The app requires valid Razorpay credentials for actual gateway transactions; demo checkout is simulated by design.




