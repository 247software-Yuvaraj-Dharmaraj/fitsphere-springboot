package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndRole(String id, Role role);

    List<User> findByRoleOrderByNameAsc(Role role);

    long countByRole(Role role);
}
