package de.dataelementhub.model.dto.element.section.validation;

import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermittedValue extends Element implements Serializable {

  private String value;
  private String urn;
  private List<ConceptAssociation> conceptAssociations;

}
