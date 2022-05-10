package de.dataelementhub.model.dto.importexport;

import java.sql.Timestamp;
import lombok.Data;
import org.springframework.http.MediaType;

@Data
public class ExportInfo {
  private String id;
  private MediaType mediaType;
  private String status;
  private float progress;
  private Timestamp timestamp;
}
