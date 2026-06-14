package com.yuvaraj.fitsphere.service;

import com.yuvaraj.fitsphere.domain.Feedback;
import com.yuvaraj.fitsphere.domain.Role;
import com.yuvaraj.fitsphere.domain.User;
import com.yuvaraj.fitsphere.dto.FeedbackDtos.CreateFeedbackRequest;
import com.yuvaraj.fitsphere.dto.FeedbackDtos.FeedbackDto;
import com.yuvaraj.fitsphere.dto.FeedbackDtos.MemberDto;
import com.yuvaraj.fitsphere.exception.HttpException;
import com.yuvaraj.fitsphere.repository.FeedbackRepository;
import com.yuvaraj.fitsphere.repository.UserRepository;
import com.yuvaraj.fitsphere.util.TimeUtil;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final FeedbackRepository feedback;
    private final UserRepository users;

    public FeedbackService(FeedbackRepository feedback, UserRepository users) {
        this.feedback = feedback;
        this.users = users;
    }

    public FeedbackDto create(String trainerId, CreateFeedbackRequest in) {
        if (!ObjectId.isValid(in.memberId())) {
            throw new HttpException(HttpStatus.NOT_FOUND, "Member not found");
        }
        users.findByIdAndRole(in.memberId(), Role.MEMBER)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Member not found"));
        Feedback fb = new Feedback();
        fb.setTrainer(trainerId);
        fb.setMember(in.memberId());
        fb.setNote(in.note());
        fb.setWeekOf(in.weekOf() != null ? parseInstant(in.weekOf()) : TimeUtil.startOfWeek());
        feedback.save(fb);
        return new FeedbackDto(fb.getId(), fb.getNote(), fb.getWeekOf(), fb.getCreatedAt(), null);
    }

    public List<FeedbackDto> forMember(String memberId) {
        List<Feedback> items = feedback.findByMemberOrderByWeekOfDescCreatedAtDesc(memberId);
        Map<String, String> trainerNames = users.findAllById(
                        items.stream().map(Feedback::getTrainer).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        return items.stream()
                .map(f -> new FeedbackDto(f.getId(), f.getNote(), f.getWeekOf(), f.getCreatedAt(),
                        trainerNames.getOrDefault(f.getTrainer(), "Trainer")))
                .toList();
    }

    public List<MemberDto> listMembers() {
        return users.findByRoleOrderByNameAsc(Role.MEMBER).stream()
                .map(m -> new MemberDto(m.getId(), m.getName(), m.getEmail()))
                .toList();
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(value).toInstant();
        }
    }
}
