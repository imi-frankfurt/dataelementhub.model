package de.dataelementhub.model.dto.element.section.validation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Numeric Validation DTO.
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
public abstract class Numeric implements Serializable {
  public static final String TYPE_INTEGER = "INTEGER";
  public static final String TYPE_FLOAT = "FLOAT";

  private String type;
  private Boolean useMinimum;
  private Boolean useMaximum;
  private String unitOfMeasure;
}
