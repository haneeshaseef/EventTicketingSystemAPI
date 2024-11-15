package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public abstract class Participant implements Runnable {
    @Id
    protected String participantId;
    protected String name;
    protected String email;
    protected Boolean isActive = true;

    @JsonIgnore
    protected transient Thread participantThread;

    public Participant(String name, String email, Boolean isActive) {
        this.name = name;
        this.email = email;
        this.isActive = isActive;
    }

    /**
     * Starts the participant's thread if not already running.
     */
    public void startParticipant() {
        if (isActive && (participantThread == null || !participantThread.isAlive())) {
            participantThread = new Thread(this, getClass().getSimpleName() + "-" + name);
            participantThread.start();
        } else {
            System.out.println(getClass().getSimpleName() + " " + name + " is already running or inactive.");
        }
    }

    /**
     * Stops the participant's thread gracefully by setting isActive to false.
     */
    public void stopParticipant() {
        isActive = false;
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

