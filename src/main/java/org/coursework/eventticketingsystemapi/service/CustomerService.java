package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceNotFoundException;
import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final TicketPoolService ticketPoolService;

    private final Map<String, Customer> activeCustomers = new ConcurrentHashMap<>();

    @Autowired
    public CustomerService(CustomerRepository customerRepository, TicketPoolService ticketPoolService) {
        this.customerRepository = customerRepository;
        this.ticketPoolService = ticketPoolService;
    }

    public Map<String, Customer> getActiveCustomers() {
        try {
            log.debug("Retrieving active customers");
            customerRepository.findByIsActive(true).forEach(customer -> {
                initializeCustomerServices(customer);
                activeCustomers.put(customer.getParticipantId(), customer);
            });
            log.info("Successfully retrieved {} active customers", activeCustomers.size());
            return activeCustomers;
        } catch (Exception e) {
            log.error("Error retrieving active customers: {}", e.getMessage(), e);
            throw new ResourceProcessingException("Failed to retrieve active customers");
        }
    }

    public Customer registerCustomer(Customer customer) {
        try {
            log.info("Attempting to register customer: {}", customer.getName());
            validateCustomerConfiguration(customer);

            Optional<Customer> existingCustomer = customerRepository.findByEmail(customer.getEmail());
            return existingCustomer.map(value -> handleExistingCustomer(value, customer)).orElseGet(() -> createNewCustomer(customer));

        } catch (IllegalArgumentException e) {
            log.error("Validation failed for customer {}: {}", customer.getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error registering customer {}: {}", customer.getName(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to register customer");
        }
    }

    private void validateCustomerConfiguration(Customer customer) {
        if (customer.getTicketsToPurchase() <= 0) {
            throw new IllegalArgumentException("Tickets to purchase must be greater than 0");
        }
        if (customer.getTicketRetrievalInterval() <= 0) {
            throw new IllegalArgumentException("Ticket retrieval interval must be greater than 0");
        }
        if (!ticketPoolService.isConfigured()) {
            log.warn("Registering customer while event is not configured");
        }
    }

    private Customer handleExistingCustomer(Customer existingCustomer, Customer newDetails) {
        if (existingCustomer.isActive()) {
            log.error("Customer with email {} is already active", existingCustomer.getEmail());
            throw new IllegalArgumentException("Customer with this email is already active");
        }

        existingCustomer.setActive(true);
        existingCustomer.setTicketsToPurchase(newDetails.getTicketsToPurchase());
        existingCustomer.setTicketRetrievalInterval(newDetails.getTicketRetrievalInterval());
        existingCustomer.setTotalTicketsPurchased(0);

        return saveAndStartCustomer(existingCustomer);
    }

    private Customer createNewCustomer(Customer customer) {
        customer.setActive(true);
        customer.setTotalTicketsPurchased(0);
        return saveAndStartCustomer(customer);
    }

    private Customer saveAndStartCustomer(Customer customer) {
        initializeCustomerServices(customer);
        Customer savedCustomer = customerRepository.save(customer);
        activeCustomers.put(savedCustomer.getParticipantId(), savedCustomer);
        startCustomerThread(savedCustomer);
        return savedCustomer;
    }

    private void initializeCustomerServices(Customer customer) {
        customer.setTicketPoolService(ticketPoolService);
    }

    private void startCustomerThread(Customer customer) {
        try {
            customer.startCustomer();
            Thread customerThread = new Thread(customer);
            customerThread.setName("Customer-" + customer.getParticipantId());
            customerThread.start();
            log.info("Customer {} thread started successfully", customer.getParticipantId());
        } catch (Exception e) {
            log.error("Error starting customer thread {}: {}", customer.getParticipantId(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to start customer thread");
        }
    }

    public void deactivateCustomer(String customerId) {
        try {
            log.info("Deactivating customer: {}", customerId);
            Customer customer = getCustomerById(customerId);

            customer.stopCustomer();
            customer.setActive(false);

            customerRepository.save(customer);
            activeCustomers.remove(customerId);

            log.info("Customer {} successfully deactivated. Final tickets purchased: {}",
                    customerId, customer.getTotalTicketsPurchased());
        } catch (Exception e) {
            log.error("Error deactivating customer {}: {}", customerId, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to deactivate customer");
        }
    }

    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }

    public Optional<Customer> findCustomerByName(String name) {
        try {
            log.debug("Searching for customer with name: {}", name);
            return customerRepository.findByNameIgnoreCase(name);
        } catch (Exception e) {
            log.error("Error finding customer by name {}: {}", name, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find customer by name");
        }
    }
}