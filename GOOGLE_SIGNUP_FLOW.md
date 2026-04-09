# BookMyJuice Google Signup Flow Documentation

**Version:** 1.0  
**Date:** April 1, 2026  
**Status:** LIVE

---

## 🎯 Overview

Google Signup provides a streamlined registration experience by leveraging Google account information to pre-fill the signup form. This reduces friction and ensures email verification through Google's authentication.

---

## 📋 User Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    Google Signup Flow                        │
└─────────────────────────────────────────────────────────────┘

1. User taps "Sign up with Google" button
   ↓
2. Google Account Selection Screen Opens
   - Shows all Google accounts on device
   - User selects one account
   ↓
3. Google Returns User Data
   - Email (verified by Google)
   - First Name
   - Last Name
   - Photo URL
   - Google ID (unique identifier)
   ↓
4. Signup Form Pre-filled (READ-ONLY)
   ┌─────────────────────────────────────┐
   │ Email:      user@gmail.com [🔒]     │ ← Read-only
   │ First Name: John        [🔒]         │ ← Read-only
   │ Last Name:  Doe         [🔒]         │ ← Read-only
   │ Photo:      [Profile Image] [🔒]    │ ← Read-only
   └─────────────────────────────────────┘
   ↓
5. User Enters Required Details (EDITABLE)
   ┌─────────────────────────────────────┐
   │ Phone:      [________________]      │ ← Required, OTP verified
   │ Address:    [________________]      │ ← Required
   │ City:       [________________]      │ ← Required
   │ State:      [________________]      │ ← Required
   │ ZIP:        [________________]      │ ← Required
   │ Password:   [________________]      │ ← Required, validated
   │ Confirm:    [________________]      │ ← Required, must match
   └─────────────────────────────────────┘
   ↓
6. User Taps "Create Account"
   ↓
7. Validation
   - Phone: 10-digit Indian number
   - Address: Complete address
   - Password: Meets strength requirements
   ↓
8. Account Created
   - User saved to database
   - Chargebee customer created
   - Email verified (via Google)
   - Photo stored (google_photo_url)
   - Google ID stored (google_id)
   ↓
9. User Logged In
   - JWT token generated
   - Redirected to dashboard
```

---

## 🔐 Security Considerations

### Read-Only Fields (Google-Fetched)

| Field | Source | Why Read-Only |
|-------|--------|---------------|
| Email | Google | Verified by Google, ensures authenticity |
| First Name | Google | From Google profile, consistent identity |
| Last Name | Google | From Google profile, consistent identity |
| Photo URL | Google | From Google profile, user can change in Google settings |
| Google ID | Google | Unique identifier, immutable |

### Editable Fields (User-Entered)

| Field | Validation | Why Editable |
|-------|------------|--------------|
| Phone | 10-digit Indian number, OTP verified | Required for delivery coordination |
| Address | Complete address required | Required for delivery |
| City | Required | Required for delivery |
| State | Required | Required for delivery |
| ZIP | 6-digit Indian PIN | Required for delivery |
| Password | 8+ chars, uppercase, lowercase, number, special char | Security requirement |
| Confirm Password | Must match password | Prevents typos |

---

## 📱 UI Implementation

### AppTextField Component

```dart
// Google-fetched fields (READ-ONLY)
AppTextField(
  label: 'Email',
  controller: _emailController,
  readOnly: true,  // ← Cannot be edited
  prefixIcon: Icons.email_outlined,
)

AppTextField(
  label: 'First Name',
  controller: _firstNameController,
  readOnly: true,  // ← Cannot be edited
  prefixIcon: Icons.person_outline,
)

AppTextField(
  label: 'Last Name',
  controller: _lastNameController,
  readOnly: true,  // ← Cannot be edited
  prefixIcon: Icons.person_outline,
)

