package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findTicketsByCustomerParticipantId(String participantId);
    List<Ticket> findTicketsByVendorName(String vendorName);
    int countByVendorParticipantId(String participantId);
}