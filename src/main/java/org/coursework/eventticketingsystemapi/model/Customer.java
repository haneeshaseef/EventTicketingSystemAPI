package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.coursework.eventticketingsystemapi.service.TicketPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = "purchasedTickets")
@ToString(exclude = "purchasedTickets")
@Document(collection = "customers")
public class Customer extends Participant {
    private static final Logger log = LoggerFactory.getLogger(Customer.class);

    @DBRef(lazy = true)
    @JsonIgnore
    private List<Ticket> purchasedTickets = new ArrayList<>();

    private int ticketsToPurchase;
    private int ticketRetrievalInterval;
    private final AtomicInteger ticketsPurchased = new AtomicInteger(0);

    @JsonIgnore
    @Autowired
    private TicketPoolService ticketPoolService;

    public Customer(String name, String email, Boolean isActive, int ticketsToPurchase, int ticketRetrievalInterval) {
        super(name, email, isActive);
        this.ticketsToPurchase = ticketsToPurchase;
        this.ticketRetrievalInterval = ticketRetrievalInterval;
    }

    @Override
    public void run() {
        log.info("Customer {} started ticket purchasing process with target: {} tickets",
                getName(), ticketsToPurchase);

        while (isActive && !Thread.currentThread().isInterrupted()) {
            try {
                // Check purchase limit
                if (ticketsPurchased.get() >= ticketsToPurchase) {
                    log.info("Customer {} reached ticket limit of {}. Stopping purchase process.",
                            getName(), ticketsToPurchase);
                    stopCustomer();
                    break;
                }

                // Verify event configuration
                if (!ticketPoolService.isConfigured()) {
                    log.warn("Customer {} waiting - Event not configured", getName());
                    Thread.sleep(ticketRetrievalInterval);
                    continue;
                }

                // Check available tickets
                if (ticketPoolService.getAvailableTickets() <= 0) {
                    log.info("Customer {} waiting - No tickets available", getName());
                    Thread.sleep(ticketRetrievalInterval);
                    continue;
                }

                // Attempt purchase
                boolean purchased = ticketPoolService.purchaseTickets(this);
                if (purchased) {
                    int currentPurchased = ticketsPurchased.incrementAndGet();
                    log.info("Customer {} successfully purchased ticket {}/{}",
                            getName(), currentPurchased, ticketsToPurchase);
                }

                // Wait before next attempt
                Thread.sleep(ticketRetrievalInterval);

            } catch (InterruptedException e) {
                log.info("Customer {} purchase process interrupted", getName());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Customer {} encountered error during purchase: {}",
                        getName(), e.getMessage(), e);
                try {
                    Thread.sleep(1000); // Brief pause on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("Customer {} finished purchasing process. Final count: {}/{}",
                getName(), ticketsPurchased.get(), ticketsToPurchase);
    }

    public void startCustomer() {
        startParticipant();
        log.info("Customer {} started with target of {} tickets", getName(), ticketsToPurchase);
    }

    public void stopCustomer() {
        stopParticipant();
        log.info("Customer {} stopped. Final ticket count: {}/{}",
                getName(), ticketsPurchased.get(), ticketsToPurchase);
    }

    public void reset() {
        this.purchasedTickets.clear();
        this.ticketsPurchased.set(0);
        log.info("Customer {} reset. Purchase history cleared.", getName());
    }

    public List<Ticket> getPurchasedTickets() {
        return new ArrayList<>(purchasedTickets);
    }

    public void addPurchasedTicket(Ticket ticket) {
        if (ticket != null) {
            purchasedTickets.add(ticket);
            log.debug("Added ticket {} to customer {}'s purchases",
                    ticket.getTicketId(), getName());
        }
    }

    public int getTicketsPurchasedCount() {
        return ticketsPurchased.get();
    }
}