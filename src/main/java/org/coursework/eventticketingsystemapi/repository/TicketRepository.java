package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByEventName(String eventName);
    List<Ticket> findByVendorParticipantId(String ParticipantId);
    List<Ticket> findByCustomerParticipantId(String ParticipantId);
}
