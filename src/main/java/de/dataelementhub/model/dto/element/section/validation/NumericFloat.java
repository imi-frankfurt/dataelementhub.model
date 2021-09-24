package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class NumericFloat extends Numeric implements Serializable {

  private Double minimum;
  private Double maximum;

  public NumericFloat() {
    this.setType(Numeric.TYPE_FLOAT);
  }
}
