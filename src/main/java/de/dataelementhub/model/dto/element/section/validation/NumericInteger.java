package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class NumericInteger extends Numeric implements Serializable {

  private Long minimum;
  private Long maximum;

  public NumericInteger() {
    this.setType(Numeric.TYPE_INTEGER);
  }
}
