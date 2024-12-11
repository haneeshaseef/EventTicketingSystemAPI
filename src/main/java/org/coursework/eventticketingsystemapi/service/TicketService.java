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
     * Retrieves a list of all tickets.
     *
     * @return a list of all tickets
     * @throws ResourceProcessingException if there is an error retrieving the tickets
     */
    public List<Ticket> getAllTickets() {
        try {
            log.info("Retrieving all active tickets");
            return ticketRepository.findAll();
        } catch (Exception e) {
            log.error("Error retrieving active tickets", e);
            throw new ResourceProcessingException("Failed to retrieve active tickets");
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
            return ticketRepository.findTicketsByVendorParticipantId(vendorId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for vendor: {}", vendorId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by vendor");
        }
    }


    /**
     * Deletes the ticket with the specified ID.
     *
     * @param ticketId the ID of the ticket to delete
     * @throws ResourceProcessingException if there is an error deleting the ticket
     */
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

    //get ticket by customer ID
    public List<Ticket> getTicketsByCustomer(String customerId) {
        try {
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }

            log.info("Retrieving tickets for customer: {}", customerId);
            return ticketRepository.findTicketsByCustomerParticipantId(customerId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for customer: {}", customerId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by customer");
        }
    }

    /**
     * Batch saves multiple tickets.
     *
     * @param tickets the list of tickets to save
     * @throws ResourceProcessingException if there is an error saving the tickets
     */
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