package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceNotFoundException;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.model.Vendor;
import org.coursework.eventticketingsystemapi.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;

    /**
     * Constructor for TicketService with dependency injection.
     *
     * @param ticketRepository Repository for ticket data operations
     */
    @Autowired
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Counts the total number of tickets sold by a specific vendor.
     *
     * @param vendor the vendor to count tickets for
     * @return the number of tickets sold by the vendor
     * @throws ResourceProcessingException if there is an error counting the tickets
     */
    public int countTicketsSoldByVendor(Vendor vendor) {
        try {
            if (vendor == null) {
                throw new IllegalArgumentException("Vendor cannot be null");
            }
            log.debug("Counting tickets sold by vendor: {}", vendor.getName());
            return ticketRepository.countByVendorParticipantId(vendor.getParticipantId());
        } catch (Exception e) {
            log.error("Error counting tickets for vendor: {}", vendor.getName(), e);
            throw new ResourceProcessingException("Failed to count vendor tickets: " + e.getMessage());
        }
    }

    /**
     * Retrieves all active tickets and returns them as a map.
     *
     * @return a map of all active tickets
     * @throws ResourceProcessingException if there is an error retrieving the tickets
     */
    public Map<String, Ticket> getActiveTickets() {
        try {
            log.info("Retrieving all active tickets");
            List<Ticket> tickets = ticketRepository.findAll();
            return tickets.stream().collect(Collectors.toMap(Ticket::getTicketId, ticket -> ticket));
        } catch (Exception e) {
            log.error("Error retrieving active tickets", e);
            throw new ResourceProcessingException("Failed to retrieve active tickets");
        }
    }

    /**
     * Retrieves a list of tickets for the specified event name.
     *
     * @param eventName the name of the event
     * @return a list of tickets for the event
     * @throws ResourceProcessingException if there is an error retrieving the tickets
     */
    public List<Ticket> getTicketsByEventName(String eventName) {
        try {
            if (eventName == null || eventName.trim().isEmpty()) {
                throw new IllegalArgumentException("Event name cannot be null or empty");
            }
            log.info("Retrieving tickets for event: {}", eventName);
            return ticketRepository.findByEventName(eventName);
        } catch (Exception e) {
            log.error("Error retrieving tickets for event: {}", eventName, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by event name");
        }
    }

    /**
     * Retrieves a list of tickets for the specified vendor.
     *
     * @param vendorId the ID of the vendor
     * @return a list of tickets for the vendor
     * @throws ResourceProcessingException if there is an error retrieving the tickets
     */
    public List<Ticket> getTicketsByVendor(String vendorId) {
        try {
            if (vendorId == null || vendorId.trim().isEmpty()) {
                throw new IllegalArgumentException("Vendor ID cannot be null or empty");
            }
            log.info("Retrieving tickets for vendor: {}", vendorId);
            return ticketRepository.findByVendorParticipantId(vendorId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for vendor: {}", vendorId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by vendor");
        }
    }

    /**
     * Retrieves a list of tickets for the specified customer.
     *
     * @param customerId the ID of the customer
     * @return a list of tickets for the customer
     * @throws ResourceProcessingException if there is an error retrieving the tickets
     */
    public List<Ticket> getTicketsByCustomer(String customerId) {
        try {
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            log.info("Retrieving tickets for customer: {}", customerId);
            return ticketRepository.findByCustomerParticipantId(customerId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for customer: {}", customerId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by customer");
        }
    }

    /**
     * Deletes the ticket with the specified ID.
     *
     * @param ticketId the ID of the ticket to delete
     * @throws ResourceProcessingException if there is an error deleting the ticket
     */
    @Transactional
    public void deleteTicket(String ticketId) {
        try {
            if (ticketId == null || ticketId.trim().isEmpty()) {
                throw new IllegalArgumentException("Ticket ID cannot be null or empty");
            }
            log.info("Deleting ticket: {}", ticketId);
            ticketRepository.deleteById(ticketId);
        } catch (Exception e) {
            log.error("Error deleting ticket: {}", ticketId, e);
            throw new ResourceProcessingException("Failed to delete ticket");
        }
    }

    /**
     * Retrieves the ticket with the specified ID.
     *
     * @param ticketId the ID of the ticket
     * @return the ticket with the specified ID
     * @throws ResourceNotFoundException   if the ticket is not found
     * @throws ResourceProcessingException if there is an error retrieving the ticket
     */
    public Ticket getTicketById(String ticketId) {
        try {
            if (ticketId == null || ticketId.trim().isEmpty()) {
                throw new IllegalArgumentException("Ticket ID cannot be null or empty");
            }
            log.info("Retrieving ticket: {}", ticketId);
            return ticketRepository.findById(ticketId).orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving ticket: {}", ticketId, e);
            throw new ResourceProcessingException("Failed to retrieve ticket");
        }
    }

    /**
     * Batch saves multiple tickets.
     *
     * @param tickets the list of tickets to save
     * @throws ResourceProcessingException if there is an error saving the tickets
     */
    @Transactional
    public void saveTickets(List<Ticket> tickets) {
        try {
            if (tickets == null || tickets.isEmpty()) {
                throw new IllegalArgumentException("Tickets list cannot be null or empty");
            }
            log.info("Batch saving {} tickets", tickets.size());
            ticketRepository.saveAll(tickets);
        } catch (Exception e) {
            log.error("Error batch saving tickets", e);
            throw new ResourceProcessingException("Failed to batch save tickets: " + e.getMessage());
        }
    }
}