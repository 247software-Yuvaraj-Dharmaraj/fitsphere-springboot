package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    Optional<Attendance> findFirstByUserAndCheckOutAtIsNull(String user);

    long countByCheckOutAtIsNullAndCheckInAtGreaterThanEqual(Instant since);

    List<Attendance> findByUser(String user);

    List<Attendance> findByUserAndCheckInAtGreaterThanEqual(String user, Instant since);

    List<Attendance> findByUserAndCheckInAtBetweenOrderByCheckInAtAsc(String user, Instant from, Instant to);

    List<Attendance> findByCheckInAtGreaterThanEqual(Instant since);
}
