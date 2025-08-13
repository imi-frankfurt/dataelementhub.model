package de.dataelementhub.model.dto.element.section.validation;

import de.dataelementhub.model.dto.element.Element;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * PermittedValue Validation DTO.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DefinedPermittedValue extends Element implements Serializable {

  private String value;
  private String urn;
 // private List<ConceptAssociation> conceptAssociations;

}
