package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.service.EventConfigurationService;
import org.coursework.eventticketingsystemapi.service.TicketPoolService;
import org.coursework.eventticketingsystemapi.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ticket-pool")
public class TicketPoolController {
    private static final Logger log = LoggerFactory.getLogger(TicketPoolController.class);

    private final TicketPoolService ticketPoolService;
    private final EventConfigurationService configurationService;
    private final TicketService ticketService;

    @Autowired
    public TicketPoolController(TicketPoolService ticketPoolService,
                                EventConfigurationService configurationService,TicketService ticketService) {
        this.ticketPoolService = ticketPoolService;
        this.configurationService = configurationService;
        this.ticketService = ticketService;
    }

    // Event Configuration Endpoints
    @GetMapping("/configuration")
    public ResponseEntity<EventConfiguration> getConfiguration() {
        log.info("Request received to retrieve event configuration");
        EventConfiguration configuration = configurationService.getEventConfiguration();
        log.info("Event configuration retrieved successfully");
        return ResponseEntity.ok(configuration);
    }

    @PostMapping("/configuration")
    public ResponseEntity<EventConfiguration> createConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to create new event configuration");
        EventConfiguration savedConfig = configurationService.saveConfiguration(configuration);
        log.info("Event configuration created successfully");
        // After saving configuration, update the ticket pool
        ticketPoolService.configureEvent(savedConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
    }

    @PutMapping("/configuration")
    public ResponseEntity<EventConfiguration> updateConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to update event configuration");
        EventConfiguration updatedConfig = configurationService.saveConfiguration(configuration);
        log.info("Event configuration updated successfully");
        // After updating configuration, reconfigure the ticket pool
        ticketPoolService.configureEvent(updatedConfig);
        return ResponseEntity.ok(updatedConfig);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", ticketPoolService.isConfigured());
        status.put("availableTickets", ticketPoolService.getAvailableTickets());

        EventConfiguration config = ticketPoolService.getEventConfiguration();
        if (config != null) {
            status.put("eventName", config.getEventName());
            status.put("maxCapacity", config.getMaxCapacity());
            status.put("ticketReleaseRate", config.getTicketReleaseRate());
            status.put("customerRetrievalRate", config.getCustomerRetrievalRate());
        }

        return ResponseEntity.ok(status);
    }

    @GetMapping("/tickets/event/{eventName}")
    public ResponseEntity<Map<String, Object>> getEventTicketStatus(@PathVariable String eventName) {
        List<Ticket> eventTickets = ticketService.getTicketsByEventName(eventName);

        Map<String, Object> ticketStatus = new HashMap<>();
        ticketStatus.put("totalTickets", eventTickets.size());
        ticketStatus.put("vendorDistribution", getVendorTicketDistribution(eventTickets));
        ticketStatus.put("customerDistribution", getCustomerTicketDistribution(eventTickets));

        return ResponseEntity.ok(ticketStatus);
    }

    @GetMapping("/tickets/vendor/{vendorId}")
    public ResponseEntity<Map<String, Object>> getVendorTicketStatus(@PathVariable String vendorId) {
        List<Ticket> vendorTickets = ticketService.getTicketsByVendor(vendorId);

        Map<String, Object> vendorStatus = new HashMap<>();
        vendorStatus.put("totalTicketsSold", vendorTickets.size());
        vendorStatus.put("ticketEvents", vendorTickets.stream()
                .map(Ticket::getEventName)
                .distinct()
                .collect(Collectors.toList()));

        return ResponseEntity.ok(vendorStatus);
    }

    @GetMapping("/tickets/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerTicketStatus(@PathVariable String customerId) {
        List<Ticket> customerTickets = ticketService.getTicketsByCustomer(customerId);

        Map<String, Object> customerStatus = new HashMap<>();
        customerStatus.put("totalTicketsPurchased", customerTickets.size());
        customerStatus.put("ticketEvents", customerTickets.stream()
                .map(Ticket::getEventName)
                .distinct()
                .collect(Collectors.toList()));

        return ResponseEntity.ok(customerStatus);
    }

    //delete ticket
    @DeleteMapping("/tickets/{ticketId}")
    public ResponseEntity<String> deleteTicket(@PathVariable String ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok("Ticket deleted successfully");
    }

    //get ticket by id
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable String ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }

    private Map<String, Long> getVendorTicketDistribution(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getVendor().getName(),
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getCustomerTicketDistribution(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        ticket -> ticket.getCustomer().getName(),
                        Collectors.counting()
                ));
    }
}