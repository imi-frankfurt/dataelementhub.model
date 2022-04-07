package de.dataelementhub.model.dto.datatransfer;

import de.dataelementhub.model.dto.element.StagedElement;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "dehub_data_transfer", namespace = "http://dehub.de/StagedElement")
@XmlAccessorType(XmlAccessType.FIELD)
public class DataTransfer {
  private String label;
  private List<StagedElement> stagedElements;
}
