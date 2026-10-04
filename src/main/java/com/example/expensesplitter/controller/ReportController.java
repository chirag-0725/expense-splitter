package com.example.expensesplitter.controller;

import com.example.expensesplitter.dto.MonthlyReportResponse;
import com.example.expensesplitter.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** GET /api/reports/monthly?month=2026-10  (month defaults to the current month if omitted) */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @RequestParam(required = false) String month,
            Authentication authentication) {
        return ResponseEntity.ok(reportService.getMonthlyReport(month, authentication.getName()));
    }
}
