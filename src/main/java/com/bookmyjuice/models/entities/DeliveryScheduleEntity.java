package com.bookmyjuice.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The day-wise delivery schedule a customer picked at subscription checkout.
 *
 * <p>
 * Chargebee's hosted-page {@code checkout_new} accepts {@code subscription[id]}
 * but not {@code subscription[meta_data]}, so the schedule cannot travel with
 * the Chargebee checkout request. The backend therefore <em>reserves</em> the
 * subscription id up front, persists the schedule under that id, and passes the
 * id to Chargebee. The {@code subscription_created} webhook then arrives with
 * exactly that id, which joins the schedule back to the subscription without
 * guessing (no "latest row for this customer" heuristics).
 * </p>
 *
 * <p>
 * {@code daySchedule} stores the raw day map as JSON, e.g.
 * {@code {"monday":"mix-punch","wednesday":"carrot-juice"}}. A day that is
 * absent from the map is not a delivery day.
 * </p>
 */
@Entity
@Table(name = "delivery_schedule")
public class DeliveryScheduleEntity {

    /** Reserve-before-checkout subscription id handed to Chargebee. */
    @Id
    @Column(name = "subscription_id", length = 64)
    private String subscriptionId;

    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "day_schedule", columnDefinition = "TEXT")
    private String daySchedule;

    /** Epoch seconds when the checkout session was created. */
    @Column(name = "created_at")
    private int createdAt;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getDaySchedule() {
        return daySchedule;
    }

    public void setDaySchedule(String daySchedule) {
        this.daySchedule = daySchedule;
    }

    public int getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(int createdAt) {
        this.createdAt = createdAt;
    }
}
