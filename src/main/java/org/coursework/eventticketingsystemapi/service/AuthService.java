package org.coursework.eventticketingsystemapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String ADMIN_CREDENTIALS_FILE = "admin-credentials.json";
    private final CustomerService customerService;
    private final VendorService vendorService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthService(CustomerService customerService, VendorService vendorService) {
        this.customerService = customerService;
        this.vendorService = vendorService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Register a new vendor
     *
     * @param vendor Vendor object
     * @return Registered vendor
     */
    public Vendor registerVendor(Vendor vendor) {
        try {
            log.info("Registering new vendor: {}", vendor.getName());

            // Validate vendor input
            validateVendorRegistration(vendor);

            if (vendorService.findVendorByName(vendor.getName()).isPresent()) {
                throw new ResourceProcessingException("Vendor with name already exists");
            }

             vendorService.registerVendor(vendor);
            log.info("Vendor registered successfully: {}", vendor.getName());
            return vendor;
        } catch (Exception e) {
            log.error("Error registering vendor: {}", vendor.getName(), e);
            throw new ResourceProcessingException("Failed to register vendor: " + e.getMessage());
        }
    }

    /**
     * Validate vendor registration
     *
     * @param vendor Vendor object
     */
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

    /**
     * Register a new customer
     *
     * @param customer Customer object
     * @return Registered customer
     */
    public Customer registerCustomer(Customer customer) {
        try {
            log.info("Registering new customer: {}", customer.getName());

            // Validate customer input
            validateCustomerRegistration(customer);

            if (customerService.findCustomerByEmail(customer.getEmail()).isPresent()) {
                throw new ResourceProcessingException("Customer with email already exists");
            }

            customerService.registerCustomer(customer);
            log.info("Customer registered successfully: {}", customer.getName());
        } catch (Exception e) {
            log.error("Error registering customer: {}", customer.getName(), e);
            throw new ResourceProcessingException("Failed to register customer: " + e.getMessage());
        }
        return customer;
    }

    /**
     * Validate customer registration
     *
     * @param customer Customer object
     */
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

    /**
     * Login a vendor
     *
     * @param email    Vendor email
     * @param password Vendor password
     */
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

    /**
     * Login a customer
     *
     * @param email    Customer email
     * @param password Customer password
     */
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

    /**
     * Logout a vendor
     *
     * @param email Vendor email
     */
    public void logoutVendor(String email) {
        try {
            log.info("Logging out vendor: {}", email);
            Vendor vendor = vendorService.findVendorByEmail(email).orElseThrow(() -> new ResourceProcessingException("Vendor not found"));

            vendorService.deactivateVendor(vendor.getParticipantId());
            log.info("Vendor {} logged out successfully", vendor.getName());
        } catch (Exception e) {
            log.error("Error logging out vendor: {}", email, e);
            throw new ResourceProcessingException("Failed to logout vendor: " + e.getMessage());
        }
    }

    /**
     * Logout a customer
     *
     * @param email Customer email
     */
    public void logoutCustomer(String email) {
        try {
            log.info("Logging out customer: {}", email);
            Customer customer = customerService.findCustomerByEmail(email).orElseThrow(() -> new ResourceProcessingException("Customer not found"));

            customerService.deactivateCustomer(customer.getParticipantId());
            log.info("Customer {} logged out successfully: {}", email, customer.getName());
        } catch (Exception e) {
            log.error("Error logging out customer: {}", email, e);
            throw new ResourceProcessingException("Failed to logout customer: " + e.getMessage());
        }
    }

    /**
     * admin login
     * @param username
     * @param password
     */
    public void loginAdmin(String username, String password) {
        try {
            log.info("Logging in admin: {}", username);

            // Read credentials from JSON file
            Path credentialsPath = Paths.get(ADMIN_CREDENTIALS_FILE);
            File credentialsFile = credentialsPath.toFile();

            // Check if file exists
            if (!credentialsFile.exists()) {
                log.error("Admin credentials file not found at: {}", credentialsPath.toAbsolutePath());
                throw new ResourceProcessingException("Admin credentials file not found");
            }

            // Read JSON file into a Map
            Map<String, String> adminCredentials = objectMapper.readValue(credentialsFile, Map.class);

            // Validate credentials
            if (!username.equals(adminCredentials.get("adminUserName")) ||
                    !password.equals(adminCredentials.get("adminPassword"))) {
                log.error("Invalid admin credentials for username: {}", username);
                throw new ResourceProcessingException("Invalid admin credentials");
            }

            log.info("Admin {} logged in successfully", username);
        } catch (IOException e) {
            log.error("Error reading admin credentials file: {}", e.getMessage());
            throw new ResourceProcessingException("Failed to read admin credentials");
        } catch (Exception e) {
            log.error("Error logging in admin: {}", username, e);
            throw new ResourceProcessingException("Failed to login admin: " + e.getMessage());
        }
    }

    /**
     * admin logout
     * @param username
     */
    public void logoutAdmin(String username) {
        try {
            log.info("Logging out admin: {}", username);
            log.info("Admin {} logged out successfully", username);
        } catch (Exception e) {
            log.error("Error logging out admin: {}", username, e);
            throw new ResourceProcessingException("Failed to logout admin: " + e.getMessage());
        }
    }
}