package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.FeedbackDtos.CreateFeedbackRequest;
import com.yuvaraj.fitsphere.dto.FeedbackDtos.FeedbackDto;
import com.yuvaraj.fitsphere.dto.FeedbackDtos.MemberDto;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedback;

    public FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @GetMapping("/me")
    public List<FeedbackDto> mine(@AuthenticationPrincipal AppUser principal) {
        return feedback.forMember(principal.id());
    }

    @PostMapping
    public ResponseEntity<FeedbackDto> create(@Valid @RequestBody CreateFeedbackRequest req, @AuthenticationPrincipal AppUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback.create(principal.id(), req));
    }

    @GetMapping("/members")
    public List<MemberDto> members() {
        return feedback.listMembers();
    }

    @GetMapping("/member/{memberId}")
    public List<FeedbackDto> forMember(@PathVariable String memberId) {
        return feedback.forMember(memberId);
    }
}
