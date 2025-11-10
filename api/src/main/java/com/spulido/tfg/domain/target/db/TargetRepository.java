package com.spulido.tfg.domain.target.db;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.spulido.tfg.domain.target.model.Target;

public interface TargetRepository extends MongoRepository<Target, String> {
    @Query("{ 'ipOrDomain' : ?0 }")
    Optional<Target> findByIpOrDomain(String ipOrDomain);
    
    Optional<Target> findByUniqueId(String uniqueId);

    @Query("{ 'assignedAgent' : ?0 }")
    Optional<Target> findByAssignedAgent(String agentId);
}
