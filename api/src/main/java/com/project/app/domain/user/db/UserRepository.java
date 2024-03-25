package com.project.app.domain.user.db;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.project.app.domain.user.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
