package de.dataelementhub.model.dto.element;

import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.Slot;
import de.dataelementhub.model.dto.element.section.validation.Datetime;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.element.section.validation.Text;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * StagedElement DTO.
 */
@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StagedElement {
  private Identification identification;

  @XmlElementWrapper(name = "definitions")
  @XmlElement(name = "definition")
  private List<Definition> definitions;

  @XmlElementWrapper(name = "slots")
  @XmlElement(name = "slot")
  private List<Slot> slots;

  @XmlElementWrapper(name = "conceptAssociations")
  @XmlElement(name = "conceptAssociation")
  private List<ConceptAssociation> conceptAssociations;
  private String valueDomainUrn;

  @XmlElementWrapper(name = "members")
  @XmlElement(name = "member")
  private List<Member> members;

  private String type;
  private String urn;
  private Text text;
  private String value;
  private Numeric numeric;
  private Datetime datetime;
  @XmlElementWrapper(name = "permittedValues")
  @XmlElement(name = "permittedValue")
  private List<PermittedValue> permittedValues;
}
