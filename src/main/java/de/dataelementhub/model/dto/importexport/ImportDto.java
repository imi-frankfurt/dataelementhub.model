package de.dataelementhub.model.dto.importexport;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "import", namespace = "http://schema.samply.de/StagedElement")
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportDto {
  private String label;
  @XmlElementWrapper(name = "stagedElements")
  @XmlElement(name = "stagedElement")
  private List<StagedElement> stagedElements;
}
