package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VendorService {
    private static final Logger log = LoggerFactory.getLogger(VendorService.class);

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private TicketPoolService ticketPoolService;

    private final Map<String, Vendor> activeVendors = new ConcurrentHashMap<>();

    public Map<String, Vendor> getActiveVendors() {
        try {
            log.debug("Retrieving active vendors");
            vendorRepository.findByIsActive(true)
                    .forEach(vendor -> activeVendors.put(vendor.getParticipantId(), vendor));
            log.info("Successfully retrieved {} active vendors", activeVendors.size());
            return activeVendors;
        } catch (Exception e) {
            log.error("Error retrieving active vendors: {}", e.getMessage(), e);
            throw new ResourceProcessingException("Failed to retrieve active vendors");
        }
    }

    public Vendor registerVendor(Vendor vendor) {
        try {
            log.info("Attempting to register new vendor: {}", vendor.getName());

            // Validate vendor configuration
            if (vendor.getTicketsPerRelease() <= 0 || vendor.getTicketReleaseInterval() <= 0) {
                log.error("Invalid vendor configuration: ticketsPerRelease={}, ticketReleaseInterval={}",
                        vendor.getTicketsPerRelease(), vendor.getTicketReleaseInterval());
                throw new IllegalArgumentException("Invalid vendor configuration parameters");
            }

            // Check for existing vendor with same email
            Optional<Vendor> existingVendor = vendorRepository.findByEmailIgnoreCase(vendor.getEmail());
            if (existingVendor.isPresent()) {
                if (existingVendor.get().getIsActive()) {
                    log.error("Vendor with email {} is already active", vendor.getEmail());
                    throw new IllegalArgumentException("Vendor with this email is already active");
                } else {
                    // Reactivate the existing vendor with preserved ticket counts
                    Vendor reactivatedVendor = existingVendor.get();
                    reactivatedVendor.setIsActive(true);
                    reactivatedVendor.setTotalTicketsSold(existingVendor.get().getTotalTicketsSold());
                    vendor = vendorRepository.save(reactivatedVendor);
                    activeVendors.put(vendor.getParticipantId(), vendor);
                    startVendorThread(vendor);
                    log.info("Reactivated existing vendor with email {} and {} tickets sold",
                            vendor.getEmail(), vendor.getTotalTicketsSold());
                    return vendor;
                }
            }

            // Set vendor as active by default
            vendor.setIsActive(true);
            vendor.setTotalTicketsSold(0);

            Vendor savedVendor = vendorRepository.save(vendor);
            activeVendors.put(savedVendor.getParticipantId(), savedVendor);
            startVendorThread(savedVendor);
            log.info("Vendor {} successfully registered with ID: {}",
                    savedVendor.getName(), savedVendor.getParticipantId());
            return savedVendor;
        } catch (Exception e) {
            log.error("Error registering vendor {}: {}", vendor.getName(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to register vendor");
        }
    }

    private void startVendorThread(Vendor vendor) {
        try {
            vendor.setTicketPoolService(ticketPoolService);
            vendor.startVendor();
            Thread vendorThread = new Thread(vendor);
            vendorThread.setName("Vendor-" + vendor.getParticipantId());
            vendorThread.start();
            log.info("Vendor {} thread started automatically after registration", vendor.getParticipantId());
        } catch (Exception e) {
            log.error("Error starting vendor thread {}: {}", vendor.getParticipantId(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to start vendor thread");
        }
    }

    public void deactivateVendor(String vendorId) {
        try {
            log.info("Deactivating vendor: {}", vendorId);
            Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);

            if (vendorOpt.isEmpty()) {
                log.warn("Vendor not found: {}", vendorId);
                throw new IllegalArgumentException("Vendor not found");
            }

            Vendor vendor = vendorOpt.get();
            vendor.setIsActive(false);
            vendor.stopVendor();
            vendorRepository.save(vendor);
            activeVendors.remove(vendorId);
            log.info("Vendor {} successfully deactivated", vendorId);
        } catch (Exception e) {
            log.error("Error deactivating vendor {}: {}", vendorId, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to deactivate vendor");
        }
    }

    public Optional<Vendor> findVendorByName(String name) {
        try {
            log.debug("Searching for vendor with name: {}", name);
            Optional<Vendor> vendor = vendorRepository.findByNameIgnoreCase(name);
            if (vendor.isPresent()) {
                log.info("Found vendor with name {}: {}", name, vendor.get().getName());
            } else {
                log.info("No vendor found with name: {}", name);
            }
            return vendor;
        } catch (Exception e) {
            log.error("Error finding vendor by name {}: {}", name, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find vendor by name");
        }
    }
}