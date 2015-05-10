package de.christl.smsoip.database.entities;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by danny on 10.05.15.
 */
public class Messages implements Serializable {

    private Long id;
    private String number;
    private String message;
    private Date date;
}
