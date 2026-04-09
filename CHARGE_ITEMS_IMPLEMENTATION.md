# GET /api/test/charge-items - Implementation Summary

**Date:** March 29, 2026  
**Status:** Implementation Complete  
**Related Requirements:** FR-PROD-001, FR-PROD-002, FR-PROD-003

---

## Overview

Implemented `GET /api/test/charge-items` endpoint that:
1. Fetches items from Chargebee API
2. Caches items locally in MySQL database
3. Filters by categories: Delight, Signature, Premium
4. Filters prices by sizes: 200ml, 300ml, 500ml

---

## Files Created

### 1. ChargeItemDTO.java
**Location:** `bmjServer/src/main/java/com/bookmyjuice/dto/ChargeItemDTO.java`

**Purpose:** Data Transfer Object for Chargebee items with filtered categories and sizes

**Structure:**
```java
ChargeItemDTO {
  // Basic item fields
  itemId: String
  name: String
  description: String
  itemFamilyId: String
  status: String
  enabledInPortal: boolean
  enabledForCheckout: boolean
  
  // Metadata fields
  category: String           // Delight, Signature, Premium
  subcategory: String
  imagePath: String
  startColor: String
  endColor: String
  calories: Integer
  popularity: Integer
  servingSize: String
  shelfLife: String
  preparationTime: String
  temperature: String
  
  // Array fields
  meals: List<String>
  benefits: List<String>
  allergies: List<String>
  tags: List<String>
  
  // Nested objects
  nutritionalInfo: NutritionalInfo
  customization: Customization
  
  // Size-based prices
  prices: List<ItemPriceDTO>
}
```

**Nested DTOs:**
- `NutritionalInfo` - protein, carbs, fiber, sugar, vitamin C, iron
- `Customization` - sugarLevel, iceLevel, addOns
- `ItemPriceDTO` - priceId, name, size, price, currencyCode, pricingModel

---

### 2. ItemService.java (Updated)
**Location:** `bmjServer/src/main/java/com/bookmyjuice/services/ItemService.java`

**New Methods:**

| Method | Purpose | Lines |
|--------|---------|-------|
| `getChargeItemsWithCategoriesAndSizes()` | Main entry point - fetch, filter, cache | 1236-1273 |
| `extractCategoryFromMetadata()` | Extract category from Chargebee metadata | 1275-1289 |
| `isValidCategory()` | Validate category (Delight/Signature/Premium) | 1291-1298 |
| `convertToChargeItemDTO()` | Convert Chargebee Item to DTO | 1300-1351 |
| `extractSizePrices()` | Extract 200/300/500ml prices | 1353-1381 |
| `extractSizeFromItemPrice()` | Extract size from price name/description | 1383-1392 |
| `isValidSize()` | Validate size (200ml/300ml/500ml) | 1394-1400 |
| `jsonNodeToArrayList()` | Convert JsonNode array to List<String> | 1402-1411 |
| `convertToNutritionalInfo()` | Convert nutritional info JsonNode | 1413-1424 |
| `convertToCustomization()` | Convert customization JsonNode | 1426-1434 |
| `cacheItemsLocally()` | Sync items to local database | 1436-1440 |
| `getItemsFromLocalCache()` | Fallback to local MySQL cache | 1442-1453 |
| `convertEntityToChargeItemDTO()` | Convert ItemEntity to DTO | 1455-1490 |
| `convertEntityPriceToDTO()` | Convert ItemPriceEntity to DTO | 1492-1504 |
| `extractSizeFromEntityPrice()` | Extract size from entity | 1506-1515 |

**Flow:**
```
GET /api/test/charge-items
    ↓
ItemService.getChargeItemsWithCategoriesAndSizes()
    ↓
1. Fetch all items from Chargebee API
2. Filter by category (Delight/Signature/Premium)
3. Convert to ChargeItemDTO
4. Extract size-based prices (200/300/500ml)
5. Cache to local MySQL database
6. Return filtered list
    ↓
If Chargebee API fails → Fallback to local cache
```

---

### 3. TestController.java (Updated)
**Location:** `bmjServer/src/main/java/com/bookmyjuice/controllers/TestController.java`

**New Endpoint:**
```java
@GetMapping("/charge-items")
@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
@ResponseBody
public ResponseEntity<?> getChargeItemsWithCategoriesAndSizes()
```

**Authorization:** USER, MODERATOR, or ADMIN role required

**Response Format:**
```json
[
  {
    "itemId": "delight_orange_200ml",
    "name": "Delight Orange Juice",
    "description": "Fresh orange juice",
    "category": "delight",
    "imagePath": "assets/orange.png",
    "startColor": "#FFA500",
    "endColor": "#FF8C00",
    "calories": 112,
    "meals": ["Orange", "Water"],
    "benefits": ["Vitamin C", "Immune boost"],
    "tags": ["citrus", "fresh", "vitamin-c"],
    "nutritionalInfo": {
      "protein": "2g",
      "carbs": "26g",
      "sugar": "21g",
      "vitaminC": "120mg"
    },
    "customization": {
      "sugarLevel": ["no-sugar", "low-sugar", "regular"],
      "iceLevel": ["no-ice", "less-ice", "regular-ice"],
      "addOns": ["chia-seeds", "protein-powder"]
    },
    "prices": [
      {
        "priceId": "delight_orange_200ml_price",
        "name": "200ml",
        "size": "200ml",
        "price": 75.00,
        "currencyCode": "INR",
        "pricingModel": "flat_fee",
        "period": 1,
        "periodUnit": "week"
      },
      {
        "priceId": "delight_orange_300ml_price",
        "name": "300ml",
        "size": "300ml",
        "price": 99.00,
        "currencyCode": "INR"
      },
      {
        "priceId": "delight_orange_500ml_price",
        "name": "500ml",
        "size": "500ml",
        "price": 149.00,
        "currencyCode": "INR"
      }
    ]
  }
]
```

