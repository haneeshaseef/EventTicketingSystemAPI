package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<Map<String, Customer>> getActiveCustomers() {
        try {
            Map<String, Customer> activeCustomers = customerService.getActiveCustomers();
            return ResponseEntity.ok(activeCustomers);
        } catch (ResourceProcessingException e) {
            log.error("Error retrieving active customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Customer> registerCustomer(@RequestBody Customer customer) {
        try {
            Customer registeredCustomer = customerService.registerCustomer(customer);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredCustomer);
        } catch (IllegalArgumentException e) {
            log.error("Error registering customer: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        } catch (ResourceProcessingException e) {
            log.error("Error registering customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{customerId}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable String customerId) {
        try {
            customerService.deactivateCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deactivating customer: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (ResourceProcessingException e) {
            log.error("Error deactivating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{customerId}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable String customerId) {
        try {
            customerService.activateCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error activating customer: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (ResourceProcessingException e) {
            log.error("Error activating customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String customerId) {
        try {
            Customer customer = customerService.getCustomerById(customerId);
            return ResponseEntity.ok(customer);
        } catch (ResourceProcessingException e) {
            log.error("Error retrieving customer: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Customer> getCustomerByEmail(@PathVariable String email) {
        try {
            Customer customer = customerService.getCustomerByEmail(email);
            return ResponseEntity.ok(customer);
        } catch (ResourceProcessingException e) {
            log.error("Error retrieving customer by email: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/with-tickets")
    public ResponseEntity<List<Customer>> getCustomersWithTickets() {
        try {
            List<Customer> customers = customerService.getCustomersWithTickets();
            return ResponseEntity.ok(customers);
        } catch (ResourceProcessingException e) {
            log.error("Error retrieving customers with tickets: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> checkCustomerExists(@RequestParam String email) {
        try {
            boolean exists = customerService.existsByEmail(email);
            return ResponseEntity.ok(exists);
        } catch (ResourceProcessingException e) {
            log.error("Error checking customer existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String customerId) {
        try {
            customerService.deleteCustomer(customerId);
            return ResponseEntity.noContent().build();
        } catch (ResourceProcessingException e) {
            log.error("Error deleting customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}