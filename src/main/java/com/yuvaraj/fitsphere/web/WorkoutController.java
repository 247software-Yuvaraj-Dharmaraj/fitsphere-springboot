package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.WorkoutDtos.LogWorkoutRequest;
import com.yuvaraj.fitsphere.dto.WorkoutDtos.Stats;
import com.yuvaraj.fitsphere.dto.WorkoutDtos.WorkoutDto;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workouts;

    public WorkoutController(WorkoutService workouts) {
        this.workouts = workouts;
    }

    @PostMapping
    public ResponseEntity<WorkoutDto> log(@Valid @RequestBody LogWorkoutRequest req, @AuthenticationPrincipal AppUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workouts.log(principal.id(), req));
    }

    @GetMapping
    public List<WorkoutDto> recent(@RequestParam(required = false) Integer limit, @AuthenticationPrincipal AppUser principal) {
        int l = Math.min(50, Math.max(1, limit != null ? limit : 10));
        return workouts.recent(principal.id(), l);
    }

    @GetMapping("/stats")
    public Stats stats(@AuthenticationPrincipal AppUser principal) {
        return workouts.stats(principal.id());
    }
}
