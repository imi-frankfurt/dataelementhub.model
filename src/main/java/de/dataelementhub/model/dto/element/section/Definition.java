package de.dataelementhub.model.dto.element.section;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Definition DTO.
 */
@Data
@EqualsAndHashCode
public class Definition implements Serializable {
  private String definition;
  private String designation;
  private String language;
}
