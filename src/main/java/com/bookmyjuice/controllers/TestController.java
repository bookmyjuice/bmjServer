package com.bookmyjuice.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.ListResult;
import com.chargebee.models.Item;
import com.chargebee.models.Item.ItemListRequest;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {

  @Autowired
  UserRepository userRepository;

  @GetMapping("/all")
  public String allAccess() {
    return "Public Content.";
  }

  @GetMapping("/user")
  @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
  public User userAccess() {
    return userRepository.findById(getUserIdFromSecurityContext())
        .orElseThrow(() -> new RuntimeException("User Not Found with id: " + getUserIdFromSecurityContext()));
    // return "User Content.";
  }

  @GetMapping("/mod")
  @PreAuthorize("hasRole('MODERATOR')")
  public String moderatorAccess() {
    return "Moderator Board.";
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminAccess() {
    return "Admin Board.";
  }

  public Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        return null; // Or throw an exception
    }

 @GetMapping("/chargebeeItems")
@PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
@ResponseBody
public ResponseEntity<?> getAllChargebeeItems() {
    try {
        // List<Item> items = new ArrayList<>();
        ItemListRequest request = Item.list();
        ListResult result = request.request();

        List<Map<String, Object>> response = new ArrayList<>();
        for (ListResult.Entry entry : result) {
            Item item = entry.item();
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.id());
            itemMap.put("externalName", item.externalName());
            
            itemMap.put("enabledInPortal", item.enabledInPortal());
            itemMap.put("enabledForCheckout", item.enabledForCheckout());
            itemMap.put("name", item.name());
            itemMap.put("description", item.description());
            itemMap.put("type", item.type());
            itemMap.put("status", item.status());
            itemMap.put("itemFamilyId", item.itemFamilyId());
            itemMap.put("isGiftable", item.isGiftable());
            itemMap.put("isShippable", item.isShippable());
            itemMap.put("deleted", item.deleted());
            itemMap.put("unit", item.unit());
            itemMap.put("applicableItems", item.applicableItems());
            itemMap.put("jsonObject", item.jsonObj.toString());
            response.add(itemMap);
        }
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
    }
}
}
