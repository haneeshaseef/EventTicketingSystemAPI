package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Ticket;
import org.coursework.eventticketingsystemapi.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    @Autowired
    private TicketRepository ticketRepository;

    public Ticket saveTicket(Ticket ticket) {
        try {
            log.info("Saving ticket for event: {}", ticket.getEventName());
            return ticketRepository.save(ticket);
        } catch (Exception e) {
            log.error("Error saving ticket", e);
            throw new ResourceProcessingException("Failed to save ticket");
        }
    }

    public Map<String, Ticket> getActiveTickets() {
        try {
            log.info("Retrieving all active tickets");
            List<Ticket> tickets = ticketRepository.findAll();
            return tickets.stream()
                    .collect(Collectors.toMap(Ticket::getTicketId, ticket -> ticket));
        } catch (Exception e) {
            log.error("Error retrieving active tickets", e);
            throw new ResourceProcessingException("Failed to retrieve active tickets");
        }
    }

    public List<Ticket> getTicketsByEventName(String eventName) {
        try {
            log.info("Retrieving tickets for event: {}", eventName);
            return ticketRepository.findByEventName(eventName);
        } catch (Exception e) {
            log.error("Error retrieving tickets for event: {}", eventName, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by event name");
        }
    }

    public List<Ticket> getTicketsByVendor(String vendorId) {
        try {
            log.info("Retrieving tickets for vendor: {}", vendorId);
            return ticketRepository.findByVendorParticipantId(vendorId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for vendor: {}", vendorId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by vendor");
        }
    }

    public List<Ticket> getTicketsByCustomer(String customerId) {
        try {
            log.info("Retrieving tickets for customer: {}", customerId);
            return ticketRepository.findByCustomerParticipantId(customerId);
        } catch (Exception e) {
            log.error("Error retrieving tickets for customer: {}", customerId, e);
            throw new ResourceProcessingException("Failed to retrieve tickets by customer");
        }
    }

    public void deleteTicket(String ticketId) {
        try {
            log.info("Deleting ticket: {}", ticketId);
            ticketRepository.deleteById(ticketId);
        } catch (Exception e) {
            log.error("Error deleting ticket: {}", ticketId, e);
            throw new ResourceProcessingException("Failed to delete ticket");
        }
    }

    public Ticket getTicketById(String ticketId) {
        try {
            log.info("Retrieving ticket: {}", ticketId);
            return ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceProcessingException("Ticket not found with id: " + ticketId));
        } catch (Exception e) {
            log.error("Error retrieving ticket: {}", ticketId, e);
            throw new ResourceProcessingException("Failed to retrieve ticket");
        }
    }
}