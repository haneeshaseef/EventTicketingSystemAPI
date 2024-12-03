package org.coursework.eventticketingsystemapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventConfigurationService {
    private static final Logger log = LoggerFactory.getLogger(EventConfigurationService.class);
    private static final String CONFIGURATION_FILE = "event-configuration.json";
    private final ObjectMapper objectMapper;

    public EventConfigurationService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public EventConfiguration getEventConfiguration() {
        log.debug("Loading event configuration");
        try {
            EventConfiguration configuration = loadConfiguration();
            if (configuration != null) {
                validateConfiguration(configuration);
                return configuration;
            }
            log.info("No configuration file found at: {}", Paths.get(CONFIGURATION_FILE).toAbsolutePath());
            return null;
        } catch (IOException e) {
            log.error("Failed to read configuration file: {}", e.getMessage());
            throw new ResourceProcessingException("Unable to process configuration file");
        }
    }

    private EventConfiguration loadConfiguration() throws IOException {
        Path path = Paths.get(CONFIGURATION_FILE);
        File file = path.toFile();

        if (!file.exists()) {
            return null;
        }

        return objectMapper.readValue(file, EventConfiguration.class);
    }

    private void validateConfiguration(EventConfiguration configuration) {
        List<String> errors = validateConfigurationRules(configuration);

        if (!errors.isEmpty()) {
            String errorMsg = String.join("; ", errors);
            log.error("Configuration validation failed: {}", errorMsg);
            throw new ResourceProcessingException("Invalid configuration provided: " + errorMsg);
        }
    }

    private List<String> validateConfigurationRules(EventConfiguration configuration) {
        List<String> errors = new ArrayList<>();

        if (configuration == null) {
            errors.add("Configuration cannot be null");
            return errors;
        }

        if (configuration.getMaxCapacity() <= 0) {
            errors.add("Maximum capacity must be greater than zero");
        }
        if (configuration.getTicketReleaseRate() <= 0) {
            errors.add("Ticket release rate must be greater than zero");
        }
        if (configuration.getCustomerRetrievalRate() <= 0) {
            errors.add("Customer retrieval rate must be greater than zero");
        }
        if (configuration.getEventName() == null || configuration.getEventName().trim().isEmpty()) {
            errors.add("Event name must not be empty");
        }

        return errors;
    }

    public EventConfiguration saveConfiguration(EventConfiguration configuration) {
        try {
            validateConfiguration(configuration);
            Path path = Paths.get(CONFIGURATION_FILE);
            objectMapper.writeValue(path.toFile(), configuration);
            log.info("Configuration saved to: {}", path.toAbsolutePath());
            return configuration;
        } catch (IOException e) {
            log.error("Failed to save configuration: {}", e.getMessage());
            throw new ResourceProcessingException("Unable to save configuration: " + e.getMessage());
        }
    }

}
