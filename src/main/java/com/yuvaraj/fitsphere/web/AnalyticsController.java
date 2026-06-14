package com.yuvaraj.fitsphere.web;

import com.yuvaraj.fitsphere.dto.AnalyticsDtos.MembersResponse;
import com.yuvaraj.fitsphere.dto.AnalyticsDtos.Overview;
import com.yuvaraj.fitsphere.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/overview")
    public Overview overview() {
        return analytics.overview();
    }

    @GetMapping("/members")
    public MembersResponse members(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "totalVisits") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "ALL") String status) {
        int p = Math.max(0, page);
        int size = Math.min(100, Math.max(1, pageSize));
        return analytics.members(q, p, size, sort, dir, status);
    }
}
