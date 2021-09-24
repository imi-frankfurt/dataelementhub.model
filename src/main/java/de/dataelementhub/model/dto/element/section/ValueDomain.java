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
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
