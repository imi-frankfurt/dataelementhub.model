package de.dataelementhub.model.dto.element.section.validation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * NumericInteger Validation DTO.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NumericInteger extends Numeric implements Serializable {

  private Long minimum;
  private Long maximum;

  public NumericInteger() {
    this.setType(Numeric.TYPE_INTEGER);
  }
}
