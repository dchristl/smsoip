package de.christl.smsoip.models;

import java.io.Serializable;
import java.util.Date;

/**
 *
 */
public class Message implements Serializable{

    private String message;
    private boolean outgoing = true;
    private final Date date;

    public Message(String message, boolean outgoing, Date date) {
        this.message = message;
        this.outgoing = outgoing;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public Date getDate() {
        return date;
    }
}
