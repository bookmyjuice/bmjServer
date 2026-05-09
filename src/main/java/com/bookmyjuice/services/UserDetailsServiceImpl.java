package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  
  @Autowired
  UserRepository userRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // Try to find user by phone/username first
    var user = userRepository.findByUsername(username);

    // If not found, try to find by email
    if (!user.isPresent()) {
      user = userRepository.findByEmail(username);
    }

    if (!user.isPresent()) {
      throw new UsernameNotFoundException("User Not Found with username or email: " + username);
    }

    // Check if user has been soft deleted
    if (user.get().isDeleted()) {
      throw new UsernameNotFoundException("User account has been deleted");
    }

    return UserDetailsImpl.build(user.get());
  }

}
