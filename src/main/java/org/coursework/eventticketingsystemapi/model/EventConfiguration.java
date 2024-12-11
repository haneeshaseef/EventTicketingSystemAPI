package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class EventConfiguration {
    private String eventName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;
    private int totalTickets;
    private int maxCapacity;
    private int ticketReleaseRate;
    private int customerRetrievalRate;

    public EventConfiguration(String eventName, LocalDateTime eventDate, int totalTickets, int maxCapacity, int ticketReleaseRate, int customerRetrievalRate) {
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.totalTickets = totalTickets;
        this.maxCapacity = maxCapacity;
        this.ticketReleaseRate = ticketReleaseRate;
        this.customerRetrievalRate = customerRetrievalRate;
    }
}
