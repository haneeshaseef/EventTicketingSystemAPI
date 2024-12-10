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
public class Vendor extends Participant{
    private static final Logger log = LoggerFactory.getLogger(Vendor.class);
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private int ticketsPerRelease;
    private long ticketReleaseInterval;
    private int ticketsToSell;
    private int ticketsReleased;
    private int totalTicketsSold;

    @Setter
    @JsonIgnore
    @Transient
    private TicketPoolService ticketPoolService;

    private volatile boolean isActive;

    public Vendor(String name, String email,String password, int ticketsPerRelease,
                  int ticketReleaseInterval, int ticketsToSell) {
        super(name, email, password);
        this.ticketsPerRelease = ticketsPerRelease;
        this.ticketReleaseInterval = ticketReleaseInterval;
        this.ticketsToSell = ticketsToSell;
        this.totalTicketsSold = 0;
        this.ticketsReleased = 0;
        this.isActive = false;
        log.info("Vendor {} initialized with: ticketsPerRelease={}, interval={}s, maxTickets={}",
                name, ticketsPerRelease, ticketReleaseInterval, ticketsToSell);
    }

    private void checkAndUpdateRunningStatus() {
        if (ticketsReleased >= ticketsToSell) {
            isActive = false;
            log.info("Vendor {} automatically stopped - reached max tickets to sell: {}", getName(), ticketsToSell);
        }
    }

    @Override
    public void run() {
        isActive = true;
        log.info("Vendor {} started ticket release process", getName());

        while (isActive) {
            try {
                if (!ticketPoolService.isConfigured()) {
                    log.warn("Vendor {} waiting - no active event configuration found. Will retry in {} s.Please configure the event first",
                            getName(), ticketReleaseInterval);
                    Thread.sleep(ticketReleaseInterval * MILLISECONDS_IN_SECOND);
                    continue;
                }

                int currentAvailable = ticketPoolService.getAvailableTickets().get();
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
                        checkAndUpdateRunningStatus(); // Check if we've reached max tickets
                        log.info("Vendor {} successfully added {} tickets, new total={}, remaining capacity={}",
                                getName(), ticketsToAdd, totalTicketsSold,
                                maxCapacity - (currentAvailable + ticketsToAdd));

                        if (!isActive) {
                            log.info("Vendor {} reached max tickets to sell. Stopping.", getName());
                            stopVendor();
                            break;
                        }

                        Thread.sleep(ticketReleaseInterval * MILLISECONDS_IN_SECOND);
                    } else {
                        log.info("Vendor {} reached limit: currentTotal={}, maxLimit={}",
                                getName(), totalTicketsSold, ticketsToSell);
                        stopVendor();
                        break;
                    }
                } else {
                    log.debug("Vendor {} waiting - pool at capacity: current={}, max={}", getName(), currentAvailable, maxCapacity);
                    Thread.sleep(ticketReleaseInterval * MILLISECONDS_IN_SECOND);
                }
            } catch (InterruptedException e) {
                log.warn("Vendor {} thread interrupted during ticket release process", getName());
                Thread.currentThread().interrupt();
                stopVendor();
                break;
            } catch (Exception e) {
                log.error("Vendor {} encountered an error during ticket release: {}", getName(), e.getMessage(), e);
                try {
                    log.debug("Vendor {} will retry operation after 1 second delay", getName());
                    Thread.sleep(MILLISECONDS_IN_SECOND);
                } catch (InterruptedException ie) {
                    log.warn("Vendor {} interrupted during error recovery", getName());
                    Thread.currentThread().interrupt();
                    stopVendor();
                    break;
                }
            }
        }

        try {
            ticketPoolService.updateVendorTicketCount(this, 0);
        } catch (Exception e) {
            log.error("Failed to update final vendor state in database: {}", e.getMessage());
        }

        log.info("Vendor {} completed ticket release process. Final statistics: addedTickets={}, target={}, completion={}%",
                getName(), totalTicketsSold, ticketsToSell,
                (totalTicketsSold * 100 / ticketsToSell));
    }

    public void startVendor() {
        if (totalTicketsSold >= ticketsToSell) {
            log.warn("Vendor {} cannot start - already reached max tickets to sell: {}", getName(), ticketsToSell);
            return;
        }
        startParticipant();
        log.info("Vendor {} initialized for ticket sales", getName());
        isActive = true;
    }

    public void stopVendor() {
        isActive = false;
        stopParticipant();
        log.info("Vendor {} stopped. Sales summary: totalSold={}, targetAmount={}, completionRate={}%",
                getName(), totalTicketsSold, ticketsToSell,
                (totalTicketsSold * 100 / ticketsToSell));
    }
}
