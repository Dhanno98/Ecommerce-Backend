# Setup & Running

This guide provides instructions for setting up, configuring, running, and testing the Ecommerce Backend API. It covers both local development using Java and PostgreSQL and containerized development using Docker Compose.

Choose the setup method that best matches your development environment. The Docker-based setup does not require Java, Maven, or PostgreSQL to be installed separately.

## Quick Navigation

- [Running Locally](#running-locally)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Start Redis](#start-redis)
  - [Running the Application](#running-the-application)
- [Running with Docker](#running-with-docker)
  - [Installation](#installation-1)
  - [Configuration](#configuration-1)
  - [Running the Application](#running-the-application-1)
- [Running Tests](#running-tests)

## Running Locally
### Installation

Follow the steps below to install the required software and obtain a local copy of the project.

**1. Prerequisites**

Ensure the following software is installed before proceeding.

| Software | Version |
|----------|----------|
| Java | 21 (LTS) |
| PostgreSQL | 16+ |
| Docker Desktop | Latest stable version |
| Git | Latest stable version |

> **Note:** The application runs locally using Java and PostgreSQL. Redis runs in a Docker container, so Docker Desktop is required for local development. The project has been developed and tested using Java 21.

**2. Clone the Repository**

```bash
git clone https://github.com/Dhanno98/Ecommerce-Backend.git
cd Ecommerce-Backend
```

### Configuration

**1. Configure PostgreSQL**

Create a PostgreSQL database that will be used by the application.

```sql
CREATE DATABASE ecommerce;
```

> **Note:** The required database tables are created automatically by Hibernate when the application starts.

**2. Configure Environment Variables**

The application reads sensitive configuration such as database credentials, JWT signing secrets, and Stripe API keys from environment variables instead of storing them in the source code.

Copy the provided `.env.example` file.

```bash
cp .env.example .env
```

The repository includes a `.env.example` file that documents all required environment variables. Update the values according to your local environment.

Example:

```text
DB_POSTGRES_URL=jdbc:postgresql://localhost:5432/ecommerce
DB_POSTGRES_USERNAME=your_db_username
DB_POSTGRES_PASSWORD=your_db_password

JWT_SECRET=your_base64_encoded_secret

STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxx
REDIS_URL=redis://localhost:6379
```

**3. Export Environment Variables**

Before starting the application, export the environment variables into the same terminal session from which you will execute the Maven commands. If you open a new terminal, the variables must be exported again unless they have been configured permanently.

#### Git Bash / Linux / macOS

```bash
export DB_POSTGRES_URL=jdbc:postgresql://localhost:5432/ecommerce
export DB_POSTGRES_USERNAME=your_db_username
export DB_POSTGRES_PASSWORD=your_db_password
export JWT_SECRET=your_base64_encoded_secret
export STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxx
export REDIS_URL=redis://localhost:6379
```

#### Windows PowerShell

```powershell
$env:DB_POSTGRES_URL="jdbc:postgresql://localhost:5432/ecommerce"
$env:DB_POSTGRES_USERNAME="your_db_username"
$env:DB_POSTGRES_PASSWORD="your_db_password"
$env:JWT_SECRET="your_base64_encoded_secret"
$env:STRIPE_SECRET_KEY="sk_test_xxxxxxxxxxxxxxxxx"
$env:REDIS_URL="redis://localhost:6379"
```

> **Important**
>
> Never commit real credentials, secrets, or API keys to version control.

### Start Redis

Redis is used by the application for caching. For local development, Redis runs in a Docker container while the Spring Boot application and PostgreSQL run directly on the host machine.

Start the Redis container using Docker Compose:

```bash
docker compose up -d redis
```

Verify that the Redis container is running:

```bash
docker compose ps
```
The redis service should be shown as running.

You can also verify the Redis connection directly:

```bash
docker compose exec redis redis-cli ping
```

A successful connection returns:
```text
PONG
```

> **Note:** Redis does not need to be installed separately on your machine. Docker Desktop is used to run the Redis container.

### Running the Application

**1. Build the Project**

Compile the application, execute all unit tests, and generate an executable JAR using the Maven Wrapper.

```bash
./mvnw clean package
```
Integration tests are executed during the Maven `verify` phase and therefore are not run by `./mvnw clean package`.

> **Note:** This project includes the Maven Wrapper (`mvnw`), allowing the project to be built without requiring a globally installed version of Maven. Use the appropriate command for your platform:
> - Windows Command Prompt: `mvnw.cmd`
> - Windows PowerShell: `.\mvnw.cmd`
> - Git Bash, Linux, and macOS: `./mvnw`

**2. Start the Application**

Run the Spring Boot application.

```bash
./mvnw spring-boot:run
```

Alternatively, execute the packaged JAR.

```bash
java -jar target/sb-ecom-0.0.1-SNAPSHOT.jar
```

**3. Verify the Installation**

Once the application has started successfully, the following resources should be available.

| Resource | URL |
|----------|-----|
| REST API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI Specification | `http://localhost:8080/v3/api-docs` |

If the Swagger UI loads successfully, the backend has been configured correctly and is ready to accept requests.

**4. Default Seeded Users**

When the application starts (except under the `test` profile), it automatically creates the following sample users if they do not already exist.

| Role | Username | Password |
|------|----------|----------|
| Customer (`ROLE_USER`) | `user1` | `password1` |
| Seller (`ROLE_SELLER`) | `seller1` | `password2` |
| Administrator (`ROLE_ADMIN`) | `admin` | `adminPass` |

These accounts are intended for development and API testing only.

## Running with Docker

### Installation

Follow the steps below to install the software required to clone and run the application using Docker.

**1. Prerequisites**

Ensure the following software is installed before proceeding.

| Software | Version |
|----------|---------|
| Docker Desktop | Latest stable version |
| Git | Latest stable version |

> **Note:** Docker Desktop includes Docker Engine and Docker Compose. Java, Apache Maven, and PostgreSQL do not need to be installed separately when running the application with Docker.

### Configuration

**1. Configure Environment Variables**

Docker Compose uses the `.env` file to provide sensitive configuration values to the application and PostgreSQL containers.

If you have not already created the `.env` file, copy the provided `.env.example` file:

```bash
cp .env.example .env
```

The repository includes a `.env.example` file that documents the required environment variables. Update the values in `.env` according to your environment.

For the Docker Compose setup, the following variables are required:

```text
DB_POSTGRES_PASSWORD=your_db_password

JWT_SECRET=your_base64_encoded_secret

STRIPE_SECRET_KEY=sk_test_xxxxxxxxxxxxxxxxx
```

The `DB_POSTGRES_PASSWORD` value is used by the PostgreSQL container to initialize the database password and is also passed to the Spring Boot container for database authentication.

The `JWT_SECRET` and `STRIPE_SECRET_KEY` values are passed to the Spring Boot container for JWT signing and Stripe payment processing.

> **Note:** The PostgreSQL database name (`ecommerce`), username (`postgres`), and internal database URL (`jdbc:postgresql://postgres:5432/ecommerce`) are defined by `docker-compose.yml`. Redis is also configured by `docker-compose.yml` and is available to the Spring Boot container at `redis://redis:6379`. The application connects to PostgreSQL and Redis using their Docker Compose service names (`postgres` and `redis`) rather than `localhost`.

> **Important**
>
> Never commit real credentials, secrets, or API keys to version control.

### Running the Application

**1. Build and Start the Containers**

Build the Spring Boot application image and start the application, PostgreSQL, and Redis containers using Docker Compose.

```bash
docker compose up --build
```

The `--build` option ensures that the Spring Boot Docker image is rebuilt before starting the containers.

To run the containers in the background, use:

```bash
docker compose up --build -d
```

**2. Verify the Containers**

Check the status of the running containers:

```bash
docker compose ps
```

The `sb-ecom`, `postgres`, and `redis` services should be running.

**3. Verify the Application**

Once the application has started successfully, the following resources should be available.

| Resource | URL |
|----------|-----|
| REST API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI Specification | `http://localhost:8080/v3/api-docs` |

If the Swagger UI loads successfully, the application has started correctly and is ready to accept requests.

**4. Default Seeded Users**

When the application starts (except under the `test` profile), it automatically creates the following sample users if they do not already exist.

| Role | Username | Password |
|------|----------|----------|
| Customer (`ROLE_USER`) | `user1` | `password1` |
| Seller (`ROLE_SELLER`) | `seller1` | `password2` |
| Administrator (`ROLE_ADMIN`) | `admin` | `adminPass` |

These accounts are intended for development and API testing only.

**5. Stop the Application**

To stop the running containers without removing them:

```bash
docker compose stop
```

The containers and their associated named volumes remain available and can be started again with:

```bash
docker compose start
```

**6. Stop and Remove the Containers**

To stop and remove the containers and Docker Compose network:

```bash
docker compose down
```

Named volumes are not removed, so PostgreSQL data and uploaded product images are preserved.

**7. Remove Containers and Volumes**

To remove the containers, network, and named volumes:

```bash
docker compose down -v
```

> **Warning:** This permanently removes the `postgres-data` and `product-images` volumes. PostgreSQL data and uploaded product images stored in these volumes will be deleted.


## Running Tests

The project contains both unit tests and integration tests.

### Run Unit Tests

Execute only the unit tests.

```bash
./mvnw test
```

### Run Unit and Integration Tests

Execute the complete test suite.

```bash
./mvnw clean verify
```

This command performs the complete Maven verification lifecycle, including:

- Compiles the application
- Executes all unit tests
- Packages the application
- Executes all integration tests
- Verifies the build

---