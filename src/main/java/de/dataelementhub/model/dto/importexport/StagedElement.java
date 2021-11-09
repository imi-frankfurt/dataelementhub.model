package de.dataelementhub.model.dto.importexport;

import de.dataelementhub.model.dto.element.section.*;

import de.dataelementhub.model.dto.element.section.validation.Datetime;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.element.section.validation.Text;
import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

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
