package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/vendor/register")
    public ResponseEntity<String> registerVendor(@RequestBody Vendor vendor) {
        authService.registerVendor(vendor);
        return ResponseEntity.status(HttpStatus.CREATED).body("Vendor registered successfully");
    }

    @PostMapping("/customer/register")
    public ResponseEntity<String> registerCustomer(@RequestBody Customer customer) {
        authService.registerCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body("Customer registered successfully");
    }

    @PostMapping("/vendor/login")
    public ResponseEntity<String> loginVendor(@RequestParam String email, @RequestParam String password) {
        authService.loginVendor(email, password);
        return ResponseEntity.ok("Vendor logged in successfully");
    }

    @PostMapping("/customer/login")
    public ResponseEntity<String> loginCustomer(@RequestParam String email, @RequestParam String password) {
        authService.loginCustomer(email, password);
        return ResponseEntity.ok("Customer logged in successfully");
    }

    @PostMapping("/vendor/logout")
    public ResponseEntity<String> logoutVendor(@RequestParam String email) {
        authService.logoutVendor(email);
        return ResponseEntity.ok("Vendor logged out successfully");
    }

    @PostMapping("/customer/logout")
    public ResponseEntity<String> logoutCustomer(@RequestParam String email) {
        authService.logoutCustomer(email);
        return ResponseEntity.ok("Customer logged out successfully");
    }
}
