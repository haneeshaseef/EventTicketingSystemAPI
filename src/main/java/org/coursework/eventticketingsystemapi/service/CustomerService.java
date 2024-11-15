package org.coursework.eventticketingsystemapi.service;

import org.coursework.eventticketingsystemapi.exception.ResourceProcessingException;
import org.coursework.eventticketingsystemapi.model.Customer;
import org.coursework.eventticketingsystemapi.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TicketPoolService ticketPoolService;

    private final Map<String, Customer> activeCustomers = new ConcurrentHashMap<>();

    public Map<String, Customer> getActiveCustomers() {
        try {
            log.debug("Retrieving active customers");
            customerRepository.findByIsActive(true)
                    .forEach(customer -> activeCustomers.put(customer.getParticipantId(), customer));
            log.info("Successfully retrieved {} active customers", activeCustomers.size());
            return activeCustomers;
        } catch (Exception e) {
            log.error("Error retrieving active customers: {}", e.getMessage(), e);
            throw new ResourceProcessingException("Failed to retrieve active customers");
        }
    }

    public Customer registerCustomer(Customer customer) {
        try {
            log.info("Attempting to register new customer: {}", customer.getName());

            // Validate customer configuration
            if (customer.getTicketsToPurchase() <= 0 || customer.getTicketRetrievalInterval() <= 0) {
                log.error("Invalid customer configuration: ticketsToPurchase={}, ticketRetrievalInterval={}",
                        customer.getTicketsToPurchase(), customer.getTicketRetrievalInterval());
                throw new IllegalArgumentException("Invalid customer configuration parameters");
            }

            // Check for existing customer with same email
            Optional<Customer> existingCustomer = customerRepository.findByEmail(customer.getEmail());
            if (existingCustomer.isPresent()) {
                if (existingCustomer.get().getIsActive()) {
                    log.error("Customer with email {} is already active", customer.getEmail());
                    throw new IllegalArgumentException("Customer with this email is already active");
                } else {
                    // Reactivate the existing customer
                    Customer reactivatedCustomer = existingCustomer.get();
                    reactivatedCustomer.setIsActive(true);
                    reactivatedCustomer.reset(); // Reset purchase history
                    customer = customerRepository.save(reactivatedCustomer);
                    activeCustomers.put(customer.getParticipantId(), customer);
                    startCustomerThread(customer);
                    log.info("Reactivated existing customer with email {}", customer.getEmail());
                    return customer;
                }
            }

            // Set customer as active by default
            customer.setIsActive(true);

            Customer savedCustomer = customerRepository.save(customer);
            activeCustomers.put(savedCustomer.getParticipantId(), savedCustomer);
            // Automatically start the customer thread
            startCustomerThread(savedCustomer);
            log.info("Customer {} successfully registered and started with ID: {}",
                    savedCustomer.getName(), savedCustomer.getParticipantId());
            return savedCustomer;
        } catch (Exception e) {
            log.error("Error registering customer {}: {}", customer.getName(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to register customer");
        }
    }

    private void startCustomerThread(Customer customer) {
        try {
            customer.setTicketPoolService(ticketPoolService);
            customer.startCustomer();
            Thread customerThread = new Thread(customer);
            customerThread.setName("Customer-" + customer.getParticipantId());
            customerThread.start();
            log.info("Customer {} thread started automatically after registration", customer.getParticipantId());
        } catch (Exception e) {
            log.error("Error starting customer thread {}: {}", customer.getParticipantId(), e.getMessage(), e);
            throw new ResourceProcessingException("Failed to start customer thread");
        }
    }

    public void deactivateCustomer(String customerId) {
        try {
            log.info("Deactivating customer: {}", customerId);
            Optional<Customer> customerOpt = customerRepository.findById(customerId);

            if (customerOpt.isEmpty()) {
                log.warn("Customer not found: {}", customerId);
                throw new IllegalArgumentException("Customer not found");
            }

            Customer customer = customerOpt.get();
            customer.setIsActive(false);
            customer.stopCustomer();
            customerRepository.save(customer);
            activeCustomers.remove(customerId);
            log.info("Customer {} successfully deactivated", customerId);
        } catch (Exception e) {
            log.error("Error deactivating customer {}: {}", customerId, e.getMessage(), e);
            throw new ResourceProcessingException("Failed to deactivate customer");
        }
    }

    public Customer getCustomerById(String customerId) {
        try {
            log.info("Retrieving customer by ID: {}", customerId);
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceProcessingException("Customer not found with id: " + customerId));
        } catch (Exception e) {
            log.error("Error retrieving customer by ID: {}", customerId, e);
            throw new ResourceProcessingException("Failed to retrieve customer");
        }
    }

    public Customer getCustomerByEmail(String email) {
        try {
            log.info("Retrieving customer by email: {}", email);
            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceProcessingException("Customer not found with email: " + email));
        } catch (Exception e) {
            log.error("Error retrieving customer by email: {}", email, e);
            throw new ResourceProcessingException("Failed to retrieve customer by email");
        }
    }

    public List<Customer> getCustomersWithTickets() {
        try {
            log.info("Retrieving customers with purchased tickets");
            return customerRepository.findCustomersWithTickets();
        } catch (Exception e) {
            log.error("Error retrieving customers with tickets", e);
            throw new ResourceProcessingException("Failed to retrieve customers with tickets");
        }
    }

    public void activateCustomer(String customerId) {
        try {
            log.info("Activating customer: {}", customerId);
            Customer customer = getCustomerById(customerId);
            customer.setIsActive(true);
            customer.reset(); // Reset purchase history before reactivating
            customerRepository.save(customer);
            startCustomerThread(customer);
            log.info("Customer {} reactivated and thread started", customerId);
        } catch (Exception e) {
            log.error("Error activating customer: {}", customerId, e);
            throw new ResourceProcessingException("Failed to activate customer");
        }
    }

    public boolean existsByEmail(String email) {
        try {
            log.debug("Checking if customer exists with email: {}", email);
            return customerRepository.findByEmail(email).isPresent();
        } catch (Exception e) {
            log.error("Error checking customer existence by email: {}", email, e);
            throw new ResourceProcessingException("Failed to check customer existence");
        }
    }

    public void deleteCustomer(String customerId) {
        try {
            log.info("Deleting customer: {}", customerId);
            Optional<Customer> customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                if (customer.getIsActive()) {
                    customer.stopCustomer(); // Stop the thread if active
                }
                activeCustomers.remove(customerId);
                customerRepository.deleteById(customerId);
                log.info("Customer {} successfully deleted", customerId);
            }
        } catch (Exception e) {
            log.error("Error deleting customer: {}", customerId, e);
            throw new ResourceProcessingException("Failed to delete customer");
        }
    }
}
