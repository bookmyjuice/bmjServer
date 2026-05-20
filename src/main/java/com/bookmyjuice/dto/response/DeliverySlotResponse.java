package com.bookmyjuice.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.bookmyjuice.models.entities.DeliverySlotEntity;

public class DeliverySlotResponse {

    private Long id;
    private Long serviceAreaId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private int maxOrders;
    private int currentOrders;
    private boolean isActive;
    private boolean available;

    public DeliverySlotResponse() {}

    public static DeliverySlotResponse fromEntity(DeliverySlotEntity entity) {
        DeliverySlotResponse response = new DeliverySlotResponse();
        response.setId(entity.getId());
        response.setServiceAreaId(entity.getServiceAreaId());
        response.setSlotDate(entity.getSlotDate());
        response.setStartTime(entity.getStartTime());
        response.setEndTime(entity.getEndTime());
        response.setMaxOrders(entity.getMaxOrders());
        response.setCurrentOrders(entity.getCurrentOrders());
        response.setActive(entity.isActive());
        response.setAvailable(entity.getCurrentOrders() < entity.getMaxOrders() && entity.isActive());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getServiceAreaId() { return serviceAreaId; }
    public void setServiceAreaId(Long serviceAreaId) { this.serviceAreaId = serviceAreaId; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getMaxOrders() { return maxOrders; }
    public void setMaxOrders(int maxOrders) { this.maxOrders = maxOrders; }

    public int getCurrentOrders() { return currentOrders; }
    public void setCurrentOrders(int currentOrders) { this.currentOrders = currentOrders; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
