package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.coursework.eventticketingsystemapi.service.EventConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-configurations")
public class EventConfigurationController {
    private static final Logger log = LoggerFactory.getLogger(EventConfigurationController.class);
    private final EventConfigurationService configurationService;

    public EventConfigurationController(EventConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public ResponseEntity<EventConfiguration> getConfiguration() {
        log.info("Request received to retrieve event configuration");
        try {
            EventConfiguration configuration = configurationService.getEventConfiguration();
            log.info("Event configuration retrieved successfully");
            return ResponseEntity.ok(configuration);
        } catch (Exception e) {
            log.error("Failed to retrieve event configuration: {}", e.getMessage());
            throw e;
        }
    }


    @PostMapping
    public ResponseEntity<EventConfiguration> createConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to create new event configuration");
        try {
            EventConfiguration savedConfig = configurationService.saveConfiguration(configuration);
            log.info("Event configuration created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
        } catch (Exception e) {
            log.error("Failed to create event configuration: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping
    public ResponseEntity<EventConfiguration> updateConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to update event configuration");
        try {
            EventConfiguration updatedConfig = configurationService.saveConfiguration(configuration);
            log.info("Event configuration updated successfully");
            return ResponseEntity.ok(updatedConfig);
        } catch (Exception e) {
            log.error("Failed to update event configuration: {}", e.getMessage());
            throw e;
        }
    }
}
