package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.coursework.eventticketingsystemapi.service.TicketPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "vendors")
public class Vendor extends Participant implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Vendor.class);

    private int ticketsPerRelease;
    private int ticketReleaseInterval;
    private int ticketsToSell;
    private int totalTicketsSold;

    @Setter
    @JsonIgnore
    @Transient
    private TicketPoolService ticketPoolService;

    private volatile boolean isRunning;

    public Vendor(String name, String email, Boolean isActive, int ticketsPerRelease,
                  int ticketReleaseInterval, int ticketsToSell) {
        super(name, email, isActive);
        this.ticketsPerRelease = ticketsPerRelease;
        this.ticketReleaseInterval = ticketReleaseInterval;
        this.ticketsToSell = ticketsToSell;
        this.totalTicketsSold = 0;
        this.isRunning = false;
        log.info("Vendor {} initialized with: ticketsPerRelease={}, interval={}, maxTickets={}",
                name, ticketsPerRelease, ticketReleaseInterval, ticketsToSell);
    }

    @Override
    public void run() {
        isRunning = true;
        log.info("Vendor {} started ticket release process", getName());

        while (isRunning && isActive && totalTicketsSold < ticketsToSell) {
            try {
                if (!ticketPoolService.isConfigured()) {
                    log.debug("Vendor {} waiting - no active event configuration found. Will retry in {} ms",
                            getName(), ticketReleaseInterval);
                    Thread.sleep(ticketReleaseInterval);
                    continue;
                }

                int currentAvailable = ticketPoolService.getAvailableTickets();
                int maxCapacity = ticketPoolService.getEventConfiguration().getMaxCapacity();

                log.debug("Vendor {} status check: currentAvailable={}, maxCapacity={}, totalTicketsSold={}",
                        getName(), currentAvailable, maxCapacity, totalTicketsSold);

                if (currentAvailable < maxCapacity) {
                    int remainingTickets = ticketsToSell - totalTicketsSold;
                    int ticketsToAdd = Math.min(
                            Math.min(ticketsPerRelease, maxCapacity - currentAvailable),
                            remainingTickets
                    );

                    if (ticketsToAdd > 0) {
                        log.info("Vendor {} attempting to add {} tickets", getName(), ticketsToAdd);
                        ticketPoolService.addTickets(this, ticketsToAdd);
                        totalTicketsSold += ticketsToAdd;
                        log.info("Vendor {} successfully added {} tickets, new total={}, remaining capacity={}",
                                getName(), ticketsToAdd, totalTicketsSold,
                                maxCapacity - (currentAvailable + ticketsToAdd));
                        Thread.sleep(ticketReleaseInterval);
                    } else {
                        log.info("Vendor {} reached limit: currentTotal={}, maxLimit={}",
                                getName(), totalTicketsSold, ticketsToSell);
                        stopVendor(); // Stop the vendor thread
                        break; // Exit the loop
                    }
                } else {
                    log.debug("Vendor {} waiting - pool at capacity: current={}, max={}", getName(), currentAvailable, maxCapacity);
                    Thread.sleep(ticketReleaseInterval);
                }
            } catch (InterruptedException e) {
                log.warn("Vendor {} thread interrupted during ticket release process", getName());
                Thread.currentThread().interrupt();
                stopVendor(); // Stop the vendor thread
                break; // Exit the loop
            } catch (Exception e) {
                log.error("Vendor {} encountered an error during ticket release: {}", getName(), e.getMessage(), e);
                try {
                    log.debug("Vendor {} will retry operation after 1 second delay", getName());
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    log.warn("Vendor {} interrupted during error recovery", getName());
                    Thread.currentThread().interrupt();
                    stopVendor(); // Stop the vendor thread
                    break; // Exit the loop
                }
            }
        }

        try {
            ticketPoolService.updateVendorTicketCount(this, 0); // Update final state
        } catch (Exception e) {
            log.error("Failed to update final vendor state in database: {}", e.getMessage());
        }

        log.info("Vendor {} completed ticket release process. Final statistics: addedTickets={}, target={}, completion={}%",
                getName(), totalTicketsSold, ticketsToSell,
                (totalTicketsSold * 100.0 / ticketsToSell));
    }

    public void startVendor() {
        startParticipant();
        log.info("Vendor {} initialized for ticket sales", getName());
        isRunning = true;
    }

    public void stopVendor() {
        isRunning = false;
        stopParticipant();
        log.info("Vendor {} stopped. Sales summary: totalSold={}, targetAmount={}, completionRate={}%",
                getName(), totalTicketsSold, ticketsToSell,
                (totalTicketsSold * 100.0 / ticketsToSell));
    }
}