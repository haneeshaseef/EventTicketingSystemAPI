package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Slf4j
@Data
@NoArgsConstructor
public abstract class Participant implements Runnable {
    @Id
    protected String participantId;
    protected String name;
    protected String email;
    protected String password;

    @JsonIgnore
    @Transient
    protected  Thread participantThread;

    public Participant(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * Starts the participant's thread if not already running.
     */
    public void startParticipant() {
        if (participantThread == null || !participantThread.isAlive()) {
            participantThread = new Thread(this, getClass().getSimpleName() + "-" + name);
            participantThread.start();
        } else {
            log.info("Participant {} is already running", name);
        }
    }

    /**
     * Stops the participant's thread gracefully by interrupting it.
     */
    public void stopParticipant() {
        if (participantThread != null && participantThread.isAlive()) {
            participantThread.interrupt();
        }
    }

    /**
     * Ensures subclasses implement their own `run()` behavior.
     */
    @Override
    public abstract void run();
}
