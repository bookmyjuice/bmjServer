# BookMyJuice Backend Server (bmjServer)

Spring Boot backend for BookMyJuice.

## ⚠️ Security Warning
**NEVER commit real secrets, passwords, or API keys to this repository.**
This project uses environment variables for all sensitive configuration.
If you have sensitive values, define them in your environment or a local `.env` file (which is ignored by `.gitignore`).

---

## ⚙️ Configuration

The application requires the following environment variables to function. 
Defaults are provided for development where marked `optional`.

| Variable | Description | Required | Default |
| :--- | :--- | :--- | :--- |
| **Database** | | | |
| `DB_USERNAME` | Database username | Yes | `bmj` |
| `DB_PASSWORD` | Database password | Yes | - |
| **Security** | | | |
| `ADMIN_USER` | Default Spring Security admin username | Yes | - |
| `ADMIN_PASSWORD` | Default Spring Security admin password | Yes | - |
| `JWT_SECRET` | Secret key for signing JWT tokens | Yes | - |
| **Chargebee** | | | |
| `CHARGEBEE_SITE` | Chargebee site name | Yes | `bookmyjuice-test` |
| `CHARGEBEE_API_KEY` | Chargebee API secret key | Yes | - |
| **Webhooks** | | | |
| `WEBHOOK_USERNAME` | Basic auth username for webhook endpoints | Yes | - |
| `WEBHOOK_PASSWORD` | Basic auth password for webhook endpoints | Yes | - |
| **Email** | | | |
| `MAIL_HOST` | SMTP Host | No | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP Port | No | `587` |
| `MAIL_USERNAME` | SMTP Username | No | `support@bookmyjuice.co.in` |
| `MAIL_PASSWORD` | SMTP Password | No | - |
| `MAIL_FROM` | From Email Address | No | `support@bookmyjuice.co.in` |

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL Database running

### 1. Setup Environment Variables
Create a `.env` file in the project root (not committed to git) or export the variables listed above.

```bash
# Example .env
DB_USERNAME=bmj
DB_PASSWORD=your_db_pass
JWT_SECRET=your_secret_key_must_be_long_enough
ADMIN_USER=admin
ADMIN_PASSWORD=admin_pass
CHARGEBEE_API_KEY=your_cb_api_key
WEBHOOK_USERNAME=webhook_user
WEBHOOK_PASSWORD=webhook_pass
```

### 2. Build the Application
```bash
mvn clean package -DskipTests
```

### 3. Run the Application

**Run with Default Profile:**
```bash
java -jar target/bmjServer-*.jar
```

**Run with Development Profile (verbose logging, dev configs):**
```bash
java -jar target/bmjServer-*.jar --spring.profiles.active=dev
```

**Run with Production Profile (secure configs):**
```bash
java -jar target/bmjServer-*.jar --spring.profiles.active=prod
```

---

## 🧪 Testing

Run the test suite:
```bash
mvn test
```

---

## 📖 Documentation
- [Architecture Overview](docs/ARCHITECTURE_OVERVIEW.md)
- [API Documentation](docs/API.md)
