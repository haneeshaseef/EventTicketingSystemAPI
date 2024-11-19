package org.coursework.eventticketingsystemapi.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.coursework.eventticketingsystemapi.exception.EventTicketingSystemException;
import org.coursework.eventticketingsystemapi.exception.InvalidResourceOperationException;
import org.coursework.eventticketingsystemapi.exception.ResourceNotFoundException;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.repository.CustomerRepository;
import org.coursework.eventticketingsystemapi.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class TicketPoolService {
    private static final Logger log = LoggerFactory.getLogger(TicketPoolService.class);
    private final Map<String, AtomicInteger> vendorCurrentAvailableCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> customerRemainingTickets = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> vendorSoldTicketCounts = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private final AtomicInteger availableTickets;
    private final EventConfigurationService configurationService;
    private final TicketService ticketService;
    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;
    @Getter
    private EventConfiguration eventConfiguration;
    @Getter
    private volatile boolean isConfigured;

    @Autowired
    public TicketPoolService(EventConfigurationService configurationService, TicketService ticketService, VendorRepository vendorRepository, CustomerRepository customerRepository) {
        this.configurationService = configurationService;
        this.ticketService = ticketService;
        this.vendorRepository = vendorRepository;
        this.customerRepository = customerRepository;
        this.availableTickets = new AtomicInteger(0);
    }

    @PostConstruct
    private void loadConfiguration() {
        lock.lock();
        try {
            eventConfiguration = configurationService.getEventConfiguration();
            if (eventConfiguration != null) {
                configureEvent(eventConfiguration);
                isConfigured = true;
            } else {
                log.error("No configuration found or error loading configuration");
                isConfigured = false;
            }
        } catch (Exception e) {
            log.error("Configuration load failed: {}", e.getMessage());
            isConfigured = false;
        } finally {
            lock.unlock();
        }
    }

    private void loadExistingParticipants() {
        lock.lock();
        try {
            // Clear existing maps
            vendorCurrentAvailableCounts.clear();
            vendorSoldTicketCounts.clear();
            customerRemainingTickets.clear();

            // Load all active vendors
            List<Vendor> activeVendors = vendorRepository.findByIsActive(true);

            for (Vendor vendor : activeVendors) {
                if (vendor.isActive()) {
                    String vendorId = vendor.getParticipantId();
                    // Calculate remaining tickets to sell
                    int soldTickets = ticketService.countTicketsSoldByVendor(vendor);
                    int remainingTickets = vendor.getTicketsToSell() - soldTickets;

                    // Initialize vendor counts
                    vendorCurrentAvailableCounts.put(vendorId, new AtomicInteger(Math.max(0, remainingTickets)));
                    vendorSoldTicketCounts.put(vendorId, new AtomicInteger(soldTickets));

                    log.info("Loaded vendor {} with {} available tickets and {} sold tickets", vendor.getName(), remainingTickets, soldTickets);
                }
            }

            // Load customers and their remaining tickets to purchase
            List<Customer> customers = customerRepository.findByIsActive(true);
            for (Customer customer : customers) {
                int ticketsPurchased = customer.getTotalTicketsPurchased();
                int remainingTickets = Math.max(0, customer.getTicketsToPurchase() - ticketsPurchased);
                customerRemainingTickets.put(customer.getParticipantId(), new AtomicInteger(remainingTickets));

                log.info("Loaded customer {} with {} remaining tickets to purchase", customer.getName(), remainingTickets);
            }
        } catch (Exception e) {
            log.error("Failed to load vendors and customers with available tickets: {}", e.getMessage());
            throw new ResourceProcessingException("Failed to initialize vendor and customer ticket counts");
        } finally {
            lock.unlock();
        }
    }

    public void synchronizeAvailableTickets() {
        lock.lock();
        try {
            int totalAvailable = 0;
            boolean hasRunningVendors = false;

            for (Map.Entry<String, AtomicInteger> entry : vendorCurrentAvailableCounts.entrySet()) {
                String vendorId = entry.getKey();
                Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);

                if (vendorOpt.isPresent()) {
                    Vendor vendor = vendorOpt.get();
                    if (vendor.isActive()) {
                        hasRunningVendors = true;
                        int vendorAvailable = entry.getValue().get();
                        totalAvailable += vendorAvailable;

                        // Update sold tickets count
                        int soldTickets = vendorSoldTicketCounts.getOrDefault(vendorId, new AtomicInteger(0)).get();
                        vendor.setTotalTicketsSold(soldTickets);
                        vendorRepository.save(vendor);
                    }
                }
            }

            if (!hasRunningVendors) {
                log.warn("No running vendors found, setting available tickets to 0");
            }

            availableTickets.set(totalAvailable);

            if (isConfigured && eventConfiguration != null) {
                eventConfiguration.setTotalTickets(totalAvailable);
                configurationService.saveConfiguration(eventConfiguration);
                log.info("Synchronized ticket counts - Total available: {}", totalAvailable);
            }
        } catch (Exception e) {
            log.error("Failed to synchronize ticket counts: {}", e.getMessage());
            throw new ResourceProcessingException("Failed to synchronize ticket counts");
        } finally {
            lock.unlock();
        }
    }

    public void configureEvent(EventConfiguration config) {
        if (config == null) {
            log.error("Cannot configure null event");
            isConfigured = false;
            throw new InvalidResourceOperationException("Provided event configuration is null");
        }

        lock.lock();
        try {
            if (!isValidConfiguration(config)) {
                log.error("Invalid event configuration provided");
                isConfigured = false;
                throw new InvalidResourceOperationException("Invalid event configuration provided");
            }

            this.eventConfiguration = config;
            this.vendorCurrentAvailableCounts.clear();
            this.vendorSoldTicketCounts.clear();

            // Load existing vendors and initialize their counts
            loadExistingParticipants();

            synchronizeAvailableTickets();
            this.isConfigured = true;

            log.info("Event configured successfully with {} total tickets and {} active vendors", availableTickets.get(), vendorCurrentAvailableCounts.size());
        } finally {
            lock.unlock();
        }
    }

    private boolean isValidConfiguration(EventConfiguration config) {
        return config.getTotalTickets() >= 0 && config.getMaxCapacity() > 0 && config.getTicketReleaseRate() > 0 && config.getCustomerRetrievalRate() > 0 && config.getEventName() != null && !config.getEventName().trim().isEmpty();
    }

    public void addTickets(Vendor vendor, int count) {
        if (!isConfigured || count <= 0 || vendor == null) {
            log.error("Cannot release tickets: Invalid state, count, or vendor");
            throw new IllegalStateException("Cannot release tickets in the current state");
        }

        lock.lock();
        try {
            String vendorId = vendor.getParticipantId();
            Vendor updatedVendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found in database"));

            // Validate ticket release
            int currentSold = vendorSoldTicketCounts.getOrDefault(vendorId, new AtomicInteger(0)).get();
            int currentAvailable = getVendorAvailableTickets(vendorId);
            int totalAfterRelease = currentSold + currentAvailable + count;

            if (totalAfterRelease > updatedVendor.getTicketsToSell()) {
                throw new InvalidResourceOperationException(
                        String.format("Cannot release %d tickets: would exceed vendor's maximum of %d",
                                count, updatedVendor.getTicketsToSell()));
            }

            if (availableTickets.get() + count > eventConfiguration.getMaxCapacity()) {
                throw new InvalidResourceOperationException(
                        String.format("Cannot release %d tickets: would exceed maximum capacity of %d",
                                count, eventConfiguration.getMaxCapacity()));
            }

            // Atomic ticket addition
            vendorCurrentAvailableCounts.compute(vendorId, (k, v) ->
                    v == null ? new AtomicInteger(count) : new AtomicInteger(v.get() + count));
            availableTickets.addAndGet(count);

            // Update vendor
            updatedVendor.setTicketsReleased(updatedVendor.getTicketsReleased() + count);
            updatedVendor.setActive(true);

            if (updatedVendor.getTotalTicketsSold() >= updatedVendor.getTicketsToSell()) {
                updatedVendor.setActive(false);
            }

            try {
                vendorRepository.save(updatedVendor);

                // Update configuration
                eventConfiguration.setTotalTickets(availableTickets.get());
                configurationService.saveConfiguration(eventConfiguration);

                log.info("Successfully released {} tickets for vendor {}. Total released: {}",
                        count, updatedVendor.getName(), updatedVendor.getTicketsReleased());
                log.debug("Current vendor {} available tickets: {}", updatedVendor.getName(), getVendorAvailableTickets(vendorId));
                log.debug("Total available tickets in event: {}", availableTickets.get());

            } catch (Exception e) {
                log.error("Failed to update vendor in database: {}", e.getMessage());
                throw new ResourceProcessingException("Failed to update vendor record");
            }
        } finally {
            lock.unlock();
        }
    }

    public int purchaseTickets(Customer customer, int count) {
        if (!isConfigured || customer == null || count <= 0) {
            log.error("Cannot process purchase: system not configured, invalid customer, or invalid count");
            throw new IllegalStateException("Cannot process purchase in current state");
        }

        lock.lock();
        try {
            Customer updatedCustomer = customerRepository.findById(customer.getParticipantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found in database"));

            // Calculate actual purchase count
            int currentPurchased = updatedCustomer.getTotalTicketsPurchased();
            int remainingAllowedPurchases = updatedCustomer.getTicketsToPurchase() - currentPurchased;

            if (remainingAllowedPurchases <= 0) {
                throw new InvalidResourceOperationException("Cannot purchase tickets, customer has reached their limit");
            }

            int actualPurchaseCount = Math.min(Math.min(count, remainingAllowedPurchases), availableTickets.get());
            if (actualPurchaseCount <= 0) {
                return 0;
            }

            // Prepare for batch operations
            List<Ticket> ticketsToSave = new ArrayList<>();
            Map<String, Integer> vendorPurchaseCounts = new HashMap<>();
            int totalPurchased = 0;

            // Get eligible vendors and sort by availability
            List<String> vendorIds = vendorCurrentAvailableCounts.entrySet().stream()
                    .filter(entry -> entry.getValue().get() > 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            Map<String, Vendor> vendorMap = vendorRepository.findAllById(vendorIds).stream()
                    .filter(Vendor::isActive)
                    .collect(Collectors.toMap(Vendor::getParticipantId, v -> v));

            LocalDateTime purchaseTime = LocalDateTime.now();

            // Process purchase from each vendor
            for (Vendor vendor : vendorMap.values()) {
                if (totalPurchased >= actualPurchaseCount) break;

                AtomicInteger availableCount = vendorCurrentAvailableCounts.get(vendor.getParticipantId());
                int vendorAvailable = availableCount.get();
                int purchaseFromVendor = Math.min(actualPurchaseCount - totalPurchased, vendorAvailable);

                if (purchaseFromVendor <= 0) continue;

                // Create tickets
                for (int i = 0; i < purchaseFromVendor; i++) {
                    ticketsToSave.add(new Ticket(vendor, updatedCustomer, eventConfiguration.getEventName()));
                }

                // Update counts atomically
                if (availableCount.addAndGet(-purchaseFromVendor) >= 0) {
                    availableTickets.addAndGet(-purchaseFromVendor);
                    vendorSoldTicketCounts.computeIfAbsent(vendor.getParticipantId(), k -> new AtomicInteger(0))
                            .addAndGet(purchaseFromVendor);

                    vendorPurchaseCounts.put(vendor.getParticipantId(), purchaseFromVendor);
                    vendor.setTotalTicketsSold(vendor.getTotalTicketsSold() + purchaseFromVendor);
                    vendor.setActive(vendor.getTotalTicketsSold() < vendor.getTicketsToSell());

                    totalPurchased += purchaseFromVendor;
                }
            }

            // Save all changes if any tickets were purchased
            if (totalPurchased > 0) {
                try {
                    // Batch save tickets
                    ticketService.saveTickets(ticketsToSave);

                    // Update vendors
                    List<Vendor> updatedVendors = vendorPurchaseCounts.keySet().stream()
                            .map(vendorMap::get)
                            .collect(Collectors.toList());
                    vendorRepository.saveAll(updatedVendors);

                    // Update customer
                    updatedCustomer.setTotalTicketsPurchased(updatedCustomer.getTotalTicketsPurchased() + totalPurchased);
                    customerRepository.save(updatedCustomer);

                    // Update configuration
                    eventConfiguration.setTotalTickets(availableTickets.get());
                    configurationService.saveConfiguration(eventConfiguration);

                    log.info("Batch ticket purchase successful - Customer: {} ({}/{}), Count: {}, Total Available: {}",
                            updatedCustomer.getName(),
                            updatedCustomer.getTotalTicketsPurchased(),
                            updatedCustomer.getTicketsToPurchase(),
                            totalPurchased,
                            availableTickets.get());

                } catch (Exception e) {
                    log.error("Failed to process batch ticket purchase: {}", e.getMessage());
                    throw new ResourceProcessingException("Failed to process ticket purchase: " + e.getMessage());
                }
            }

            return totalPurchased;

        } finally {
            lock.unlock();
        }
    }

    public void updateVendorTicketCount(Vendor vendor, int addedTickets) {
        lock.lock();
        try {
            String vendorId = vendor.getParticipantId();
            vendorCurrentAvailableCounts.computeIfPresent(vendorId, (k, v) -> {
                v.addAndGet(addedTickets);
                return v;
            });
            log.debug("Updated vendor {} ticket count, added {} tickets", vendor.getName(), addedTickets);
        } finally {
            lock.unlock();
        }
    }

    private int getVendorAvailableTickets(String vendorId) {
        if (vendorId == null) return 0;
        AtomicInteger count = vendorCurrentAvailableCounts.get(vendorId);
        return count != null ? count.get() : 0;
    }

    @PreDestroy
    public void shutdown() {
        lock.lock();
        try {
            if (isConfigured && eventConfiguration != null) {
                // Update sold ticket counts and running states for all vendors
                vendorCurrentAvailableCounts.forEach((vendorId, availableCount) -> {
                    try {
                        Optional<Vendor> vendorOpt = vendorRepository.findById(vendorId);
                        if (vendorOpt.isPresent()) {
                            Vendor vendor = vendorOpt.get();
                            // Get final counts
                            int finalAvailable = availableCount.get();
                            int finalSold = vendorSoldTicketCounts.getOrDefault(vendorId, new AtomicInteger(0)).get();

                            // Update vendor state
                            vendor.setTotalTicketsSold(finalSold);
                            vendor.setActive(finalAvailable > 0);

                            // Save vendor state
                            vendorRepository.save(vendor);

                            log.info("Shutdown: Updated vendor {} - Sold: {}, Available: {}, Running: {}", vendor.getName(), finalSold, finalAvailable, vendor.isActive());
                        }
                    } catch (Exception e) {
                        log.error("Failed to update vendor {} during shutdown: {}", vendorId, e.getMessage());
                    }
                });

                // Optional: Persist customer remaining tickets during shutdown
                customerRemainingTickets.forEach((customerId, remainingTicketsCount) -> {
                    try {
                        Optional<Customer> customerOpt = customerRepository.findById(customerId);
                        if (customerOpt.isPresent()) {
                            Customer customer = customerOpt.get();
                            int finalRemainingTickets = remainingTicketsCount.get();

                            log.info("Shutdown: Customer {} - Remaining tickets: {}", customer.getName(), finalRemainingTickets);
                            // Additional logic if needed to persist remaining tickets
                        }
                    } catch (Exception e) {
                        log.error("Failed to process customer {} during shutdown: {}", customerId, e.getMessage());
                    }
                });

                synchronizeAvailableTickets();
                configurationService.saveConfiguration(eventConfiguration);

                log.info("Shutdown completed successfully. Final available tickets: {}", availableTickets.get());
            }
        } finally {
            lock.unlock();
        }
    }
}