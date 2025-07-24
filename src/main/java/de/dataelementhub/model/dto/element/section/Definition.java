package de.dataelementhub.model.dto.element.section;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Definition DTO.
 */
@Data
@EqualsAndHashCode
public class Definition implements Serializable {

  private String designation;
  private String definition;
  private String language;
}
