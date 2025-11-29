package de.dataelementhub.model.dto.element;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * Dataelement DTO.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
public class DataElement extends Element implements Serializable {

  private ValueDomain valueDomain;
  private String valueDomainUrn;
  private List<ConceptAssociation> conceptAssociations;
}