// User-entered fields (EDITABLE)
AppTextField(
  label: 'Phone Number',
  controller: _phoneController,
  readOnly: false,  // ← Can be edited
  keyboardType: TextInputType.phone,
  validator: (value) {
    if (value == null || value.isEmpty) {
      return 'Phone number is required';
    }
    if (!isValidPhone(value)) {
      return 'Enter a valid 10-digit number';
    }
    return null;
  },
)
```

### Google Sign-In Integration

```dart
// Step 1: Trigger Google Sign-In
final googleUser = await MyGoogleSignIn.signIn();

if (googleUser != null) {
  // Step 2: Extract data from Google
  final email = googleUser.email;
  final firstName = googleUser.givenName ?? '';
  final lastName = googleUser.familyName ?? '';
  final photoUrl = googleUser.photoUrl;
  final googleId = googleUser.id;
  
  // Step 3: Pre-fill form (read-only)
  _emailController.text = email;
  _firstNameController.text = firstName;
  _lastNameController.text = lastName;
  _googlePhotoUrl = photoUrl;
  _googleId = googleId;
  
  // Step 4: Set read-only state
  setState(() {
    _isGoogleSignup = true;
  });
}
```

---

## 🗄️ Database Schema

### Users Table Updates

```sql
-- New columns for Google signup
ALTER TABLE users 
  ADD COLUMN google_photo_url VARCHAR(500),
  ADD COLUMN google_id VARCHAR(100) UNIQUE;

-- Existing column (already added)
ALTER TABLE users 
  ADD COLUMN chargebee_customer_id VARCHAR(50) UNIQUE;
```

### User Entity (Java)

```java
/**
 * Google Photo URL - populated when user signs up via Google
 * Read-only field (cannot be edited by user)
 */
@Column(name = "google_photo_url", length = 500)
private String googlePhotoUrl;

/**
 * Google ID - unique identifier from Google account
 * Used for Google sign-in authentication
 */
@Column(name = "google_id", unique = true)
private String googleId;

// Getters and Setters
public String getGooglePhotoUrl() {
    return googlePhotoUrl;
}

public void setGooglePhotoUrl(String googlePhotoUrl) {
    this.googlePhotoUrl = googlePhotoUrl;
}

public String getGoogleId() {
    return googleId;
}

public void setGoogleId(String googleId) {
    this.googleId = googleId;
}
```

---

## 🧪 Testing

### Test Cases

| Test ID | Test Name | Status |
|---------|-----------|--------|
| TC-GS-001 | Google account selection opens | ✅ Implemented |
| TC-GS-002 | Email pre-filled from Google (read-only) | ✅ Implemented |
| TC-GS-003 | Name pre-filled from Google (read-only) | ✅ Implemented |
| TC-GS-004 | Photo fetched from Google (read-only) | ✅ Implemented |
| TC-GS-005 | Phone field editable and validated | ✅ Implemented |
| TC-GS-006 | Address fields editable and required | ✅ Implemented |
| TC-GS-007 | Password validated (strength requirements) | ✅ Implemented |
| TC-GS-008 | Account created with Google data | ✅ Implemented |
| TC-GS-009 | Email verified via Google (no email verification needed) | ✅ Implemented |
| TC-GS-010 | Google ID stored in database | ✅ Implemented |

### Manual Testing Checklist

- [ ] Google button opens account selection
- [ ] Email field is pre-filled and read-only
- [ ] First Name field is pre-filled and read-only
- [ ] Last Name field is pre-filled and read-only
- [ ] Photo is fetched from Google
- [ ] Phone field is editable and requires validation
- [ ] Address fields are editable and required
- [ ] Password field validates strength requirements
- [ ] Submit creates account successfully
- [ ] User is logged in after signup
- [ ] Email is marked as verified (no email verification needed)
- [ ] Google ID is stored in database
- [ ] Photo URL is stored in database

---

## 🔧 Configuration

### Google Cloud Console Setup

1. **Create Project**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create new project: "BookMyJuice"

2. **Enable Google Sign-In API**
   - APIs & Services → Library
   - Search "Google Sign-In API"
   - Enable

3. **Create OAuth 2.0 Credentials**
   - APIs & Services → Credentials
   - Create Credentials → OAuth 2.0 Client ID
   - Application type: Android
   - Package name: `com.bookmyjuice.app`
   - SHA-1: (from `keytool -list -v -keystore ~/.android/debug.keystore`)

4. **Add to Flutter App**
   ```yaml
   # pubspec.yaml
   dependencies:
     google_sign_in: ^6.0.0
   ```

5. **Configure Android**
   ```xml
   <!-- AndroidManifest.xml -->
   <meta-data
     android:name="com.google.android.gms.version"
     android:value="@integer/google_play_services_version" />
   ```

---

## 📊 Comparison: Google Signup vs Email Signup

| Feature | Google Signup | Email Signup |
|---------|---------------|--------------|
| **Email Verification** | ✅ Automatic (via Google) | ❌ Manual (6-digit code) |
| **Form Pre-fill** | ✅ Email, Name, Photo | ❌ All fields empty |
| **Read-Only Fields** | ✅ Email, Name, Photo | ❌ None |
| **Editable Fields** | Phone, Address, Password | All fields |
| **Steps to Complete** | 5 steps | 7 steps |
| **Time to Complete** | ~2 minutes | ~4 minutes |
| **Security** | ✅ Google 2FA available | ❌ Email verification only |
| **User Experience** | ⭐⭐⭐⭐⭐ (Excellent) | ⭐⭐⭐⭐ (Good) |

---

## 🚨 Edge Cases

### Case 1: User Cancels Google Selection
```
User taps "Sign up with Google"
  ↓
