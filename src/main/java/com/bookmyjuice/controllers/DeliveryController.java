package com.bookmyjuice.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.bookmyjuice.dto.request.AddressRequest;
import com.bookmyjuice.dto.response.AddressResponse;
import com.bookmyjuice.dto.response.DeliverySlotResponse;
import com.bookmyjuice.models.entities.DeliverySlotEntity;
import com.bookmyjuice.models.entities.ServiceAreaEntity;
import com.bookmyjuice.models.entities.UserAddressEntity;
import com.bookmyjuice.repository.DeliverySlotRepository;
import com.bookmyjuice.repository.ServiceAreaRepository;
import com.bookmyjuice.repository.UserAddressRepository;
import com.bookmyjuice.services.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);

    @Autowired
    private ServiceAreaRepository serviceAreaRepository;

    @Autowired
    private DeliverySlotRepository deliverySlotRepository;

    @Autowired
    private UserAddressRepository userAddressRepository;

    // ==================== Service Area Endpoints ====================

    @GetMapping("/service-areas")
    public ResponseEntity checkServiceability(@RequestParam String pincode) {
        try {
            var serviceArea = serviceAreaRepository.findByPincode(pincode);
            if (serviceArea.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "Pincode not serviceable", "serviced", false));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "serviced", serviceArea.get().isServiced(),
                    "data", serviceArea.get()
            ));
        } catch (Exception e) {
            logger.error("Error checking serviceability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/service-areas/city/{city}")
    public ResponseEntity getServiceAreasByCity(@PathVariable String city) {
        try {
            var serviceAreas = serviceAreaRepository.findByCity(city);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", serviceAreas.size(),
                    "data", serviceAreas
            ));
        } catch (Exception e) {
            logger.error("Error fetching service areas by city: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Delivery Slot Endpoints ====================

    @GetMapping("/slots")
    public ResponseEntity getSlots(@RequestParam Long serviceAreaId, @RequestParam String date) {
        try {
            var slotDate = LocalDate.parse(date);
            var slots = deliverySlotRepository
                    .findByServiceAreaIdAndSlotDateAndIsActiveTrue(serviceAreaId, slotDate);
            var responses = slots.stream()
                    .map(DeliverySlotResponse::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", responses.size(),
                    "data", responses
            ));
        } catch (Exception e) {
            logger.error("Error fetching slots: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/slots/range")
    public ResponseEntity getSlotsForRange(
            @RequestParam Long serviceAreaId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            var start = LocalDate.parse(startDate);
            var end = LocalDate.parse(endDate);
            var slots = deliverySlotRepository
                    .findByServiceAreaIdAndSlotDateBetweenAndIsActiveTrue(serviceAreaId, start, end);
            var responses = slots.stream()
                    .map(DeliverySlotResponse::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", responses.size(),
                    "data", responses
            ));
        } catch (Exception e) {
            logger.error("Error fetching slots for range: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Address Endpoints ====================

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity getAddresses() {
        try {
            Long userId = getUserIdFromSecurityContext();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }
            var addresses = userAddressRepository.findByUserId(userId);
            var responses = addresses.stream()
                    .map(AddressResponse::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", responses.size(),
                    "data", responses
            ));
        } catch (Exception e) {
            logger.error("Error fetching addresses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity addAddress(@RequestBody AddressRequest request) {
        try {
            Long userId = getUserIdFromSecurityContext();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            var entity = new UserAddressEntity();
            entity.setUserId(userId);
            entity.setLabel(request.getLabel());
            entity.setFullName(request.getFullName());
            entity.setPhone(request.getPhone());
            entity.setAddressLine1(request.getAddressLine1());
            entity.setAddressLine2(request.getAddressLine2());
            entity.setLandmark(request.getLandmark());
            entity.setCity(request.getCity());
            entity.setState(request.getState());
            entity.setPincode(request.getPincode());
            entity.setLatitude(request.getLatitude());
            entity.setLongitude(request.getLongitude());
            entity.setDeliveryInstructions(request.getDeliveryInstructions());

            if (request.isDefault()) {
                userAddressRepository.setDefaultFalseForUser(userId);
                entity.setDefault(true);
            } else {
                var existing = userAddressRepository.findByUserId(userId);
                entity.setDefault(existing.isEmpty());
            }

            var saved = userAddressRepository.save(entity);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "status", "success",
                            "message", "Address added successfully",
                            "data", AddressResponse.fromEntity(saved)
                    ));
        } catch (Exception e) {
            logger.error("Error adding address: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/addresses/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity updateAddress(@PathVariable Long id, @RequestBody AddressRequest request) {
        try {
            Long userId = getUserIdFromSecurityContext();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            var existingOpt = userAddressRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Address not found"));
            }

            var entity = existingOpt.get();
            if (!entity.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not own this address"));
            }

            entity.setLabel(request.getLabel());
            entity.setFullName(request.getFullName());
            entity.setPhone(request.getPhone());
            entity.setAddressLine1(request.getAddressLine1());
            entity.setAddressLine2(request.getAddressLine2());
            entity.setLandmark(request.getLandmark());
            entity.setCity(request.getCity());
            entity.setState(request.getState());
            entity.setPincode(request.getPincode());
            entity.setLatitude(request.getLatitude());
            entity.setLongitude(request.getLongitude());
            entity.setDeliveryInstructions(request.getDeliveryInstructions());

            if (request.isDefault()) {
                userAddressRepository.setDefaultFalseForUser(userId);
                entity.setDefault(true);
            }

            var saved = userAddressRepository.save(entity);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Address updated successfully",
                    "data", AddressResponse.fromEntity(saved)
            ));
        } catch (Exception e) {
            logger.error("Error updating address: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/addresses/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity deleteAddress(@PathVariable Long id) {
        try {
            Long userId = getUserIdFromSecurityContext();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            var existingOpt = userAddressRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Address not found"));
            }

            var entity = existingOpt.get();
            if (!entity.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not own this address"));
            }

            userAddressRepository.delete(entity);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Address deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deleting address: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/addresses/{id}/default")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity setDefaultAddress(@PathVariable Long id) {
        try {
            Long userId = getUserIdFromSecurityContext();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            var existingOpt = userAddressRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Address not found"));
            }

            var entity = existingOpt.get();
            if (!entity.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You do not own this address"));
            }

            userAddressRepository.setDefaultFalseForUser(userId);
            entity.setDefault(true);
            userAddressRepository.save(entity);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Default address updated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error setting default address: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        return null;
    }
}
