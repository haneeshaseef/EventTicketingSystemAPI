package org.coursework.eventticketingsystemapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
@NoArgsConstructor

public abstract class Participant implements Runnable {
    @Id
    protected String participantId;
    protected String name;
    protected String email;

    @JsonIgnore
    @Transient
    protected  Thread participantThread;

    public Participant(String name, String email) {
        this.name = name;
        this.email = email;

    }

    /**
     * Starts the participant's thread if not already running.
     */
    public void startParticipant() {
        if (participantThread == null || !participantThread.isAlive()) {
            participantThread = new Thread(this, getClass().getSimpleName() + "-" + name);
            participantThread.start();
        } else {
            System.out.println(getClass().getSimpleName() + " " + name + " is already running.");
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
