package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByEventName(String eventName);
    List<Ticket> findByVendorParticipantId(String participantId);
    List<Ticket> findByCustomerParticipantId(String participantId);
    int countByVendorParticipantId(String participantId);
    List<Ticket> findByEventNameAndVendorParticipantId(String eventName, String vendorParticipantId);
    long countByEventName(String eventName);

    @Query("{'active': true}")
    List<Ticket> findActiveTickets();

    @Query(value = "{'vendorParticipantId': ?0, 'active': true}", count = true)
    long countActiveTicketsByVendorParticipantId(String participantId);
}