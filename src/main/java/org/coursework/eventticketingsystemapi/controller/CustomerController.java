package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
@Validated
public class CustomerController {
    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    //Get all customers
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        log.debug("Retrieving all customers");
        Map<String, Customer> allCustomers = customerService.getAllCustomers();

        Map<String, Object> response = new HashMap<>();
        response.put("customers", allCustomers);
        response.put("count", allCustomers.size());

        log.info("Successfully retrieved {} all customers", allCustomers.size());
        return ResponseEntity.ok(response);
    }

    //Get active customers
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveCustomers() {
        log.debug("Retrieving active customers");
        Map<String, Customer> activeCustomers = customerService.getActiveCustomers();

        Map<String, Object> response = new HashMap<>();
        response.put("customers", activeCustomers);
        response.put("count", activeCustomers.size());

        log.info("Successfully retrieved {} active customers", activeCustomers.size());
        return ResponseEntity.ok(response);
    }

    //Register new customer
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerCustomer(@RequestBody Customer customer) {
        validateCustomerInput(customer);

        log.debug("Registering new customer: {}", customer.getName());
        Customer registeredCustomer = customerService.registerCustomer(customer);

        Map<String, Object> response = new HashMap<>();
        response.put("customer", registeredCustomer);
        response.put("message", "Customer successfully registered");

        log.info("Successfully registered customer: {}", registeredCustomer.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // deactivate customer by name
    @PutMapping("/{customerName}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateCustomer(@PathVariable String customerName) {
        log.debug("Deactivating customer: {}", customerName);
        Optional<Customer> customerOptional = customerService.findCustomerByName(customerName);

        Customer customer = customerOptional.get();

        customerService.deactivateCustomer(customer.getParticipantId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer successfully deactivated");
        response.put("customerName", customer.getName());
        response.put("ticketsPurchased", customer.getTotalTicketsPurchased());

        log.info("Successfully deactivated customer: {}", customerName);
        return ResponseEntity.ok(response);
    }

    // reactive customer by name
    @PutMapping("/{customerName}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateCustomer(@PathVariable String customerName) {
        log.debug("Reactivating customer: {}", customerName);

        Optional<Customer> customerOptional = customerService.findCustomerByName(customerName);
        if (customerOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No customer found with name: " + customerName);
            return ResponseEntity.notFound().build();
        }
        Customer customer = customerOptional.get();
        customerService.reactivateCustomer(customer.getParticipantId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer successfully reactivated");
        response.put("customerName", customer.getName());
        response.put("ticketsPurchased", customer.getTotalTicketsPurchased());

        log.info("Successfully reactivated customer: {}", customerName);
        return ResponseEntity.ok(response);
    }

    //find total tickets purchased by customer
    @GetMapping("/{customerName}/totalTicketsPurchased")
    public ResponseEntity<Map<String, Object>> findTotalTicketsPurchasedByCustomer(@PathVariable String customerName) {
        log.debug("Finding total tickets purchased by customer: {}", customerName);
        int totalTicketsPurchased = customerService.findTotalTicketsPurchasedByCustomer(customerName);

        Map<String, Object> response = new HashMap<>();
        response.put("customerName", customerName);
        response.put("totalTicketsPurchased", totalTicketsPurchased);

        log.info("Successfully found total tickets purchased by customer: {}", customerName);
        return ResponseEntity.ok(response);
    }

    //get customer by ID
    @GetMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable String customerId) {
        log.debug("Retrieving customer by ID: {}", customerId);
        Customer customer = customerService.getCustomerById(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);
        response.put("ticketsPurchased", customer.getTotalTicketsPurchased());
        response.put("isActive", customer.isActive());

        log.info("Successfully retrieved customer: {}", customer.getName());
        return ResponseEntity.ok(response);
    }

    //get customer by name
    @GetMapping("/name/{name}")
    public ResponseEntity<Map<String, Object>> getCustomerByName(@PathVariable String name) {
        log.debug("Retrieving customer by name: {}", name);
        Optional<Customer> customerOptional = customerService.findCustomerByName(name);

        if (customerOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No customer found with name: " + name);
            return ResponseEntity.notFound().build();
        }

        Customer customer = customerOptional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);
        response.put("ticketsPurchased", customer.getTotalTicketsPurchased());
        response.put("isActive", customer.isActive());

        log.info("Successfully retrieved customer by name: {}", name);
        return ResponseEntity.ok(response);
    }

    //delete customer by ID
    @DeleteMapping("/{customerId}")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable String customerId) {
        log.debug("Deleting customer: {}", customerId);
        Customer customer = customerService.getCustomerById(customerId);
        String customerName = customer.getName();

        customerService.deactivateCustomer(customerId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer successfully deleted");
        response.put("customerName", customerName);

        log.info("Successfully deleted customer: {}", customerName);
        return ResponseEntity.ok(response);
    }

    //get customer details by email
    @GetMapping("/details/{email}")
    public ResponseEntity<Map<String, Object>> getCustomerDetailsByEmail(@PathVariable String email) {
        log.debug("Retrieving customer details for email: {}", email);
        Optional<Customer> customerOptional = customerService.findCustomerByEmail(email);

        if (customerOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No customer found with email: " + email);
            return ResponseEntity.notFound().build();
        }

        Customer customer = customerOptional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("customer", customer);

        log.info("Successfully retrieved customer details for email: {}", email);
        return ResponseEntity.ok(response);
    }

    //validate customer input
    private void validateCustomerInput(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer data cannot be null");
        }
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }
        if (customer.getTicketsToPurchase() <= 0) {
            throw new IllegalArgumentException("Tickets to purchase must be greater than 0");
        }
        if (customer.getTicketRetrievalInterval() <= 0) {
            throw new IllegalArgumentException("Ticket retrieval interval must be greater than 0");
        }
    }
}