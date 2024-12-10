package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);

    List<Customer> findByIsActive(Boolean isActive);

    Optional<Customer> findByNameIgnoreCase(String name);
}
