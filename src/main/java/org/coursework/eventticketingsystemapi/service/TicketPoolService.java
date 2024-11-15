package org.coursework.eventticketingsystemapi.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.model.EventConfiguration;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.repository.VendorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class TicketPoolService {
    private static final Logger log = LoggerFactory.getLogger(TicketPoolService.class);
    private final Map<Vendor, AtomicInteger> vendorCurrentAvailableCounts;
    private final AtomicInteger availableTickets;
    private final ReentrantLock lock;

    @Getter
    private EventConfiguration eventConfiguration;

    @Getter
    private volatile boolean isConfigured;

    @Autowired
    private EventConfigurationService configurationService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private VendorRepository vendorRepository;

    public TicketPoolService() {
        this.vendorCurrentAvailableCounts = new ConcurrentHashMap<>();
        this.availableTickets = new AtomicInteger(0);
        this.lock = new ReentrantLock();
        this.isConfigured = false;
    }

    @PostConstruct
    public void init() {
        loadConfiguration();
    }

    @PreDestroy
    public void shutdown() {
        lock.lock();
        try {
            if (isConfigured && eventConfiguration != null) {
                eventConfiguration.setTotalTickets(availableTickets.get());
                configurationService.saveConfiguration(eventConfiguration);
                log.info("Updated configuration with {} available tickets before shutdown", availableTickets.get());
            }
        } catch (ResourceProcessingException e) {
            log.error("Failed to save configuration during shutdown: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

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

    public void configureEvent(EventConfiguration config) {
        if (config == null) {
            log.error("Cannot configure null event");
            isConfigured = false;
            return;
        }

        lock.lock();
        try {
            if (!isValidConfiguration(config)) {
                log.error("Invalid event configuration provided");
                isConfigured = false;
                return;
            }

            this.eventConfiguration = config;
            this.vendorCurrentAvailableCounts.clear();

            // Load existing vendors and their capabilities from database
            List<Vendor> vendors = vendorRepository.findByIsActive(true);
            for (Vendor vendor : vendors) {
                vendorCurrentAvailableCounts.put(vendor, new AtomicInteger(0));
            }

            this.availableTickets.set(config.getTotalTickets());
            this.isConfigured = true;

            log.info("Event configured successfully with {} total tickets and {} vendors",
                    availableTickets.get(), vendors.size());
        } finally {
            lock.unlock();
        }
    }

    public void updateVendorTicketCount(Vendor vendor, int addedTickets) {
        lock.lock();
        try {
            vendorCurrentAvailableCounts.computeIfPresent(vendor, (k, v) -> {
                v.addAndGet(addedTickets);
                return v;
            });
            log.debug("Updated vendor {} ticket count, added {} tickets",
                    vendor.getName(), addedTickets);
        } finally {
            lock.unlock();
        }
    }

    private boolean isValidConfiguration(EventConfiguration config) {
        return config.getTotalTickets() >= 0 && config.getMaxCapacity() > 0 && config.getTicketReleaseRate() > 0 && config.getCustomerRetrievalRate() > 0 && config.getEventName() != null && !config.getEventName().trim().isEmpty();
    }

    public void addTickets(Vendor vendor, int count) {
        lock.lock();
        try {
            if (!isConfigured || count <= 0 || vendor == null) {
                log.error("Cannot add tickets: Invalid state, count, or vendor");
                return;
            }

            int currentAvailable = getVendorAvailableTickets(vendor);
            int newTotal = currentAvailable + count;

            if (newTotal > vendor.getTicketsToSell()) {
                log.warn("Cannot add {} tickets: would exceed maximum tickets of {} for vendor {}",
                        count, vendor.getTicketsToSell(), vendor.getName());
                return;
            }

            if (newTotal > eventConfiguration.getMaxCapacity()) {
                log.warn("Cannot add {} tickets: would exceed maximum capacity of {}",
                        count, eventConfiguration.getMaxCapacity());
                return;
            }

            // Update the vendor's available ticket count
            vendorCurrentAvailableCounts.compute(vendor, (k, v) -> {
                if (v == null) {
                    log.debug("Initializing ticket count for vendor {}", vendor.getName());
                    return new AtomicInteger(count);
                } else {
                    log.debug("Incrementing ticket count for vendor {} by {}", vendor.getName(), count);
                    v.addAndGet(count);
                    return v;
                }
            });

            // Update the total available tickets
            int previousTotal = availableTickets.get();
            availableTickets.addAndGet(count);
            log.debug("Total available tickets updated from {} to {}", previousTotal, availableTickets.get());

            // Update the event configuration
            eventConfiguration.setTotalTickets(availableTickets.get());
            configurationService.saveConfiguration(eventConfiguration);
            log.debug("Event configuration updated with new total tickets: {}", availableTickets.get());

            log.info("Successfully added {} tickets for vendor {}", count, vendor.getName());
            log.debug("Current vendor {} available tickets: {}",
                    vendor.getName(), getVendorAvailableTickets(vendor));
            log.debug("Event total available tickets: {}", availableTickets.get());

        } catch (Exception e) {
            log.error("Failed to add tickets for vendor {}: {}", vendor.getName(), e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public boolean purchaseTickets(Customer customer) {
        lock.lock();
        try {
            if (!isConfigured || customer == null) {
                log.error("Cannot process purchase: Invalid configuration or null customer");
                return false;
            }

            // Check if there are available tickets
            if (availableTickets.get() <= 0) {
                log.warn("No available tickets in the pool");
                return false;
            }

            // Find the vendor with available tickets
            for (Map.Entry<Vendor, AtomicInteger> entry : vendorCurrentAvailableCounts.entrySet()) {
                Vendor vendor = entry.getKey();
                AtomicInteger availableCount = entry.getValue();

                if (availableCount != null && availableCount.get() > 0) {
                    // Try to decrement available count
                    if (availableCount.decrementAndGet() < 0) {
                        availableCount.incrementAndGet();
                        log.debug("No available tickets for vendor: {}", vendor.getName());
                        continue;
                    }

                    // Decrement total available tickets
                    availableTickets.decrementAndGet();

                    try {
                        // Create new ticket
                        Ticket ticket = new Ticket();
                        ticket.setEventName(eventConfiguration.getEventName());
                        ticket.setVendor(vendor);
                        ticket.setCustomer(customer);
                        ticket.setCreatedAt(LocalDateTime.now());

                        // First save the initial ticket
                        ticketService.saveTicket(ticket);

                        // Update event configuration
                        eventConfiguration.setTotalTickets(availableTickets.get());
                        configurationService.saveConfiguration(eventConfiguration);

                        log.info("Successfully processed ticket purchase for customer {} from vendor {}",
                                customer.getName(), vendor.getName());

                        return true;

                    } catch (Exception e) {
                        // Rollback available count change if ticket creation/purchase fails
                        availableCount.incrementAndGet();
                        availableTickets.incrementAndGet();

                        log.error("Failed to process ticket purchase: {}", e.getMessage(), e);
                        return false;
                    }
                }
            }

            log.warn("No available tickets from any vendor");
            return false;

        } finally {
            lock.unlock();
        }
    }

    public int getAvailableTickets() {
        return availableTickets.get();
    }

    private int getVendorAvailableTickets(Vendor vendor) {
        if (vendor == null) return 0;
        AtomicInteger count = vendorCurrentAvailableCounts.get(vendor);
        return count != null ? count.get() : 0;
    }
}