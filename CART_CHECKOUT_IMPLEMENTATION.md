# Cart Checkout Implementation - FR-CHK-001

**Date:** March 29, 2026  
**Status:** Implementation Complete + Unit Tests  
**Endpoint:** POST /api/test/cartCheckout

---

## Overview

Implemented cart checkout endpoint that:
1. Accepts cart items with `itemPriceId` and `quantity`
2. Uses authenticated user's `customerId`
3. Generates Chargebee hosted page URL (test mode)
4. Returns checkout URL for WebView redirect

---

## Existing Implementation

### Controller: CheckoutController.java
**Location:** `bmjServer/src/main/java/com/bookmyjuice/controllers/CheckoutController.java`

**Endpoint:**
```java
@PostMapping("/cartCheckout")
@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
public ResponseEntity<?> cartCheckout(@RequestBody List<Map<String, Object>> cartItems)
```

**Request Format:**
```json
[
  {
    "itemPriceId": "delight-watermelon-200ml-INR",
    "quantity": 2
  },
  {
    "itemPriceId": "signature-abc-300ml-INR",
    "quantity": 1
  }
]
```

**Response Format:**
```json
{
  "id": "hp_test_123",
  "url": "https://bookmyjuice-test.chargebee.com/hosted_pages/checkout?hp_id=hp_test_123",
  "state": "created",
  "type": "checkout_one_time_for_items"
}
```

---

## Implementation Details

### Flow
```
Flutter App (CartScreen)
        ↓
  User taps "Proceed to Checkout"
        ↓
  POST /api/test/cartCheckout
  Headers: Authorization: Bearer {jwt}
  Body: [{itemPriceId, quantity}, ...]
        ↓
  CheckoutController.cartCheckout()
        ↓
  Extract customerId from JWT token
        ↓
  Build Chargebee request:
    HostedPage.checkoutOneTimeForItems()
      .customerId("100")
      .itemPriceItemPriceId(0, "delight-watermelon-200ml-INR")
      .itemPriceQuantity(0, 2)
      .itemPriceItemPriceId(1, "signature-abc-300ml-INR")
      .itemPriceQuantity(1, 1)
        ↓
  Chargebee Test Site
        ↓
  Returns HostedPage URL
        ↓
  Flutter receives URL
        ↓
  Opens WebView with checkout URL
```

### Code Implementation
```java
@PostMapping("/cartCheckout")
@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
public ResponseEntity<?> cartCheckout(@RequestBody List<Map<String, Object>> cartItems) {
    try {
        // Build Chargebee checkout request
        HostedPage.CheckoutOneTimeForItemsRequest req = HostedPage.checkoutOneTimeForItems()
            .customerId(getUserIdFromSecurityContext());
        
        // Add each cart item to the request
        for (int i = 0; i < cartItems.size(); i++) {
            Map<String, Object> item = cartItems.get(i);
            String itemPriceId = String.valueOf(item.get("itemPriceId"));
            int quantity = item.get("quantity") != null 
                ? Integer.parseInt(item.get("quantity").toString()) 
                : 1;
            
            req = req.itemPriceItemPriceId(i, itemPriceId)
                     .itemPriceQuantity(i, quantity);
        }
        
        // Execute request
        Result result = req.request();
        HostedPage hostedPage = result.hostedPage();
        
        return ResponseEntity.ok(hostedPage.toJson());
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body("Error: " + e.getMessage());
    }
}
```

---

## Unit Tests (TC-CHK-001 to TC-CHK-005)

### Test File
**Location:** `bmjServer/src/test/java/com/bookmyjuice/controllers/CartCheckoutControllerTest.java`

| Test ID | Test Name | Status |
|---------|-----------|--------|
| TC-CHK-001 | Cart checkout with valid items | ✅ Implemented |
| TC-CHK-002 | Cart checkout with empty cart | ✅ Implemented |
| TC-CHK-003 | Cart checkout with invalid item data | ✅ Implemented |
| TC-CHK-004 | Cart checkout with single item | ✅ Implemented |
| TC-CHK-005 | Cart checkout with missing quantity (defaults to 1) | ✅ Implemented |

