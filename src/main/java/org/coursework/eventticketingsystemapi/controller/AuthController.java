package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Vendor Registration
    @PostMapping("/vendor/register")
    public ResponseEntity<Map<String, Object>> registerVendor(@RequestBody Vendor vendor) {
        log.debug("Registering new vendor: {}", vendor.getName());
        validateVendorInput(vendor);

        Vendor registeredVendor = authService.registerVendor(vendor);

        Map<String, Object> response = new HashMap<>();
        response.put("vendor", registeredVendor);
        response.put("message", "Vendor successfully registered");

        log.info("Successfully registered vendor: {}", registeredVendor.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Customer Registration
    @PostMapping("/customer/register")
    public ResponseEntity<Map<String, Object>> registerCustomer(@RequestBody Customer customer) {
        log.debug("Registering new customer: {}", customer.getName());
        validateCustomerInput(customer);

        Customer registeredCustomer = authService.registerCustomer(customer);

        Map<String, Object> response = new HashMap<>();
        response.put("customer", registeredCustomer);
        response.put("message", "Customer successfully registered");

        log.info("Successfully registered customer: {}", registeredCustomer.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // vendor login
    @PostMapping("/vendor/login")
    public ResponseEntity<Map<String, Object>> loginVendor(@RequestParam String email, @RequestParam String password) {
        log.debug("Vendor login attempt: {}", email);

        authService.loginVendor(email, password);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "Vendor logged in successfully");

        log.info("Successfully logged in vendor: {}", email);
        return ResponseEntity.ok(response);
    }

    // customer login
    @PostMapping("/customer/login")
    public ResponseEntity<Map<String, Object>> loginCustomer(@RequestParam String email, @RequestParam String password) {
        log.debug("Customer login attempt: {}", email);

        authService.loginCustomer(email, password);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "Customer logged in successfully");

        log.info("Successfully logged in customer: {}", email);
        return ResponseEntity.ok(response);
    }

    // vendor logout
    @PostMapping("/vendor/logout")
    public ResponseEntity<Map<String, Object>> logoutVendor(@RequestParam String email) {
        log.debug("Vendor logout attempt: {}", email);

        authService.logoutVendor(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "Vendor logged out successfully");

        log.info("Successfully logged out vendor: {}", email);
        return ResponseEntity.ok(response);
    }

    // customer logout
    @PostMapping("/customer/logout")
    public ResponseEntity<Map<String, Object>> logoutCustomer(@RequestParam String email) {
        log.debug("Customer logout attempt: {}", email);

        authService.logoutCustomer(email);

        Map<String, Object> response = new HashMap<>();
        response.put("email", email);
        response.put("message", "Customer logged out successfully");

        log.info("Successfully logged out customer: {}", email);
        return ResponseEntity.ok(response);
    }

    // admin login
    @PostMapping("/admin/login")
    public ResponseEntity<Map<String, Object>> loginAdmin(@RequestParam String username, @RequestParam String password) {
        log.debug("Admin login attempt: {}", username);

        authService.loginAdmin(username, password);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("message", "Admin logged in successfully");

        log.info("Successfully logged in admin: {}", username);
        return ResponseEntity.ok(response);
    }

    // admin logout
    @PostMapping("/admin/logout")
    public ResponseEntity<Map<String, Object>> logoutAdmin(@RequestParam String username) {
        log.debug("Admin logout attempt: {}", username);

        authService.logoutAdmin(username);

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("message", "Admin logged out successfully");

        log.info("Successfully logged out admin: {}", username);
        return ResponseEntity.ok(response);
    }

   // validate vendor input
    private void validateVendorInput(Vendor vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor data cannot be null");
        }
        if (vendor.getName() == null || vendor.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor name is required");
        }
        if (vendor.getEmail() == null || vendor.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor email is required");
        }
        if (vendor.getPassword() == null || vendor.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor password is required");
        }
    }

    // validate customer input
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
        if (customer.getPassword() == null || customer.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer password is required");
        }
    }
}