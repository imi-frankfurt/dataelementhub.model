package de.dataelementhub.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.validation.Datetime;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.element.section.validation.Text;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ValueDomain DTO.
 */
@Data
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(callSuper = true)
public class ValueDomain extends Element implements Serializable {

  public static final String TYPE_STRING = "STRING";
  public static final String TYPE_NUMERIC = "NUMERIC";
  public static final String TYPE_BOOLEAN = "BOOLEAN";
  public static final String TYPE_TBD = "TBD";
  public static final String TYPE_ENUMERATED = "ENUMERATED";
  public static final String TYPE_DATE = "DATE";
  public static final String TYPE_DATETIME = "DATETIME";
  public static final String TYPE_TIME = "TIME";
  private String type;
  private Text text;
  private Numeric numeric;
  private Datetime datetime;
  private List<PermittedValue> permittedValues;
  private List<ConceptAssociation> conceptAssociations;

  /**
   * Check if the validations differ between two value domains.
   */
  public boolean validationsDiffer(ValueDomain other) {
    if (!this.type.equalsIgnoreCase(other.type)) {
      return true;
    }
    if ((this.text == null && other.text != null) || !Objects.equals(this.text, other.text)) {
      return true;
    }
    if ((this.numeric == null && other.numeric != null) || !Objects.equals(this.numeric,
        other.numeric)) {
      return true;
    }
    if ((this.datetime == null && other.datetime != null) || !Objects.equals(this.datetime,
        other.datetime)) {
      return true;
    }
    if ((this.permittedValues == null && other.permittedValues != null) || !(this.permittedValues
        == other.permittedValues)) {
      return true;
    }
    // Concept associations may differ, so they are not reflected here

    return false;
  }
}
