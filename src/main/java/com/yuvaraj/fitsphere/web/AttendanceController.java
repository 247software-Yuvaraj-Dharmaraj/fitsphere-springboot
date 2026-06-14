package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.AttendanceDtos.AttendanceDto;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.BestTime;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.MonthEntry;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Occupancy;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Summary;
import com.yuvaraj.fitsphere.dto.AttendanceDtos.Trend;
import com.yuvaraj.fitsphere.security.AppUser;
import com.yuvaraj.fitsphere.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendance;

    public AttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceDto> checkIn(@AuthenticationPrincipal AppUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendance.checkIn(principal.id()));
    }

    @PostMapping("/check-out")
    public AttendanceDto checkOut(@AuthenticationPrincipal AppUser principal) {
        return attendance.checkOut(principal.id());
    }

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal AppUser principal) {
        return attendance.getSummary(principal.id());
    }

    @GetMapping("/month")
    public List<MonthEntry> month(@RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month,
                                  @AuthenticationPrincipal AppUser principal) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int y = year != null ? year : today.getYear();
        int m = month != null ? month : today.getMonthValue();
        return attendance.getMonth(principal.id(), y, m);
    }

    @GetMapping("/occupancy")
    public Occupancy occupancy() {
        return attendance.getOccupancy();
    }

    @GetMapping("/trend")
    public Trend trend(@RequestParam(required = false) Integer days, @AuthenticationPrincipal AppUser principal) {
        int d = Math.min(90, Math.max(7, days != null ? days : 14));
        return attendance.getTrend(principal.id(), d);
    }

    @GetMapping("/best-time")
    public BestTime bestTime() {
        return attendance.getBestTime();
    }
}
