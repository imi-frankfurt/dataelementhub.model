package de.dataelementhub.model.dto.export;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "import", namespace = "http://dehub.de/StagedElement")
@XmlAccessorType(XmlAccessType.FIELD)
public class Export {
  private String label;
  @XmlElementWrapper(name = "stagedElements")
  @XmlElement(name = "stagedElement")
  private List<StagedElement> stagedElements;
}
