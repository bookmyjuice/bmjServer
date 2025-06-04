package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.repository.PlanRepository;
import com.chargebee.models.Event;
import com.bookmyjuice.models.entities.PlanEntity;
import com.bookmyjuice.models.mappers.PlanMapper;

@Service
public class PlanService {
    @Autowired
    private PlanRepository planRepository;

    public boolean savePlan(Event event) {
        var plan = event.content().plan();
        // Check if plan exists, update if so, else create
        PlanEntity entity = planRepository.findById(plan.id())
            .orElseGet(() -> PlanMapper.toEntity(plan));
        PlanMapper.toEntity(plan, entity); // update fields from event
        planRepository.save(entity);
        return true;
    }

    public boolean updatePlan(Event event) {
        var plan = event.content().plan();
        // Always upsert: update if exists, else create
        PlanEntity entity = planRepository.findById(plan.id())
            .orElseGet(() -> PlanMapper.toEntity(plan));
        PlanMapper.toEntity(plan, entity);
        planRepository.save(entity);
        return true;
    }

    public boolean deletePlan(Event event) {
        var plan = event.content().plan();
        if (planRepository.existsById(plan.id())) {
            planRepository.deleteById(plan.id());
        }
        return true;
    }
}
