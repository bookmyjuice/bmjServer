package com.bookmyjuice.services;

import org.springframework.stereotype.Service;

import com.chargebee.models.Event;

@Service
public class ChargeService {

    public boolean saveCharge(Event event) {
        // Charge is not a top-level object in Chargebee API v2, so this is likely a custom event or needs to be handled via Invoice/Transaction
        // For now, skip implementation or log warning
        return false;
    }

    public boolean updateCharge(Event event) {
        return false;
    }

    public boolean deleteCharge(Event event) {
        return false;
    }
}
