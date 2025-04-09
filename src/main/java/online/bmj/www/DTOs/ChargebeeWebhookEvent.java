package online.bmj.www.DTOs;

public class ChargebeeWebhookEvent {
    private String eventType;
    private Object payload;

    // Getters and setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}