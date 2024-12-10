package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Vendor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository<Vendor, String> {
    Optional<Vendor> findByEmailIgnoreCase(String email);
    Optional<Vendor> findByNameIgnoreCase(String name);
    List<Vendor> findByIsActive(boolean isActive);
}