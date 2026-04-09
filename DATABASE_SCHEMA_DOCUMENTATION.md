# BookMyJuice Database Schema Documentation

**Version:** 2.0  
**Date:** April 1, 2026  
**Status:** LIVE

---

## 📊 Entity Relationship Diagram

```
users (1) ──────────────────────────────── (1) customers
              [users.chargebee_customer_id = customers.id]

product_families (1) ───────────────── (N) items
              [items.product_family_id = product_families.id]

items (1) ──────────────────────────── (N) item_prices
              [item_prices.item_id = items.id]

plans (1) ──────────────────────────── (N) subscriptions
              [subscriptions.plan_id = plans.id]

customers (1) ──────────────────────── (N) subscriptions
              [subscriptions.customer_id = customers.id]

customers (1) ──────────────────────── (N) invoices
              [invoices.customer_id = customers.id]

customers (1) ──────────────────────── (N) orders
              [orders.customer_id = customers.id]

customers (1) ──────────────────────── (N) payments
              [payments.customer_id = customers.id]

invoices (1) ───────────────────────── (N) orders
              [orders.invoice_id = invoices.id]

invoices (1) ───────────────────────── (N) payments
              [payments.invoice_id = invoices.id]

subscriptions (1) ──────────────────── (N) invoices  [nullable]
              [invoices.subscription_id = subscriptions.id]
```

---

## 📋 Table Specifications

### 1. users

**Purpose:** Local user authentication and profile data

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | User ID |
| username | VARCHAR(20) | UNIQUE, NOT NULL | Username (phone or email) |
| email | VARCHAR(50) | UNIQUE, NOT NULL | Email address |
| password | VARCHAR(120) | NOT NULL | BCrypt hashed password |
| first_name | VARCHAR(25) | | First name |
| last_name | VARCHAR(25) | | Last name |
| phone | VARCHAR(20) | | Phone number |
| address | VARCHAR(120) | | Address line 1 |
| extended_addr | VARCHAR(120) | | Address line 2 |
| extended_addr2 | VARCHAR(120) | | Address line 3 |
| city | VARCHAR(120) | | City |
| state | VARCHAR(120) | | State |
| zip | VARCHAR(6) | | ZIP/PIN code |
| country | VARCHAR(2) | | Country code (ISO 3166-1) |
| **chargebee_customer_id** | VARCHAR(50) | **FK → customers.id** | **Chargebee customer ID** ⚠️ ADDED |
| **google_photo_url** | VARCHAR(500) | | **Google photo URL** ⚠️ ADDED |
| **google_id** | VARCHAR(100) | **UNIQUE** | **Google account ID** ⚠️ ADDED |

**Relationships:**
- 1:1 → customers (via chargebee_customer_id)
- N:M → roles (via user_roles)

**⚠️ ISSUE FIXED:** `chargebee_customer_id` field has been ADDED to User entity!

**⚠️ NEW FIELDS ADDED:**
- `google_photo_url` - Stores Google profile photo URL for Google signup users
- `google_id` - Stores unique Google account ID for authentication

---

### Database Migration Required

```sql
-- Add new columns to users table
ALTER TABLE users 
  ADD COLUMN chargebee_customer_id VARCHAR(50) UNIQUE,
  ADD COLUMN google_photo_url VARCHAR(500),
  ADD COLUMN google_id VARCHAR(100) UNIQUE;

-- Add foreign key constraint
ALTER TABLE users 
  ADD CONSTRAINT fk_users_customers 
  FOREIGN KEY (chargebee_customer_id) REFERENCES customers(id);
```

### 2. customers (customer_entity)