---

### 4. ItemServiceChargeItemsTest.java
**Location:** `bmjServer/src/test/java/com/bookmyjuice/services/ItemServiceChargeItemsTest.java`

**Test Cases:**

| Test ID | Test Name | Type | Status |
|---------|-----------|------|--------|
| TC-PROD-001 | Fetch Chargebee items - valid categories | Unit Test | ✅ Implemented |
| TC-PROD-002 | Filter items - only Delight/Signature/Premium | Unit Test | ✅ Implemented |
| TC-PROD-003 | Filter prices - only 200/300/500ml sizes | Unit Test | ✅ Implemented |

**Test Coverage:**
- TC-PROD-001: Verifies that items with valid categories are returned
- TC-PROD-002: Verifies that invalid categories are filtered out
- TC-PROD-003: Verifies that only 200/300/500ml prices are included

---

## API Usage

### Request
```http
GET /api/test/charge-items
Authorization: Bearer {jwt_token}
```

### Response Codes

| Code | Meaning |
|------|---------|
| 200 OK | Success - returns filtered items |
| 401 Unauthorized | Missing or invalid JWT token |
| 403 Forbidden | Insufficient role (requires USER/MODERATOR/ADMIN) |
| 400 Bad Request | Error processing request |

### Example cURL
```bash
curl -X GET "http://localhost:8080/api/test/charge-items" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## Category & Size Filtering

### Valid Categories
| Category | Description | Price Range |
|----------|-------------|-------------|
| **Delight** | Entry-level fresh juices | ₹69-99/bottle |
| **Signature** | Premium blended juices | ₹75-105/bottle |
| **Premium** | Cold-pressed premium juices | ₹83-109/bottle |

### Valid Sizes
| Size | Description | Typical Use |
|------|-------------|-------------|
| **200ml** | Small (Single serving) | Trial, kids, light drinkers |
| **300ml** | Medium (Regular serving) | Daily consumption, adults |
| **500ml** | Large (Family serving) | Sharing, families |

---

## Caching Strategy

### Primary Source: Chargebee API
- Fetch all items on first request
- Filter by category and size
- Cache to local MySQL database

### Fallback: Local MySQL Cache
- Used if Chargebee API is unavailable
- Items synced via webhooks (real-time)
- Items synced via ChargebeeSyncService (startup)

### Cache Tables
```sql
-- Items table
item_entity (id, name, description, metadata, ...)

-- Item prices table
item_price_entity (id, item_id, name, price, size, ...)
```

---

## Testing

### Run Unit Tests
```bash
cd bmjServer
mvn test -Dtest=ItemServiceChargeItemsTest
```

### Test Endpoint Manually
```bash
# Start backend
cd bmjServer
mvn spring-boot:run

# Test endpoint (requires authentication)
curl -X GET "http://localhost:8080/api/test/charge-items" \
  -H "Authorization: Bearer {token}"
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```
Navigate to: Test Controller → GET /api/test/charge-items

---

## Implementation Checklist

- [x] Create ChargeItemDTO with Lombok @Builder
- [x] Add ItemService.getChargeItemsWithCategoriesAndSizes()
- [x] Add TestController.getChargeItemsWithCategoriesAndSizes() endpoint
- [x] Implement category filtering (Delight/Signature/Premium)
- [x] Implement size filtering (200ml/300ml/500ml)
- [x] Add local cache fallback
- [x] Create unit tests (TC-PROD-001 to TC-PROD-003)
- [x] Add Swagger documentation
- [x] Add error handling

---

## Performance Considerations

| Aspect | Implementation |
|--------|----------------|
| **First Request** | Fetch from Chargebee (~500ms) |
| **Subsequent Requests** | Fetch from cache (~50ms) |
| **Fallback** | Local cache if Chargebee unavailable |
| **Rate Limiting** | Chargebee API limits apply |
| **Memory** | DTOs created per request (garbage collected) |

---

## Error Handling

| Error | Handling |
|-------|----------|
| Chargebee API unavailable | Fallback to local cache |
| Invalid metadata format | Skip item, log warning |
| Missing category | Skip item (not included in response) |
| Missing prices | Return item without prices |
| Database error | Return empty list, log error |

---

## Related Documents

| Document | Location |
|----------|----------|
| Business Requirements | `../requirements.yaml` (product_categories section) |
| Test Cases | `../docs/Test_Cases_Detailed.md` |
| API Documentation | `../docs/API.md` |
| Chargebee Integration | `../docs/architecture/ADR-003-chargebee-integration-strategy.md` |

---

**Implementation Status:** ✅ Complete  
**Test Status:** ✅ Unit tests implemented  
**Documentation:** ✅ Complete  
**Ready for:** QA Testing
