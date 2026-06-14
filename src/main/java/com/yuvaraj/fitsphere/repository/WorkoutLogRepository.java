package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.WorkoutLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkoutLogRepository extends MongoRepository<WorkoutLog, String> {

    List<WorkoutLog> findByUserOrderByDateDesc(String user, Pageable pageable);

    List<WorkoutLog> findByUser(String user);
}
