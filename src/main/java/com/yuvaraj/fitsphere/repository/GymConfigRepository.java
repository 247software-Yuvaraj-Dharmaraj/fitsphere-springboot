package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.GymConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GymConfigRepository extends MongoRepository<GymConfig, String> {
}