**Purpose:** Chargebee customer data (synced from Chargebee)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Chargebee customer ID |
| first_name | VARCHAR(100) | | First name |
| last_name | VARCHAR(100) | | Last name |
| email | VARCHAR(255) | | Email |
| phone | VARCHAR(50) | | Phone |
| company | VARCHAR(100) | | Company name |
| auto_collection | VARCHAR(50) | | Auto collection settings |
| net_term_days | INT | | Net term days |
| allow_direct_debit | BOOLEAN | | Allow direct debit |
| created_at | BIGINT | | Creation timestamp |
| updated_at | BIGINT | | Update timestamp |
| taxability | VARCHAR(50) | | Tax status |
| pii_cleared | VARCHAR(50) | | PII cleared status |
| channel | VARCHAR(50) | | Channel |
| resource_version | BIGINT | | Resource version |
| deleted | BOOLEAN | | Deleted flag |
| promotional_credits | BIGINT | | Promotional credits |
| refundable_credits | BIGINT | | Refundable credits |
| excess_payments | BIGINT | | Excess payments |
| unbilled_charges | BIGINT | | Unbilled charges |
| preferred_currency_code | VARCHAR(3) | | Preferred currency |
| primary_payment_source_id | VARCHAR(50) | | Primary payment source |
| migrated | BOOLEAN | | Migrated flag |

**Relationships:**
- 1:1 ← users (via users.chargebee_customer_id)
- 1:N → subscriptions
- 1:N → invoices
- 1:N → orders
- 1:N → payments
- 1:1 → billing_address
- 1:1 → shipping_address

---

### 3. product_families

**Purpose:** Product categorization (Delight, Signature, Premium)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Product family ID |
| name | VARCHAR(100) | | Family name (Delight/Signature/Premium) |
| description | TEXT | | Description |
| status | VARCHAR(50) | | Status (active/archived) |

**Relationships:**
- 1:N → items

---

### 4. items (item_entity)

**Purpose:** Products/items from Chargebee

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Item ID |
| name | VARCHAR(255) | | Item name |
| description | TEXT | | Description |
| type | VARCHAR(50) | | Type (plan/addon/charge) |
| status | VARCHAR(50) | | Status |
| meta_data | TEXT | | JSON metadata |
| external_name | VARCHAR(255) | | External name |
| enabled_in_portal | BOOLEAN | | Enabled in portal |
| enabled_for_checkout | BOOLEAN | | Enabled for checkout |
| **product_family_id** | VARCHAR(50) | **FK → product_families.id** | **Product family** ⚠️ MISSING |
| unit | VARCHAR(50) | | Unit |
| archived | BOOLEAN | | Archived flag |
| giftable | BOOLEAN | | Giftable flag |
| shippable | BOOLEAN | | Shippable flag |
| deleted | BOOLEAN | | Deleted flag |
| json_object | TEXT | | Full JSON object |

**Relationships:**
- N:1 ← product_families (via product_family_id) ⚠️ MISSING
- 1:N → item_prices

**⚠️ ISSUE:** `product_family_id` field is MISSING from ItemEntity!

---

### 5. item_prices (item_price_entity)

**Purpose:** Item pricing variants (sizes, plans)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Item price ID |
| item_id | VARCHAR(50) | FK → items.id | Parent item ID |
| name | VARCHAR(255) | | Price name (e.g., "200ml", "300ml") |
| description | TEXT | | Description |
| price | DECIMAL(10,2) | | Price amount |
| currency_code | VARCHAR(3) | | Currency code |
| pricing_model | VARCHAR(50) | | Pricing model |
| period | INT | | Billing period |
| period_unit | VARCHAR(50) | | Period unit (day/week/month) |
| status | VARCHAR(50) | | Status |
| trial_period | INT | | Trial period |
| trial_period_unit | VARCHAR(50) | | Trial period unit |

**Relationships:**
- N:1 ← items (via item_id)

---

### 6. plans

**Purpose:** Subscription plans from Chargebee

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Plan ID |
| name | VARCHAR(255) | | Plan name |
| description | TEXT | | Description |
| price | DECIMAL(10,2) | | Price |
| currency_code | VARCHAR(3) | | Currency |
| period | INT | | Billing period |
| period_unit | VARCHAR(50) | | Period unit |
| status | VARCHAR(50) | | Status |

