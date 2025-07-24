package de.dataelementhub.model.dto.importexport;

import de.dataelementhub.model.dto.element.StagedElement;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * Export DTO.
 */
@Data
@XmlRootElement(name = "dehub_data_transfer", namespace = "http://dehub.de/StagedElement")
@XmlAccessorType(XmlAccessType.FIELD)
public class ImportExport {
  private String label;
  private List<StagedElement> stagedElements;
}
