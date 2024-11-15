package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
    List<Customer> findByIsActive(Boolean isActive);

    @Query("{'purchasedTickets': {$size: {$gt: 0}}}")
    List<Customer> findCustomersWithTickets();
}