### Test Execution
```bash
cd bmjServer
mvn test -Dtest=CartCheckoutControllerTest
```

---

## Integration with Flutter

### Frontend Call (CartScreen.dart)
```dart
// In CartScreen._buildCheckoutSection()
final userRepository = getIt.get<UserRepository>();
final cartItems = items
    .map((cartItem) => {
          "itemPriceId": cartItem.selectedPrice?.id ?? '',
          "quantity": cartItem.quantity,
        })
    .toList();

try {
  final checkoutUrl = await userRepository.getCartCheckoutUrl(cartItems);
  if (checkoutUrl.isNotEmpty) {
    Navigator.of(context).pushNamed('/checkout', arguments: checkoutUrl);
  }
} catch (e) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text('Checkout error: $e')),
  );
}
```

### UserRepository Method
```dart
Future<String> getCartCheckoutUrl(List<Map<String, dynamic>> cartItems) async {
  var response = await http.post(
    Uri.parse('$server/api/test/cartCheckout'),
    headers: {
      "Authorization": "Bearer $token",
      "Accept": "application/json",
      "Content-Type": "application/json",
    },
    body: jsonEncode(cartItems),
  );

  if (response.statusCode == 200) {
    var body = const Utf8Decoder().convert(response.bodyBytes);
    final dynamic jsonResponse = json.decode(body);
    return jsonResponse["url"] ?? jsonResponse["hosted_page"]?["url"] ?? "";
  }
  return "Error: ${response.statusCode}";
}
```

---

## Chargebee Configuration

### Test Mode
- **Site:** `bookmyjuice-test`
- **API Key:** `test_fMwLpyDFENxTWox6zsbpaYNAoL3yiY9v`
- **Hosted Page URL Format:** `https://bookmyjuice-test.chargebee.com/hosted_pages/checkout?hp_id={hp_id}`

### Production Mode (Future)
- **Site:** `bookmyjuice`
- **API Key:** `{production_api_key}`
- **Hosted Page URL Format:** `https://bookmyjuice.chargebee.com/hosted_pages/checkout?hp_id={hp_id}`

---

## Error Handling

| Error | Response Code | Response Body |
|-------|---------------|---------------|
| Success | 200 OK | `{id, url, state, type}` |
| Invalid itemPriceId | 400 Bad Request | `Error: Invalid item price ID` |
| Customer not found | 400 Bad Request | `Error: Customer not found` |
| Authentication missing | 401 Unauthorized | (Spring Security) |
| Insufficient role | 403 Forbidden | (Spring Security) |

---

## Security

### Authentication
- JWT token required in `Authorization` header
- Token extracted and validated by Spring Security

### Authorization
- `@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")`
- Only authenticated users with USER, MODERATOR, or ADMIN role can access

### Customer ID Mapping
```java
public String getUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId().toString(); // Returns Chargebee customer ID
    }
    return null;
}
```

---

## Testing Checklist

### Manual Testing
- [ ] Add items to cart (200ml, 300ml, 500ml sizes)
- [ ] Navigate to CartScreen
- [ ] Verify subtotal, tax, delivery fee, total
- [ ] Tap "Proceed to Checkout"
- [ ] Verify Chargebee hosted page opens in WebView
- [ ] Complete test payment (use Chargebee test cards)
- [ ] Verify order confirmation
- [ ] Verify order appears in Order History

### API Testing (Postman/cURL)
```bash
# Login to get token
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"SecurePass123!"}'

# Use token to checkout
curl -X POST http://localhost:8080/api/test/cartCheckout \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '[{"itemPriceId":"delight-watermelon-200ml-INR","quantity":2}]'
```

---

## Related Documents

| Document | Location |
|----------|----------|
| Cart Implementation | `../lush/CART_IMPLEMENTATION_SUMMARY.md` |
| Product Catalog | `../lush/PRODUCT_CATALOG_IMPLEMENTATION.md` |
| Requirements | `../requirements.yaml` (MVP-CHECKOUT section) |
| Test Cases | `../docs/Test_Cases_Detailed.md` |

---

**Implementation Status:** ✅ Complete  
**Test Status:** ✅ Unit tests implemented  
**Ready for:** Integration Testing
