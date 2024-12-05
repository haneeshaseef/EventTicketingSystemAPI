package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.repository.CustomerRepository;
import org.coursework.eventticketingsystemapi.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final CustomerService customerService;
    private final   VendorService vendorService;

    @Autowired
    public AuthService(CustomerService customerService, VendorService vendorService) {
        this.customerService = customerService;
        this.vendorService = vendorService;
    }

    // register a vendor
    public void registerVendor(Vendor vendor) {
        try {
            log.info("Registering new vendor: {}", vendor.getName());

            // Validate vendor input
            validateVendorRegistration(vendor);

            vendorService.registerVendor(vendor);
            log.info("Vendor registered successfully: {}", vendor.getName());
        } catch (Exception e) {
            log.error("Error registering vendor: {}", vendor.getName(), e);
            throw new ResourceProcessingException("Failed to register vendor: " + e.getMessage());
        }
    }

    // validate vendor registration
    private void validateVendorRegistration(Vendor vendor) {
        if (vendor == null) {
            throw new ResourceProcessingException("Vendor cannot be null");
        }

        if (vendorService.findVendorByEmail(vendor.getEmail()).isPresent()) {
            throw new ResourceProcessingException("Vendor with email already exists");
        }

        if (vendor.getPassword() == null || vendor.getPassword().length() < 6) {
            throw new ResourceProcessingException("Password must be at least 6 characters long");
        }
    }

    // register a customer
    public void registerCustomer(Customer customer) {
        try {
            log.info("Registering new customer: {}", customer.getName());

            // Validate customer input
            validateCustomerRegistration(customer);

            customerService.registerCustomer(customer);
            log.info("Customer registered successfully: {}", customer.getName());
        } catch (Exception e) {
            log.error("Error registering customer: {}", customer.getName(), e);
            throw new ResourceProcessingException("Failed to register customer: " + e.getMessage());
        }
    }

    // validate customer registration
    private void validateCustomerRegistration(Customer customer) {
        if (customer == null) {
            throw new ResourceProcessingException("Customer cannot be null");
        }

        if (customerService.findCustomerByEmail(customer.getEmail()).isPresent()) {
            throw new ResourceProcessingException("Customer with email already exists");
        }

        if (customer.getPassword() == null || customer.getPassword().length() < 6) {
            throw new ResourceProcessingException("Password must be at least 6 characters long");
        }
    }

    // login a vendor
    public void loginVendor(String email, String password) {
        try {
            log.info("Logging in vendor: {}", email);

            // Find vendor by email
            Optional<Vendor> vendorOptional = vendorService.findVendorByEmail(email);

            // Validate vendor credentials
            if (vendorOptional.isEmpty()) {
                throw new ResourceProcessingException("Vendor not found");
            }

            Vendor vendor = vendorOptional.get();

            // Basic password check (in a real system, use secure password hashing)
            if (!vendor.getPassword().equals(password)) {
                throw new ResourceProcessingException("Invalid credentials");
            }

            vendorService.reactivateVendor(vendor.getParticipantId());
            log.info("Vendor logged in successfully: {}", email);
        } catch (Exception e) {
            log.error("Login failed for vendor: {}", email, e);
            throw new ResourceProcessingException("Login failed: " + e.getMessage());
        }
    }

    // login a customer
    public void loginCustomer(String email, String password) {
        try {
            log.info("Logging in customer: {}", email);

            // Find customer by email
            Optional<Customer> customerOptional = customerService.findCustomerByEmail(email);

            // Validate customer credentials
            if (customerOptional.isEmpty()) {
                throw new ResourceProcessingException("Customer not found");
            }

            Customer customer = customerOptional.get();

            if (!customer.getPassword().equals(password)) {
                throw new ResourceProcessingException("Invalid credentials");
            }
            customerService.reactivateCustomer(customer.getParticipantId());
            log.info("Customer logged in successfully: {}", email);
        } catch (Exception e) {
            log.error("Login failed for customer: {}", email, e);
            throw new ResourceProcessingException("Login failed: " + e.getMessage());
        }
    }

    // logout a vendor
    public void logoutVendor(String email) {
        try {
            log.info("Logging out vendor: {}", email);
            Vendor vendor = vendorService.findVendorByEmail(email)
                    .orElseThrow(() -> new ResourceProcessingException("Vendor not found"));

            vendorService.deactivateVendor(vendor.getParticipantId());
            log.info("Vendor {} logged out successfully: {}", email, vendor.getName());
        } catch (Exception e) {
            log.error("Error logging out vendor: {}", email, e);
            throw new ResourceProcessingException("Failed to logout vendor: " + e.getMessage());
        }
    }

    // logout a customer
    public void logoutCustomer(String email) {
        try {
            log.info("Logging out customer: {}", email);
            Customer customer = customerService.findCustomerByEmail(email)
                    .orElseThrow(() -> new ResourceProcessingException("Customer not found"));

            customerService.deactivateCustomer(customer.getParticipantId());
            log.info("Customer {} logged out successfully: {}", email, customer.getName());
        } catch (Exception e) {
            log.error("Error logging out customer: {}", email, e);
            throw new ResourceProcessingException("Failed to logout customer: " + e.getMessage());
        }
    }
}