**Relationships:**
- 1:N → subscriptions

---

### 7. subscriptions (subscription_entity)

**Purpose:** Active subscriptions

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Subscription ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| plan_id | VARCHAR(50) | FK → plans.id | Plan ID |
| status | VARCHAR(50) | | Status (active/cancelled/expired) |
| created_at | BIGINT | | Creation timestamp |
| started_at | BIGINT | | Start timestamp |
| current_term_start | BIGINT | | Current term start |
| current_term_end | BIGINT | | Current term end |
| cancelled_at | BIGINT | | Cancellation timestamp |
| cancel_reason | VARCHAR(255) | | Cancellation reason |

**Relationships:**
- N:1 ← customers (via customer_id)
- N:1 ← plans (via plan_id)
- 1:N → invoices (nullable)

---

### 8. invoices (invoice_entity)

**Purpose:** Invoice records from Chargebee

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Invoice ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| subscription_id | VARCHAR(50) | FK → subscriptions.id | Subscription ID (nullable) |
| total | DECIMAL(10,2) | | Total amount |
| amount_paid | DECIMAL(10,2) | | Amount paid |
| amount_due | DECIMAL(10,2) | | Amount due |
| status | VARCHAR(50) | | Status (paid/pending/void) |
| created_at | BIGINT | | Creation timestamp |
| due_date | BIGINT | | Due date |
| paid_at | BIGINT | | Paid timestamp |

**Relationships:**
- N:1 ← customers (via customer_id)
- N:1 ← subscriptions (via subscription_id, nullable)
- 1:N → orders
- 1:N → payments

---

### 9. orders (order_entity)

**Purpose:** Order records from Chargebee

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Order ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| invoice_id | VARCHAR(50) | FK → invoices.id | Invoice ID |
| total | DECIMAL(10,2) | | Total amount |
| status | VARCHAR(50) | | Status |
| created_at | BIGINT | | Creation timestamp |

**Relationships:**
- N:1 ← customers (via customer_id)
- N:1 ← invoices (via invoice_id)

---

### 10. payments (payment_entity)

**Purpose:** Payment records from Chargebee

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | VARCHAR(50) | PRIMARY KEY | Payment ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| invoice_id | VARCHAR(50) | FK → invoices.id | Invoice ID |
| amount | DECIMAL(10,2) | | Amount |
| status | VARCHAR(50) | | Status |
| method | VARCHAR(50) | | Payment method |
| created_at | BIGINT | | Creation timestamp |

**Relationships:**
- N:1 ← customers (via customer_id)
- N:1 ← invoices (via invoice_id)

---

### 11. billing_address (billing_address_entity)

**Purpose:** Customer billing addresses

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Address ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| line1 | VARCHAR(255) | | Address line 1 |
| line2 | VARCHAR(255) | | Address line 2 |
| line3 | VARCHAR(255) | | Address line 3 |
| city | VARCHAR(100) | | City |
| state | VARCHAR(100) | | State |
| zip | VARCHAR(20) | | ZIP/PIN code |
| country | VARCHAR(2) | | Country code |

**Relationships:**
- 1:1 → customers

---

### 12. shipping_address (shipping_address_entity)

**Purpose:** Customer shipping addresses

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Address ID |
| customer_id | VARCHAR(50) | FK → customers.id | Customer ID |
| line1 | VARCHAR(255) | | Address line 1 |
| line2 | VARCHAR(255) | | Address line 2 |
| line3 | VARCHAR(255) | | Address line 3 |
| city | VARCHAR(100) | | City |
| state | VARCHAR(100) | | State |
| zip | VARCHAR(20) | | ZIP/PIN code |
| country | VARCHAR(2) | | Country code |

**Relationships:**
- 1:1 → customers

---

### 13. user_roles

**Purpose:** User-role mapping (many-to-many)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| user_id | BIGINT | FK → users.id, PRIMARY KEY | User ID |
| role_id | INT | FK → roles.id, PRIMARY KEY | Role ID |

