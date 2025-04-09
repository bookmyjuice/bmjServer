package online.bmj.www.services;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import online.bmj.www.models.SubscriptionStatus;

@Converter(autoApply = true)
public class SubscriptionStatusConverter 
    implements AttributeConverter<SubscriptionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SubscriptionStatus status) {
        return status != null ? status.getChargebeeValue() : null;
    }

    @Override
    public SubscriptionStatus convertToEntityAttribute(String dbValue) {
        return dbValue != null ? 
            SubscriptionStatus.fromChargebeeValue(dbValue) : 
            null;
    }
}