package de.dataelementhub.model.dto.importexport;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.Data;

/**
 * Import DTO.
 */
@Data
@XmlRootElement(name = "import", namespace = "http://schema.samply.de/StagedElement")
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportDto {
  private String label;
  @XmlElementWrapper(name = "stagedElements")
  @XmlElement(name = "stagedElement")
  private List<StagedElement> stagedElements;
}
