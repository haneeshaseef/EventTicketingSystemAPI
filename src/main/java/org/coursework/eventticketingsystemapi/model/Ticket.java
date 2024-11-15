package org.coursework.eventticketingsystemapi.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "tickets")
@EqualsAndHashCode(exclude = {"vendor", "customer"})
@ToString(exclude = {"vendor", "customer"})
public class Ticket {
    @Id
    private String ticketId;

    @DBRef
    private Vendor vendor;

    @DBRef
    private Customer customer;

    @Indexed
    private String eventName;

    private LocalDateTime createdAt;
    private LocalDateTime purchasedAt;

    public Ticket() {
        this.createdAt = LocalDateTime.now();
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
        if (customer != null) {
            this.purchasedAt = LocalDateTime.now();
            if (!customer.getPurchasedTickets().contains(this)) {
                customer.getPurchasedTickets().add(this);
            }
        }
    }
}