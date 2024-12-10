package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.LogEntry;
import org.coursework.eventticketingsystemapi.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogService logService;

    @Autowired
    public LogController(LogService logService) {
        this.logService = logService;
    }

    // Get logs
    @GetMapping
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long afterId
    ) {
        List<LogEntry> logs = logService.getTailLogs(limit, afterId);
        return ResponseEntity.ok(logs);
    }
}