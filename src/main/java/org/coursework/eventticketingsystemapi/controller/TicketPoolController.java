package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ticket-pool")
public class TicketPoolController {
    private static final Logger log = LoggerFactory.getLogger(TicketPoolController.class);

    private final TicketPoolService ticketPoolService;
    private final EventConfigurationService configurationService;
    private final TicketService ticketService;
    private final CustomerService customerService;
    private final VendorService vendorService;

    @Autowired
    public TicketPoolController(TicketPoolService ticketPoolService,
                                EventConfigurationService configurationService, TicketService ticketService, CustomerService customerService, VendorService vendorService) {
        this.ticketPoolService = ticketPoolService;
        this.configurationService = configurationService;
        this.ticketService = ticketService;
        this.customerService = customerService;
        this.vendorService = vendorService;
    }

    // Event Configuration Endpoints
    @GetMapping("/configuration")
    public ResponseEntity<EventConfiguration> getConfiguration() {
        log.info("Request received to retrieve event configuration");
        EventConfiguration configuration = configurationService.getEventConfiguration();
        log.info("Event configuration retrieved successfully");
        return ResponseEntity.ok(configuration);
    }

    // Create new configuration
    @PostMapping("/configuration")
    public ResponseEntity<EventConfiguration> createConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to create new event configuration");
        EventConfiguration savedConfig = configurationService.saveConfiguration(configuration);
        log.info("Event configuration created successfully");
        // After saving configuration, update the ticket pool
        ticketPoolService.configureEvent(savedConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
    }

    // Update configuration
    @PutMapping("/configuration")
    public ResponseEntity<EventConfiguration> updateConfiguration(@RequestBody EventConfiguration configuration) {
        log.info("Request received to update event configuration");
        EventConfiguration updatedConfig = configurationService.saveConfiguration(configuration);
        log.info("Event configuration updated successfully");
        // After updating configuration, reconfigure the ticket pool
        ticketPoolService.configureEvent(updatedConfig);
        return ResponseEntity.ok(updatedConfig);
    }

    //Get ticket pool status
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

    //get all tickets
    @GetMapping("/tickets")
    public ResponseEntity<List<Ticket>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    //find ticket by customer name
    @GetMapping("/customer/name/{customerName}/tickets")
    public ResponseEntity<List<Ticket>> findTicketsByCustomer(@PathVariable String customerName) {
        Optional<Customer> customer = customerService.findCustomerByName(customerName);

        if (customer.isPresent()) {
            List<Ticket> tickets = ticketService.getTicketsByCustomer(customer.get().getParticipantId());
            return ResponseEntity.ok(tickets);
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    //find ticket by vendor name
    @GetMapping("/vendor/name/{vendorName}/tickets")
    public ResponseEntity<List<Ticket>> findTicketsByVendor(@PathVariable String vendorName) {
        Optional<Vendor> vendor = vendorService.findVendorByName(vendorName);

        if (vendor.isPresent()) {
            List<Ticket> tickets = ticketService.getTicketsByVendor(vendor.get().getParticipantId());
            return ResponseEntity.ok(tickets);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //deleteTicketForCustomer with ticket ID
    @DeleteMapping("/tickets/{ticketId}/deleteTicket")
    public ResponseEntity<String> deleteTicketForCustomer(@PathVariable String ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok("Ticket deleted successfully");
    }

    //get ticket by id
    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable String ticketId) {
        Ticket ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ticket);
    }

}