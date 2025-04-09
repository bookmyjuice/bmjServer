// package online.bmj.www.repository.jpa;

// import java.util.List;
// import java.util.Optional;

// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Repository;

// import online.bmj.www.entities.PlansEntity;

// @Repository
// public interface PlansRepository extends JpaRepository<PlansEntity, Long> {

//     /**
//      * Finds a plan by its Chargebee Plan ID.
//      * 
//      * @param planId Chargebee's unique plan ID.
//      * @return Optional containing the PlanEntity if found.
//      */
//     Optional<PlansEntity> findByPlanId(String planId);

//     /**
//      * Fetches all plans.
//      * 
//      * @return List of all available plans.
//      */
//     @Override
//     @NonNull List<PlansEntity> findAll();

//     /**
//      * Checks if a plan exists by its Chargebee Plan ID.
//      * 
//      * @param planId Chargebee's unique plan ID.
//      * @return true if the plan exists, false otherwise.
//      */
//     boolean existsByPlanId(String planId);

//     /**
//      * Deletes a plan by its Chargebee Plan ID.
//      * 
//      * @param planId Chargebee's unique plan ID.
//      */
//     void deleteByPlanId(String planId);
// }
