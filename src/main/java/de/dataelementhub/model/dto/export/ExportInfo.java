package de.dataelementhub.model.dto.export;

import java.sql.Timestamp;
import lombok.Data;

@Data
public class ExportInfo {
  private String id;
  private String format;
  private String status;
  private float progress;
  private Timestamp timestamp;
}
