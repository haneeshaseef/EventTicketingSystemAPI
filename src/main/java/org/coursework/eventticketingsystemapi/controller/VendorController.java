package org.coursework.eventticketingsystemapi.controller;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.service.VendorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {
    private static final Logger log = LoggerFactory.getLogger(VendorController.class);

    @Autowired
    private VendorService vendorService;

    @GetMapping
    public ResponseEntity<Map<String, Vendor>> getActiveVendors() {
        try {
            Map<String, Vendor> activeVendors = vendorService.getActiveVendors();
            return ResponseEntity.ok(activeVendors);
        } catch (ResourceProcessingException e) {
            log.error("Error retrieving active vendors: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Vendor> registerVendor(@RequestBody Vendor vendor) {
        try {
            Vendor registeredVendor = vendorService.registerVendor(vendor);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredVendor);
        } catch (IllegalArgumentException e) {
            log.error("Error registering vendor: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(null);
        } catch (ResourceProcessingException e) {
            log.error("Error registering vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{vendorId}/deactivate")
    public ResponseEntity<Void> deactivateVendor(@PathVariable String vendorId) {
        try {
            vendorService.deactivateVendor(vendorId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deactivating vendor: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        } catch (ResourceProcessingException e) {
            log.error("Error deactivating vendor: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Vendor> findVendorByName(@RequestParam("name") String name) {
        try {
            Optional<Vendor> vendor = vendorService.findVendorByName(name);
            return vendor.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (ResourceProcessingException e) {
            log.error("Error finding vendor by name: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}