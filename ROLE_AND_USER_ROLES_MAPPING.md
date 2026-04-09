# Role and User Roles Mapping Documentation

**Date:** March 30, 2026  
**Version:** 1.0

---

## Overview

BookMyJuice uses a **Role-Based Access Control (RBAC)** system with three predefined roles. Users can be assigned one or more roles, and the mapping between users and roles is stored in a join table.

---

## Database Schema

### Tables Involved

```sql
-- 1. Users Table
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(20) UNIQUE NOT NULL,
  email VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(120) NOT NULL,
  phone VARCHAR(20),
  first_name VARCHAR(25),
  last_name VARCHAR(25),
  address VARCHAR(120),
  extended_addr VARCHAR(120),
  extended_addr2 VARCHAR(120),
  city VARCHAR(120),
  state VARCHAR(120),
  zip VARCHAR(6),
  country VARCHAR(2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Roles Table (3 predefined roles)
CREATE TABLE roles (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name ENUM('ROLE_USER', 'ROLE_MODERATOR', 'ROLE_ADMIN') UNIQUE NOT NULL
);

-- Predefined roles data
INSERT INTO roles (name) VALUES 
  ('ROLE_USER'),
  ('ROLE_MODERATOR'),
  ('ROLE_ADMIN');

-- 3. User_Roles Join Table (Many-to-Many mapping)
CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id INT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);
```

---

## Entity Relationship

```
┌─────────────────┐
│     users       │
│                 │
│  id (PK)        │
│  username       │
│  email          │
│  password       │
│  phone          │
│  ...            │
└────────┬────────┘
         │
         │ 1:N (via join table)
         │
         ▼
┌─────────────────┐
│   user_roles    │
│                 │
│  user_id (FK)   │───┐
│  role_id (FK)   │───┼─── Many-to-Many
└─────────────────┘   │
                      │
                      ▼
              ┌─────────────────┐
              │     roles       │
              │                 │
              │  id (PK)        │
              │  name (ENUM)    │
              └─────────────────┘
```

---

## Three Role Types

| Role ID | Role Name | Description | Permissions |
|---------|-----------|-------------|-------------|
| 1 | `ROLE_USER` | Regular user | Browse products, place orders, manage own subscriptions |
| 2 | `ROLE_MODERATOR` | Content moderator | All USER permissions + manage products, view reports |
| 3 | `ROLE_ADMIN` | Administrator | All permissions + user management, system configuration |

---

## User-to-Role Mapping Examples

### Example 1: Regular User (Single Role)
```
User: John (id=1)
Roles: [ROLE_USER]

user_roles table:
user_id | role_id
--------|--------
1       | 1
```

### Example 2: User with Multiple Roles
```
User: Jane (id=2)
Roles: [ROLE_USER, ROLE_MODERATOR]

user_roles table:
user_id | role_id
--------|--------
2       | 1
2       | 2
```

### Example 3: Administrator (All Roles)
```
User: Admin (id=3)
Roles: [ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN]

user_roles table:
user_id | role_id
--------|--------
3       | 1
3       | 2
3       | 3
```

---

## Java Entity Classes

### Role Entity
```java
package com.bookmyjuice.models;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ERole name;  // ERole is an enum: ROLE_USER, ROLE_MODERATOR, ROLE_ADMIN

  // Getters and setters
  public Integer getId() { return id; }
  public ERole getName() { return name; }
  public void setName(ERole name) { this.name = name; }
}
```

### User Entity
```java
package com.bookmyjuice.models;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @NotBlank
  @Size(max = 20)
  private String username;

  @NotBlank
  @Size(max = 50)
  @Email
  private String email;

  @NotBlank
  @Size(max = 120)
  private String password;

  @Size(max = 20)
  private String phone;

  // Many-to-Many relationship with roles
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<com.bookmyjuice.models.Role> roles = new HashSet<>();

  // Getters and setters
  public Set<com.bookmyjuice.models.Role> getRoles() { return roles; }
  public void setRoles(Set<com.bookmyjuice.models.Role> roles) { this.roles = roles; }
}
```

