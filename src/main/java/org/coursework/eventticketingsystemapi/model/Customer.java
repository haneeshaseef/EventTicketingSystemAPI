package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.coursework.eventticketingsystemapi.exception.InvalidResourceOperationException;
import org.coursework.eventticketingsystemapi.service.TicketPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "customers")
public class Customer extends Participant {
    private static final Logger log = LoggerFactory.getLogger(Customer.class);
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private int ticketsToPurchase;
    private long ticketRetrievalInterval;
    private int totalTicketsPurchased;

    @Setter
    @JsonIgnore
    @Transient
    private TicketPoolService ticketPoolService;

    private volatile boolean isActive;

    public Customer(String name, String email, String password,int ticketsToPurchase,
                    long ticketRetrievalInterval) {
        super(name, email, password);
        this.ticketsToPurchase = ticketsToPurchase;
        this.ticketRetrievalInterval = ticketRetrievalInterval;
        this.totalTicketsPurchased = 0;
        this.isActive = false;
        log.info("Customer {} initialized with: ticketsToPurchase={}, interval={}s",
                name, ticketsToPurchase, ticketRetrievalInterval);
    }

    private void checkAndUpdateRunningStatus() {
        if (totalTicketsPurchased >= ticketsToPurchase) {
            isActive = false;
            log.info("Customer {} automatically stopped - reached max tickets to purchase: {}", getName(), ticketsToPurchase);
        }
    }

    @Override
    public void run() {
        isActive = true;
        log.info("Customer {} started ticket purchasing process", getName());

        while (isActive) {
            try {
                if (!ticketPoolService.isConfigured()) {
                    log.warn("Customer {} waiting - no active event configuration found. Will retry in {} s.",
                            getName(), ticketRetrievalInterval);
                    Thread.sleep(ticketRetrievalInterval * MILLISECONDS_IN_SECOND);
                    continue;
                }

                int currentAvailable = ticketPoolService.getAvailableTickets().get();
                int remainingTickets = ticketsToPurchase - totalTicketsPurchased;

                log.debug("Customer {} status check: currentAvailable={}, remainingTickets={}, totalPurchased={}",
                        getName(), currentAvailable, remainingTickets, totalTicketsPurchased);

                // Check if the target is already reached
                checkAndUpdateRunningStatus();
                if (!isActive) {
                    log.info("Customer {} reached purchase target: {}", getName(), ticketsToPurchase);
                    break;
                }

                if (currentAvailable > 0 && remainingTickets > 0) {
                    int maxBatchSize = ticketPoolService.getEventConfiguration().getCustomerRetrievalRate();
                    int ticketsToAttempt = Math.min(
                            Math.min(maxBatchSize, currentAvailable),
                            remainingTickets
                    );

                    if (ticketsToAttempt > 0) {
                        log.info("Customer {} attempting to purchase {} tickets", getName(), ticketsToAttempt);
                        try {
                            int purchasedTickets = ticketPoolService.purchaseTickets(this, ticketsToAttempt);

                            if (purchasedTickets > 0) {
                                totalTicketsPurchased += purchasedTickets;
                                log.info("Customer {} purchased {} tickets. Total: {}/{}",
                                        getName(), purchasedTickets, totalTicketsPurchased, ticketsToPurchase);

                                checkAndUpdateRunningStatus();
                                if (!isActive) {
                                    stopCustomer();
                                    break;
                                }
                            }

                            Thread.sleep(ticketRetrievalInterval * MILLISECONDS_IN_SECOND);
                        } catch (InvalidResourceOperationException e) {
                            log.warn("Customer {} purchase attempt failed: {}", getName(), e.getMessage());
                            if (e.getMessage().contains("reached their limit")) {
                                isActive = false;
                                stopCustomer();
                                break;
                            }
                            Thread.sleep(MILLISECONDS_IN_SECOND);
                        }
                    } else {
                        log.info("Customer {} reached purchase limit: totalPurchased={}, targetAmount={}",
                                getName(), totalTicketsPurchased, ticketsToPurchase);
                        isActive = false;
                        stopCustomer();
                        break;
                    }
                } else {
                    if (currentAvailable == 0) {
                        log.warn("Customer {} waiting - no tickets currently available for purchase. Will retry in {} s.",
                                getName(), ticketRetrievalInterval);
                    } else {
                        log.debug("Customer {} waiting - target already reached or no more tickets to purchase", getName());
                    }
                    Thread.sleep(ticketRetrievalInterval * MILLISECONDS_IN_SECOND);
                }
            } catch (InterruptedException e) {
                log.warn("Customer {} thread interrupted during purchase process", getName());
                Thread.currentThread().interrupt();
                isActive = false;
                stopCustomer();
                break;
            } catch (Exception e) {
                log.error("Customer {} encountered an error during ticket purchase: {}", getName(), e.getMessage(), e);
                try {
                    log.debug("Customer {} will retry operation after 1 second delay", getName());
                    Thread.sleep(MILLISECONDS_IN_SECOND);
                } catch (InterruptedException ie) {
                    log.warn("Customer {} interrupted during error recovery", getName());
                    Thread.currentThread().interrupt();
                    isActive = false;
                    stopCustomer();
                    break;
                }
            }
        }

        log.info("Customer {} completed purchase process. Final statistics: purchasedTickets={}, targetAmount={}, completionRate={}%",
                getName(), totalTicketsPurchased, ticketsToPurchase,
                (totalTicketsPurchased * 100 / ticketsToPurchase));
    }

    public void startCustomer() {
        if (totalTicketsPurchased >= ticketsToPurchase) {
            log.warn("Customer {} cannot start - already reached ticket purchase target: {}", getName(), ticketsToPurchase);
            return;
        }
        startParticipant();
        log.info("Customer {} initialized for ticket purchases", getName());
        isActive = true;
    }

    public void stopCustomer() {
        isActive = false;
        stopParticipant();
        log.info("Customer {} stopped. Purchase summary: totalPurchased={}, targetAmount={}, completionRate={}%",
                getName(), totalTicketsPurchased, ticketsToPurchase,
                (totalTicketsPurchased * 100 / ticketsToPurchase));
    }
}