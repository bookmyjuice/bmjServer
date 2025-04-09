package online.bmj.www.repository.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import online.bmj.www.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
   
  Optional<User> findByEmail(String email);
  Optional<User> findByPhone(String phone);


  Boolean existsByPhone(String phone);

  Boolean existsByEmail(String email);
  User findTopByOrderByIdDesc();  
}
