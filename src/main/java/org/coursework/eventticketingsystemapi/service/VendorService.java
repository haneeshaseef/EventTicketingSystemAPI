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

    private final VendorRepository vendorRepository;
    private final TicketPoolService ticketPoolService;

    private final Map<String, Vendor> activeVendors = new ConcurrentHashMap<>();

    /**
     * Constructor for VendorService with dependency injection.
     *
     * @param vendorRepository Repository for vendor data operations
     * @param ticketPoolService Service for managing ticket pools
     */
    @Autowired
    public VendorService(VendorRepository vendorRepository, TicketPoolService ticketPoolService) {
        this.vendorRepository = vendorRepository;
        this.ticketPoolService = ticketPoolService;
    }

    /**
     * Retrieves all vendors from the repository.
     *
     * @return Map of all vendors with their participant IDs as keys
     */
    public Map<String, Vendor> getAllVendors() {
        log.debug("Retrieving all vendors");
        Map<String, Vendor> allVendors = new ConcurrentHashMap<>();
        vendorRepository.findAll().forEach(vendor -> allVendors.put(vendor.getParticipantId(), vendor));
        log.info("Successfully retrieved {} all vendors", allVendors.size());
        return allVendors;
    }

    /**
     * Retrieves all active vendors from the repository and initializes their services.
     *
     * @return Map of active vendors with their participant IDs as keys
     * @throws ResourceProcessingException If there's an error retrieving active vendors
     */
    public Map<String, Vendor> getActiveVendors() {
        try {
            log.debug("Retrieving active vendors");
            vendorRepository.findByIsActive(true).forEach(vendor -> {
                initializeVendorServices(vendor);
                activeVendors.put(vendor.getParticipantId(), vendor);
            });
            log.info("Successfully retrieved {} active vendors", activeVendors.size());
            return activeVendors;
        } catch (Exception e) {
            log.error("Error retrieving active vendors: {}", e.getMessage(), e);
            throw new ResourceProcessingException("Failed to retrieve active vendors");
        }
    }

    /**
     * Registers a new vendor or reactivates an existing vendor.
     *
     * @param vendor Vendor to be registered
     * @return Registered or reactivated vendor
     * @throws IllegalArgumentException If vendor configuration is invalid
     * @throws ResourceProcessingException If registration fails
     */
    public Vendor registerVendor(Vendor vendor) {
        try {
            log.info("Attempting to register vendor: {}", vendor.getName());
            validateVendorConfiguration(vendor);

            Optional<Vendor> existingVendor = vendorRepository.findByEmailIgnoreCase(vendor.getEmail());

            return existingVendor.map(value -> handleExistingVendor(value, vendor)).orElseGet(() -> createNewVendor(vendor));

        } catch (IllegalArgumentException e) {
            log.error("Validation failed for vendor {}: {}", vendor.getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error registering vendor {}: {}", vendor.getName(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to register vendor");
        }
    }

    /**
     * Validates the configuration parameters for a vendor.
     *
     * @param vendor Vendor to validate
     * @throws IllegalArgumentException If any configuration parameter is invalid
     */
    private void validateVendorConfiguration(Vendor vendor) {
        if (vendor.getTicketsPerRelease() <= 0) {
            throw new IllegalArgumentException("Tickets per release must be greater than 0");
        }
        if (vendor.getTicketReleaseInterval() <= 0) {
            throw new IllegalArgumentException("Ticket release interval must be greater than 0");
        }
        if (vendor.getTicketsToSell() <= 0) {
            throw new IllegalArgumentException("Total tickets to sell must be greater than 0");
        }
        if (vendor.getTicketsPerRelease() > vendor.getTicketsToSell()) {
            throw new IllegalArgumentException("Tickets per release cannot exceed total tickets to sell");
        }
    }

    /**
     * Handles registration of an existing vendor by updating their details.
     *
     * @param existingVendor Previously registered vendor
     * @param newDetails New vendor details
     * @return Updated and saved vendor
     * @throws IllegalArgumentException If vendor is already active
     */
    private Vendor handleExistingVendor(Vendor existingVendor, Vendor newDetails) {
        if (existingVendor.isActive()) {
            log.error("Vendor with email {} is already active", existingVendor.getEmail());
            throw new IllegalArgumentException("Vendor with this email is already active");
        }

        existingVendor.setActive(true);
        existingVendor.setTicketsPerRelease(newDetails.getTicketsPerRelease());
        existingVendor.setTicketReleaseInterval(newDetails.getTicketReleaseInterval());
        existingVendor.setTicketsToSell(newDetails.getTicketsToSell());
        existingVendor.setTicketsReleased(0);
        existingVendor.setTotalTicketsSold(0);

        return saveAndStartVendor(existingVendor);
    }

    /**
     * Creates a new vendor with initial configuration.
     *
     * @param vendor Vendor to be created
     * @return Saved and started vendor
     */
    private Vendor createNewVendor(Vendor vendor) {
        vendor.setActive(true);
        vendor.setTicketsReleased(0);
        vendor.setTotalTicketsSold(0);
        return saveAndStartVendor(vendor);
    }

    /**
     * Saves a vendor and starts their thread for ticket sales.
     *
     * @param vendor Vendor to save and start
     * @return Saved vendor
     */
    private Vendor saveAndStartVendor(Vendor vendor) {
        initializeVendorServices(vendor);
        Vendor savedVendor = vendorRepository.save(vendor);
        activeVendors.put(savedVendor.getParticipantId(), savedVendor);
        startVendorThread(savedVendor);
        return savedVendor;
    }

    /**
     * Initializes vendor services for a given vendor.
     *
     * @param vendor Vendor to initialize
     */
    private void initializeVendorServices(Vendor vendor) {
        vendor.setTicketPoolService(ticketPoolService);
    }

    /**
     * Starts a new thread for a vendor to manage ticket sales.
     *
     * @param vendor Vendor whose thread is to be started
     * @throws ResourceProcessingException If thread start fails
     */
    private void startVendorThread(Vendor vendor) {
        try {
            vendor.startVendor();
            Thread vendorThread = new Thread(vendor);
            vendorThread.setName("Vendor-" + vendor.getParticipantId());
            vendorThread.start();
            log.info("Vendor {} thread started successfully", vendor.getParticipantId());
        } catch (Exception e) {
            log.error("Error starting vendor thread {}: {}", vendor.getParticipantId(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to start vendor thread");
        }
    }

    /**
     * Deactivates a vendor by stopping their thread and updating their status.
     *
     * @param vendorId Unique identifier of the vendor to deactivate
     * @throws ResourceProcessingException If deactivation fails
     * @throws IllegalArgumentException If vendor is not found
     */
    public void deactivateVendor(String vendorId) {
        try {
            log.info("Deactivating vendor: {}", vendorId);
            Vendor vendor = getVendorById(vendorId);

            vendor.stopVendor();
            vendor.setActive(false);

            vendorRepository.save(vendor);
            activeVendors.remove(vendorId);

            log.info("Vendor {} successfully deactivated. Final tickets sold: {}",
                    vendorId, vendor.getTotalTicketsSold());
        } catch (Exception e) {
            log.error("Error deactivating vendor {}: {}", vendorId, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to deactivate vendor");
        }
    }

    /**
     * Reactivates a vendor by starting their thread and updating their status.
     *
     * @param vendorId Name of the vendor to reactivate
     * @throws IllegalArgumentException If vendor is not found
     */
    public void reactivateVendor(String vendorId) {
        log.debug("Reactivating vendor: {}", vendorId);
        Vendor vendor = getVendorById(vendorId);
        vendor.setActive(true);
        vendorRepository.save(vendor);
        vendor.setTicketPoolService(ticketPoolService);
        activeVendors.put(vendor.getParticipantId(), vendor);
        startVendorThread(vendor);
        log.info("Successfully reactivated vendor: {}", vendorId);
    }

    /**
     * Deletes a vendor by deactivating them and removing them from the repository.
     *
     * @param vendorName Unique identifier of the vendor to delete
     * @throws ResourceProcessingException If deletion fails
     * @throws IllegalArgumentException If vendor is not found
     */
    public void deleteVendor(String vendorName) {
        try {
            log.info("Deleting vendor: {}", vendorName);
            Vendor vendor = findVendorByName(vendorName)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with name: " + vendorName));
            deactivateVendor(vendor.getParticipantId());
            vendorRepository.delete(vendor);

            log.info("Vendor {} successfully deleted", vendorName);
        } catch (Exception e) {
            log.error("Error deleting vendor {}: {}", vendorName, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to delete vendor");
        }
    }

    /**
     * Retrieves a vendor by their unique identifier.
     *
     * @param vendorId Unique identifier of the vendor
     * @return Vendor with the specified ID
     * @throws IllegalArgumentException If no vendor is found with the given ID
     */
    public Vendor getVendorById(String vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found with ID: " + vendorId));
    }

    /**
     * Finds a vendor by their name (case-insensitive).
     *
     * @param name Name of the vendor to find
     * @return Optional containing the vendor if found
     * @throws ResourceProcessingException If search fails
     */
    public Optional<Vendor> findVendorByName(String name) {
        try {
            log.debug("Searching for vendor with name: {}", name);
            return vendorRepository.findByNameIgnoreCase(name);
        } catch (Exception e) {
            log.error("Error finding vendor by name {}: {}", name, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find vendor by name");
        }
    }

    /**
     * Finds a vendor by their email (case-insensitive).
     *
     * @param email Email of the vendor to find
     * @return Optional containing the vendor if found
     * @throws ResourceProcessingException If search fails
     */
    public Optional<Vendor> findVendorByEmail(String email) {
        try {
            log.debug("Searching for vendor with email: {}", email);
            return vendorRepository.findByEmailIgnoreCase(email);
        } catch (Exception e) {
            log.error("Error finding vendor by email {}: {}", email, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find vendor by email");
        }
    }

    /**
     * Updates the details of a vendor.
     *
     * @param vendorName Name of the vendor to update
     * @param vendor Updated vendor details
     * @return Updated vendor
     * @throws ResourceProcessingException If update fails
     * @throws IllegalArgumentException If vendor is not found
     */
    public Vendor updateVendor(String vendorName, Vendor vendor) {
        try {
            log.debug("Updating vendor: {}", vendorName);
            Vendor existingVendor = findVendorByName(vendorName)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found with name: " + vendorName));

            existingVendor.setEmail(vendor.getEmail());
            existingVendor.setTicketsPerRelease(vendor.getTicketsPerRelease());
            existingVendor.setTicketReleaseInterval(vendor.getTicketReleaseInterval());
            existingVendor.setTicketsToSell(vendor.getTicketsToSell());

            return vendorRepository.save(existingVendor);
        } catch (Exception e) {
            log.error("Error updating vendor {}: {}", vendorName, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to update vendor");
        }
    }

}