**Relationships:**
- N:M ← users ↔ roles

---

### 14. roles

**Purpose:** User roles (USER, ADMIN, MODERATOR)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | INT | PRIMARY KEY, AUTO_INCREMENT | Role ID |
| name | VARCHAR(50) | UNIQUE, NOT NULL | Role name (ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR) |

**Relationships:**
- N:M ↔ users (via user_roles)

---

## ⚠️ CRITICAL ISSUES FOUND

### Issue 1: Missing `chargebee_customer_id` in User Entity

**Impact:** Cannot link local users to Chargebee customers

**Fix Required:**
```java
// In User.java
@Column(name = "chargebee_customer_id", unique = true)
private String chargebeeCustomerId;

// Getters and setters
public String getChargebeeCustomerId() { return chargebeeCustomerId; }
public void setChargebeeCustomerId(String chargebeeCustomerId) { this.chargebeeCustomerId = chargebeeCustomerId; }
```

**Database Migration:**
```sql
ALTER TABLE users ADD COLUMN chargebee_customer_id VARCHAR(50) UNIQUE;
ALTER TABLE users ADD CONSTRAINT fk_users_customers 
    FOREIGN KEY (chargebee_customer_id) REFERENCES customers(id);
```

---

### Issue 2: Missing `product_family_id` in ItemEntity

**Impact:** Cannot categorize items by product family (Delight/Signature/Premium)

**Fix Required:**
```java
// In ItemEntity.java
@Column(name = "product_family_id")
private String productFamilyId;

@ManyToOne
@JoinColumn(name = "product_family_id")
private ProductFamilyEntity productFamily;

// Getters and setters
public String getProductFamilyId() { return productFamilyId; }
public void setProductFamilyId(String productFamilyId) { this.productFamilyId = productFamilyId; }
```

**Database Migration:**
```sql
CREATE TABLE product_families (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    description TEXT,
    status VARCHAR(50)
);

ALTER TABLE items ADD COLUMN product_family_id VARCHAR(50);
ALTER TABLE items ADD CONSTRAINT fk_items_product_families 
    FOREIGN KEY (product_family_id) REFERENCES product_families(id);
```

---

## 📊 Data Flow

### User Signup Flow
```
1. User signs up → users table
2. Create Chargebee customer → customers table
3. Update users.chargebee_customer_id → Link user to customer
```

### Product Catalog Sync
```
1. Chargebee webhook: item.created → items table
2. Chargebee webhook: item_price.created → item_prices table
3. Chargebee webhook: product_family.created → product_families table
4. Link items.product_family_id → product_families.id
```

### Subscription Purchase
```
1. User selects plan → plans table
2. Create subscription in Chargebee → subscriptions table
3. Link subscriptions.customer_id → customers.id
4. Link subscriptions.plan_id → plans.id
```

### Invoice Generation
```
1. Chargebee creates invoice → invoices table
2. Link invoices.customer_id → customers.id
3. Link invoices.subscription_id → subscriptions.id (nullable)
```

### Payment Processing
```
1. Payment received → payments table
2. Link payments.customer_id → customers.id
3. Link payments.invoice_id → invoices.id
```

---

## 🔧 Maintenance

### Regular Tasks
- [ ] Daily: Sync Chargebee data via webhooks
- [ ] Weekly: Verify user-customer link integrity
- [ ] Monthly: Clean up orphaned records
- [ ] Quarterly: Review and optimize indexes

### Monitoring Queries
```sql
-- Find users without Chargebee customer link
SELECT * FROM users WHERE chargebee_customer_id IS NULL;

-- Find items without product family
SELECT * FROM items WHERE product_family_id IS NULL;

-- Find orphaned subscriptions
SELECT * FROM subscriptions s 
LEFT JOIN customers c ON s.customer_id = c.id 
WHERE c.id IS NULL;
```

---

**Last Updated:** April 1, 2026  
**Next Review:** May 1, 2026  
**Maintained By:** BookMyJuice Engineering Team
