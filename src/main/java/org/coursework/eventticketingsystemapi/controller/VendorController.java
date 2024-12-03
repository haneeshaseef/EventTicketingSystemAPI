package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.service.VendorService;
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
@RequestMapping("/api/vendors")
@Validated
public class VendorController {
    private static final Logger log = LoggerFactory.getLogger(VendorController.class);

    private final VendorService vendorService;

    @Autowired
    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    //Get all Vendors
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllVendors() {
        log.debug("Retrieving all vendors");
        Map<String, Vendor> allVendors = vendorService.getAllVendors();

        Map<String, Object> response = new HashMap<>();
        response.put("vendors", allVendors);
        response.put("count", allVendors.size());

        log.info("Successfully retrieved {} all vendors", allVendors.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveVendors() {
        log.debug("Retrieving active vendors");
        Map<String, Vendor> activeVendors = vendorService.getActiveVendors();

        Map<String, Object> response = new HashMap<>();
        response.put("vendors", activeVendors);
        response.put("count", activeVendors.size());

        log.info("Successfully retrieved {} active vendors", activeVendors.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerVendor(@RequestBody Vendor vendor) {
        validateVendorInput(vendor);

        log.debug("Registering new vendor: {}", vendor.getName());
        Vendor registeredVendor = vendorService.registerVendor(vendor);

        Map<String, Object> response = new HashMap<>();
        response.put("vendor", registeredVendor);
        response.put("message", "Vendor successfully registered");

        log.info("Successfully registered vendor: {}", registeredVendor.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //update vendor by name
    @PutMapping("/{vendorName}")
    public ResponseEntity<Map<String, Object>> updateVendor(@PathVariable String vendorName, @RequestBody Vendor vendor) {
        validateVendorInput(vendor);

        log.debug("Updating vendor: {}", vendorName);
        Vendor updatedVendor = vendorService.updateVendor(vendorName, vendor);

        Map<String, Object> response = new HashMap<>();
        response.put("vendor", updatedVendor);
        response.put("message", "Vendor successfully updated");

        log.info("Successfully updated vendor: {}", vendorName);
        return ResponseEntity.ok(response);
    }

    //deactivate vendor by name
    @PutMapping("/{vendorName}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateVendor(@PathVariable String vendorName) {
        log.debug("Deactivating vendor: {}", vendorName);
        vendorService.deactivateVendor(vendorName);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Vendor successfully deactivated");
        response.put("vendorName", vendorName);

        log.info("Successfully deactivated vendor: {}", vendorName);
        return ResponseEntity.ok(response);
    }

    //reactive vendor by name
    @PutMapping("/{vendorName}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateVendor(@PathVariable String vendorName) {
        log.debug("Reactivating vendor: {}", vendorName);
        vendorService.reactivateVendor(vendorName);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Vendor successfully reactivated");
        response.put("vendorName", vendorName);

        log.info("Successfully reactivated vendor: {}", vendorName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Map<String, Object>> getVendorByName(@PathVariable String name) {
        log.debug("Retrieving vendor by name: {}", name);
        Optional<Vendor> vendorOptional = vendorService.findVendorByName(name);

        if (vendorOptional.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "No vendor found with name: " + name);
            return ResponseEntity.notFound().build();
        }

        Vendor vendor = vendorOptional.get();
        Map<String, Object> response = new HashMap<>();
        response.put("vendor", vendor);
        response.put("totalTicketsSold", vendor.getTotalTicketsSold());
        response.put("isActive", vendor.isActive());

        log.info("Successfully retrieved vendor by name: {}", name);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{vendorName}")
    public ResponseEntity<Map<String, Object>> deleteVendor(@PathVariable String vendorName) {
        log.debug("Deleting vendor: {}", vendorName);

        vendorService.deactivateVendor(vendorName);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Vendor successfully deleted");
        response.put("vendorName", vendorName);

        log.info("Successfully deleted vendor: {}", vendorName);
        return ResponseEntity.ok(response);
    }

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
        if (vendor.getTicketsPerRelease() <= 0) {
            throw new IllegalArgumentException("Vendor tickets per release must be greater than 0");
        }
        if (vendor.getTicketReleaseInterval() <= 0) {
            throw new IllegalArgumentException("Vendor ticket release interval must be greater than 0");
        }
    }
}