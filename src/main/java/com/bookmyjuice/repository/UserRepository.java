<<<<<<<< HEAD:src/main/java/com/bookmyjuice/repository/UserRepository.java
package com.bookmyjuice.repository;
========
package online.bmj.www.repository.jpa;
>>>>>>>> d97884e9565256ce746f426f71499cf53ac87269:src/main/java/online/bmj/www/repository/jpa/UserRepository.java

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

<<<<<<<< HEAD:src/main/java/com/bookmyjuice/repository/UserRepository.java
import com.bookmyjuice.models.User;
========
import online.bmj.www.models.User;
>>>>>>>> d97884e9565256ce746f426f71499cf53ac87269:src/main/java/online/bmj/www/repository/jpa/UserRepository.java

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
   
  Optional<User> findByEmail(String email);
  Optional<User> findByPhone(String phone);


  Boolean existsByPhone(String phone);

  Boolean existsByEmail(String email);
  User findTopByOrderByIdDesc();  
}
