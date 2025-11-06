package de.dataelementhub.model.dto.importexport;

import java.sql.Timestamp;
import lombok.Data;

/**
 * Export Description.
 */
@Data
public class ExportDescription {
  private String id;
  private String format;
  private String status;
  private Timestamp timestamp;
}