Google account selection opens
  ↓
User cancels or presses back
  ↓
Return to signup method selection
  ↓
Show message: "Google signup cancelled"
```

### Case 2: No Google Accounts on Device
```
User taps "Sign up with Google"
  ↓
Google shows "No accounts found"
  ↓
User adds Google account
  ↓
Continue with signup flow
```

### Case 3: Google Signup with Existing Email
```
User selects Google account
  ↓
Email already exists in database
  ↓
Show error: "Email already registered. Please login."
  ↓
Offer to login instead
```

### Case 4: Network Error During Google Sign-In
```
User taps "Sign up with Google"
  ↓
Network error
  ↓
Show error: "Failed to connect to Google. Please check internet."
  ↓
Offer to try again or use email signup
```

---

## 📈 Analytics Tracking

### Events to Track

```dart
// Google signup initiated
Analytics.logEvent(
  'google_signup_initiated',
  parameters: {'timestamp': DateTime.now().toIso8601String()},
);

// Google account selected
Analytics.logEvent(
  'google_account_selected',
  parameters: {
    'google_id': googleId,
    'email_domain': email.split('@').last,
  },
);

// Google signup completed
Analytics.logEvent(
  'google_signup_completed',
  parameters: {
    'google_id': googleId,
    'signup_duration_seconds': duration.inSeconds,
  },
);

// Google signup abandoned
Analytics.logEvent(
  'google_signup_abandoned',
  parameters: {
    'abandoned_at_step': step,
    'reason': reason,
  },
);
```

---

## 📚 References

| Document | Location |
|----------|----------|
| QWEN_PROJECT_GUARDRAILS.md | `docs/QWEN_PROJECT_GUARDRAILS.md` |
| SIGNUP_SCREEN_IMPLEMENTATION.md | `lush/SIGNUP_SCREEN_IMPLEMENTATION.md` |
| DATABASE_SCHEMA_DOCUMENTATION.md | `bmjServer/DATABASE_SCHEMA_DOCUMENTATION.md` |
| requirements.yaml | `requirements.yaml` (MVP-AUTH-002) |

---

**Last Updated:** April 1, 2026  
**Maintained By:** BookMyJuice Engineering Team  
**Next Review:** May 1, 2026
