package de.dataelementhub.model.dto.importexport;

import java.sql.Timestamp;
import lombok.Data;

@Data
public class ImportDescription {
  private String id;
  private String namespaceUrn;
  private String status;
  private Timestamp timestamp;
}
