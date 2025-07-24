package de.dataelementhub.model.dto.element.section;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Slot DTO.
 */
@Data
@EqualsAndHashCode
public class Slot implements Serializable {

  private String name;
  private String value;
}
