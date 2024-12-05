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

    /**
     * Retrieves all customers and returns them as a map.
     *
     * @return a map of all customers
     * @throws ResourceProcessingException if there is an error retrieving the customers
     */
    public Map<String, Customer> getAllCustomers() {
        try {
            log.debug("Retrieving all customers");
            customerRepository.findAll().forEach(this::initializeCustomerServices);
            log.info("Successfully retrieved {} all customers", activeCustomers.size());
            return activeCustomers;
        } catch (Exception e) {
            log.error("Error retrieving all customers: {}", e.getMessage(), e);
            throw new ResourceProcessingException("Failed to retrieve all customers");
        }
    }

    /**
     * Retrieves all active customers and returns them as a map.
     *
     * @return a map of all active customers
     * @throws ResourceProcessingException if there is an error retrieving the customers
     */
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

    /**
     * Registers a new customer with the specified details.
     *
     * @param customer the customer to register
     * @return the registered customer
     * @throws IllegalArgumentException       if the configuration is invalid
     * @throws ResourceProcessingException if there is an error registering the customer
     */
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

    /**
     * Finds the total tickets purchased by the customer with the specified name.
     *
     * @param customerName the name of the customer
     * @return the total tickets purchased by the customer
     * @throws ResourceProcessingException if there is an error finding the total tickets purchased
     */
    public int findTotalTicketsPurchasedByCustomer(String customerName) {
        try {
            log.debug("Finding total tickets purchased by customer: {}", customerName);
            Customer customer = getCustomerById(customerName);
            int totalTicketsPurchased = customer.getTotalTicketsPurchased();
            log.info("Total tickets purchased by customer {}: {}", customerName, totalTicketsPurchased);
        } catch (Exception e) {
            log.error("Error finding total tickets purchased by customer {}: {}", customerName, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find total tickets purchased by customer");
        }
        return 0;
    }

    /**
     * Validates the configuration for the customer.
     *
     * @param customer the customer to validate
     * @throws IllegalArgumentException if the configuration is invalid
     */
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

    /**
     * Handles an existing customer by reactivating them with the new details.
     *
     * @param existingCustomer the existing customer
     * @param newDetails       the new details for the customer
     * @return the updated customer
     * @throws IllegalArgumentException if the existing customer is already active
     */
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

    /**
     * Creates a new customer with the specified details.
     *
     * @param customer the customer to create
     * @return the created customer
     */
    private Customer createNewCustomer(Customer customer) {
        customer.setActive(true);
        customer.setTotalTicketsPurchased(0);
        return saveAndStartCustomer(customer);
    }

    /**
     * Saves the customer and starts the customer thread.
     *
     * @param customer the customer to save and start
     * @return the saved customer
     */
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

    /**
     * Starts the customer thread for the specified customer.
     *
     * @param customer the customer to start
     * @throws ResourceProcessingException if there is an error starting the customer thread
     */
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

    /**
     * Reactivates the customer with the specified name.
     *
     * @param customerName the name of the customer to reactivate
     * @throws ResourceNotFoundException   if the customer is not found
     * @throws ResourceProcessingException if there is an error reactivating the customer
     */
    public void reactivateCustomer(String customerName) {
        try {
            log.info("Reactivating customer: {}", customerName);
            Customer customer = findCustomerByName(customerName)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with name: " + customerName));
            customer.startCustomer();
            customer.setActive(true);
            customerRepository.save(customer);
            activeCustomers.put(customer.getParticipantId(), customer);
            startCustomerThread(customer);
            log.info("Customer {} successfully reactivated", customerName);
        } catch (Exception e) {
            log.error("Error reactivating customer {}: {}", customerName, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to reactivate customer");
        }
    }

    /**
     * Deactivates the customer with the specified name.
     *
     * @param customerName the name of the customer to deactivate
     * @throws ResourceProcessingException if there is an error deactivating the customer
     */
    public void deactivateCustomer(String customerName) {
        try {
            log.info("Deactivating customer: {}", customerName);
            Customer customer = getCustomerById(customerName);

            customer.stopCustomer();
            customer.setActive(false);

            customerRepository.save(customer);
            activeCustomers.remove(customerName);

            log.info("Customer {} successfully deactivated. Final tickets purchased: {}",
                    customerName, customer.getTotalTicketsPurchased());
        } catch (Exception e) {
            log.error("Error deactivating customer {}: {}", customerName, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to deactivate customer");
        }
    }

    /**
     * Retrieves the customer with the specified name.
     *
     * @param name the name of the customer
     * @return the customer with the specified name
     * @throws ResourceProcessingException if there is an error retrieving the customer
     */
    public Optional<Customer> findCustomerByName(String name) {
        try {
            log.debug("Searching for customer with name: {}", name);
            return customerRepository.findByNameIgnoreCase(name);
        } catch (Exception e) {
            log.error("Error finding customer by name {}: {}", name, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find customer by name");
        }
    }

   /**
     * Retrieves the customer with the specified email.
     *
     * @param email the email of the customer
     * @return the customer with the specified email
     * @throws ResourceProcessingException if there is an error retrieving the customer
     */
    public Optional<Customer> findCustomerByEmail(String email) {
        try {
            log.debug("Searching for customer with email: {}", email);
            return customerRepository.findByEmail(email);
        } catch (Exception e) {
            log.error("Error finding customer by email {}: {}", email, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to find customer by email");
        }
    }

    /**
     * Retrieves the customer with the specified ID.
     *
     * @param customerId the ID of the customer
     * @return the customer with the specified ID
     * @throws ResourceNotFoundException   if the customer is not found
     * @throws ResourceProcessingException if there is an error retrieving the customer
     */
    public Customer getCustomerById(String customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }
}