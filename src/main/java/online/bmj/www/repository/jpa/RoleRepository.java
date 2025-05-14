<<<<<<<< HEAD:src/main/java/com/bookmyjuice/repository/RoleRepository.java
package com.bookmyjuice.repository;
========
package online.bmj.www.repository.jpa;
>>>>>>>> d97884e9565256ce746f426f71499cf53ac87269:src/main/java/online/bmj/www/repository/jpa/RoleRepository.java

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

<<<<<<<< HEAD:src/main/java/com/bookmyjuice/repository/RoleRepository.java
import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
========
import online.bmj.www.models.ERole;
import online.bmj.www.models.Role;
>>>>>>>> d97884e9565256ce746f426f71499cf53ac87269:src/main/java/online/bmj/www/repository/jpa/RoleRepository.java

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
