package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * NumericFloat Validation DTO.
 */
@Data
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@org.eclipse.persistence.oxm.annotations.XmlDiscriminatorValue("FLOAT")
public class NumericFloat extends Numeric implements Serializable {

  private Double minimum;
  private Double maximum;

  public NumericFloat() {
    this.setType(Numeric.TYPE_FLOAT);
  }
}