### ERole Enum
```java
package com.bookmyjuice.models;

public enum ERole {
  ROLE_USER,
  ROLE_MODERATOR,
  ROLE_ADMIN
}
```

---

## Role Assignment Logic

### Default Role Assignment (Signup)
When a new user signs up, they are automatically assigned the `ROLE_USER` role:

```java
// In AuthController.signup()
Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
roles.add(userRole);
user.setRoles(roles);
```

### Role Assignment Based on Request
Users can request specific roles during signup:

```java
Set<String> strRoles = signUpRequest.getRole();
Set<com.bookmyjuice.models.Role> roles = new HashSet<>();

if (strRoles == null) {
    // Default to ROLE_USER
    com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
    roles.add(userRole);
} else {
    strRoles.forEach(role -> {
        switch (role) {
            case "admin" -> {
                com.bookmyjuice.models.Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(adminRole);
            }
            case "mod" -> {
                com.bookmyjuice.models.Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(modRole);
            }
            default -> {
                com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            }
        }
    });
}
```

---

## Spring Security Integration

### UserDetailsImpl
The `UserDetailsImpl` class converts User entity to Spring Security's UserDetails:

```java
public static UserDetailsImpl build(User user) {
  List<GrantedAuthority> authorities = user.getRoles().stream()
      .map(role -> new SimpleGrantedAuthority(role.getName().name()))
      .collect(Collectors.toList());
  
  return new UserDetailsImpl(
      user.getId(),
      user.getUsername(),
      user.getEmail(),
      user.getPassword(),
      authorities);
}
```

### Role-Based Authorization
Endpoints are protected using `@PreAuthorize`:

```java
// Public access
@GetMapping("/all")
public String allAccess() { ... }

// USER, MODERATOR, or ADMIN
@GetMapping("/user")
@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
public User userAccess() { ... }

// MODERATOR only
@GetMapping("/mod")
@PreAuthorize("hasRole('MODERATOR')")
public String moderatorAccess() { ... }

// ADMIN only
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public String adminAccess() { ... }
```

---

## Common Issues and Solutions

### Issue 1: Wrong Role Import
**Problem:** Importing `javax.management.relation.Role` instead of `com.bookmyjuice.models.Role`

**Solution:**
```java
// WRONG
import javax.management.relation.Role;

// CORRECT
import com.bookmyjuice.models.Role;
```

### Issue 2: Type Mismatch in Set
**Problem:** `Set<Role>` vs `Set<com.bookmyjuice.models.Role>`

**Solution:**
```java
// WRONG
Set<Role> roles = new HashSet<>();

// CORRECT
Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
```

### Issue 3: Missing getPhone() Method
**Problem:** User entity missing phone getter/setter

**Solution:**
```java
@Size(max = 20)
private String phone;

public String getPhone() {
    return phone;
}

public void setPhone(String phone) {
    this.phone = phone;
}
```

---

## Database Queries

### Get User with Roles
```java
@Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username")
Optional<User> findByUsernameWithRoles(@Param("username") String username);
```

### Get Users by Role
```java
@Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
List<User> findByRole(@Param("roleName") ERole roleName);
```

---

## Testing

### Test User Creation with Roles
```java
@Test
void testUserWithMultipleRoles() {
    User user = new User("testuser", "test@example.com", "password123");
    
    Set<Role> roles = new HashSet<>();
    roles.add(new Role(ERole.ROLE_USER));
    roles.add(new Role(ERole.ROLE_MODERATOR));
    
    user.setRoles(roles);
    
    assertEquals(2, user.getRoles().size());
    assertTrue(user.getRoles().stream()
        .anyMatch(r -> r.getName() == ERole.ROLE_USER));
    assertTrue(user.getRoles().stream()
        .anyMatch(r -> r.getName() == ERole.ROLE_MODERATOR));
}
```

---

## References

| Document | Location |
|----------|----------|
| Spring Security Documentation | https://spring.io/projects/spring-security |
| JPA Many-to-Many Mapping | https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/ |
| Spring Data JPA Repositories | https://docs.spring.io/spring-data/jpa/docs/current/reference/html/ |

---

**Last Updated:** March 30, 2026  
**Maintained By:** BookMyJuice Engineering Team
