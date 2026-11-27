# FoodExpress – Food Delivery Application

FoodExpress is a full-stack food delivery web application built with Java 21, Spring Boot 3, MySQL 8, JWT authentication, and a plain HTML/CSS/JavaScript + Bootstrap frontend.

Roles: **Customer, Restaurant Owner, Delivery Partner, Admin**.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- MySQL 8, or Docker Desktop with Docker Compose
- Python 3 (only for the lightweight local frontend server)

## Run with Docker (recommended)

1. Copy `.env.example` to `.env` in the project root.
2. Review the values. The supplied values are for local development only.
3. Run:

```bash
docker compose up --build
```

Open:

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html

To stop the containers:

```bash
docker compose down
```

To also delete the local MySQL volume and all local demo data:

```bash
docker compose down -v
```

## Run locally on Windows

Open PowerShell in the project root.

1. Ensure MySQL 8 is running. The JDBC URL creates `food_delivery_db` automatically when the MySQL account has permission.
2. Set your local database credentials (adjust the password as needed):

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

3. Build and start the backend:

```powershell
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

4. Open a second PowerShell window in the project root and start the frontend:

```powershell
py serve-frontend.py
```

Open http://localhost:3000.

## Run locally on macOS/Linux

```bash
export DB_USERNAME=root
export DB_PASSWORD='your_mysql_password'
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

In a second terminal, from the project root:

```bash
python3 serve-frontend.py
```

## Demo accounts

The database migrations seed local demo users. All demo passwords are `Demo@1234`, except the admin password shown below.

| Role | Email | Password |
|---|---|---|
| Admin | admin@foodapp.com | Admin@123 |
| Customer | customer@demo.com | Demo@1234 |
| Delivery Partner | partner@demo.com | Demo@1234 |
| Restaurant Owners | owner1@demo.com through owner30@demo.com | Demo@1234 |

A newly registered restaurant must be approved by the admin before customers can browse it.

Demo coupons: `WELCOME50`, `FLAT30`, `SAVE20`.

## Payment configuration

The default local configuration uses `PAYMENT_DEMO=true`. This is a simulated checkout for development and does not move money through Razorpay.

To use Razorpay Test Mode:

1. Create Razorpay test API credentials in your Razorpay dashboard.
2. Set `PAYMENT_DEMO=false`.
3. Set `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` in your environment or `.env` file.
4. Restart the backend.

Never put the Razorpay secret in frontend code or commit real credentials. Do not use demo mode in production.

## Configuration

See `.env.example` for the supported environment variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_ACCESS_EXP`, `JWT_REFRESH_EXP`
- `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `PAYMENT_DEMO`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `UPLOAD_DIR`

The example JWT secret and passwords are development placeholders. Generate a unique strong Base64-encoded JWT secret and use secret management for any deployed environment.

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


## Run the application locally (Windows)

1. Start MySQL 8 and make sure the `food_delivery_db` database can be created by your MySQL user.
2. Open PowerShell in the `backend` folder. Set the database password in that same PowerShell window:
   ```powershell
   $secure = Read-Host "Enter MySQL password" -AsSecureString
   $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $secure).Password
   ```
3. Start the full application:
   ```powershell
   mvn spring-boot:run
   ```
4. Open **http://localhost:8080/api/index.html**. The frontend is now served by Spring Boot; Python and Node.js are not required.

The frontend files are also kept in `frontend/` and are copied into Spring Boot's static resources at `backend/src/main/resources/static/`. If you change frontend files later, copy the updated files into that static folder before rebuilding.


## Visual design refresh
The frontend now uses a warm cream, forest-green and lime palette inspired by the supplied farm-stand reference. Food cards use rounded, real-food Unsplash photography as a fallback when no restaurant/menu image has been uploaded. Restaurant logo fallbacks use a matching forest-green monogram. Replace fallback photos with licensed restaurant-owned images for production use.
