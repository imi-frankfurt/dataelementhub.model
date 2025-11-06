package de.dataelementhub.model.service.Terminologies;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 /**
 * Inner class representing a FHiR concept with code and display name.
 */
@Data
@AllArgsConstructor
public class Concept {
    private String term;
    private String text;
    private String system;
    private String version;
}
