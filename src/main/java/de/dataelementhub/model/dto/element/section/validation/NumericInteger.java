package de.dataelementhub.model.dto.element.section.validation;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.Serializable;

/**
 * NumericInteger Validation DTO.
 */
@Data
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@org.eclipse.persistence.oxm.annotations.XmlDiscriminatorValue("INTEGER")
public class NumericInteger extends Numeric implements Serializable {

  private Long minimum;
  private Long maximum;

  public NumericInteger() {
    this.setType(Numeric.TYPE_INTEGER);
  }
}
