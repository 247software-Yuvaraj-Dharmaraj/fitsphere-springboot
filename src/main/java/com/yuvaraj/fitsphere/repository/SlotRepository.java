package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.Slot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SlotRepository extends MongoRepository<Slot, String> {

    /** Slots within a single UTC day [from, to), ordered by start time.
     *  (Uses an explicit query because two predicates on the same field
     *  aren't expressible via a derived method name in Spring Data MongoDB.) */
    @Query(value = "{ 'date': { $gte: ?0, $lt: ?1 } }", sort = "{ 'startTime': 1 }")
    List<Slot> findInDay(Instant from, Instant to);

    List<Slot> findByDateGreaterThanEqualOrderByDateAscStartTimeAsc(Instant from);
}
