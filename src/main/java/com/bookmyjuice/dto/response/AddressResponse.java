package com.bookmyjuice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bookmyjuice.models.entities.UserAddressEntity;

public class AddressResponse {

    private Long id;
    private String label;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isDefault;
    private String deliveryInstructions;
    private LocalDateTime createdAt;

    public AddressResponse() {}

    public static AddressResponse fromEntity(UserAddressEntity entity) {
        AddressResponse response = new AddressResponse();
        response.setId(entity.getId());
        response.setLabel(entity.getLabel());
        response.setFullName(entity.getFullName());
        response.setPhone(entity.getPhone());
        response.setAddressLine1(entity.getAddressLine1());
        response.setAddressLine2(entity.getAddressLine2());
        response.setLandmark(entity.getLandmark());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setPincode(entity.getPincode());
        response.setLatitude(entity.getLatitude());
        response.setLongitude(entity.getLongitude());
        response.setDefault(entity.isDefault());
        response.setDeliveryInstructions(entity.getDeliveryInstructions());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
