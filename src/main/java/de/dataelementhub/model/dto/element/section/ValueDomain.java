package de.dataelementhub.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.validation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

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
  public static final String TYPE_DEFINED = "DEFINED";
  public static final String TYPE_DATE = "DATE";
  public static final String TYPE_DATETIME = "DATETIME";
  public static final String TYPE_TIME = "TIME";
  private String type;
  private Text text;
  private Numeric numeric;
  private Datetime datetime;
  private List<PermittedValue> permittedValues;
  private List<DefinedPermittedValue> definedPermittedValues;
  private List<ConceptAssociation> conceptAssociations;
  private ValueDomainReferenceDTO valueDomainReferenceDTO;
}
