package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Numeric Validation DTO.
 */
@Data
@EqualsAndHashCode
public abstract class Numeric implements Serializable {
  public static final String TYPE_INTEGER = "INTEGER";
  public static final String TYPE_FLOAT = "FLOAT";

  private String type;
  private Boolean useMinimum;
  private Boolean useMaximum;
  private String unitOfMeasure;
}
