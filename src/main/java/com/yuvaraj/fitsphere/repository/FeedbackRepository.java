package com.yuvaraj.fitsphere.repository;

import com.yuvaraj.fitsphere.domain.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {

    List<Feedback> findByMemberOrderByWeekOfDescCreatedAtDesc(String member);
}
