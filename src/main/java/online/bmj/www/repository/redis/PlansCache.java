// package online.bmj.www.repository.redis;

// import java.util.List;
// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.CachePut;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Component;

// import online.bmj.www.entities.PlansEntity;
// import online.bmj.www.repository.jpa.PlansRepository;

// @Component
// public class PlansCache {
//     @Autowired
//     PlansRepository planRepository;

//     /**
//      * Retrieves a plan from cache by Chargebee Plan ID.
//      * If not found, fetches from DB and caches it.
//      */
//     @Cacheable(value = "plans", key = "#planId")
//     public Optional<PlansEntity> getPlanById(String planId) {
//         return planRepository.findByPlanId(planId);
//     }

//     /**
//      * Retrieves all plans and caches the result.
//      */
//     @Cacheable(value = "allPlans")
//     public List<PlansEntity> getAllPlans() {
//         return planRepository.findAll();
//     }

//     /**
//      * Saves or updates a plan in MySQL and updates the cache.
//      */
//     @CachePut(value = "plans", key = "#plan.planId")
//     public PlansEntity saveOrUpdatePlan(PlansEntity plan) {
//         return planRepository.save(plan);
//     }

//     /**
//      * Deletes a plan from MySQL and clears the cache.
//      */
//     @CacheEvict(value = {"plans", "allPlans"}, key = "#planId")
//     public void deletePlan(String planId) {
//         planRepository.deleteByPlanId(planId);
//     }

//     /**
//      * Clears all plan caches.
//      */
//     @CacheEvict(value = {"plans", "allPlans"}, allEntries = true)
//     public void clearAllPlanCache() {
//         // This method is used to clear all cached plan data.
//     }
// }
