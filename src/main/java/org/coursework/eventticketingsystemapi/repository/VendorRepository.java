package org.coursework.eventticketingsystemapi.repository;

import org.coursework.eventticketingsystemapi.model.Vendor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository<Vendor, String> {
    Optional<Vendor> findByEmailIgnoreCase(String email);
    List<Vendor> findByIsActive(boolean isActive);
    Optional<Vendor> findByNameIgnoreCase(String name);
    boolean existsByEmailIgnoreCase(String email);
    List<Vendor> findAllByOrderByNameAsc();
}
