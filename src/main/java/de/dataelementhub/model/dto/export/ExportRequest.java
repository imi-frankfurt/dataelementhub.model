package de.dataelementhub.model.dto.export;

import java.util.List;
import lombok.Data;

@Data
public class ExportRequest {
  private String label;
  private List<String> elementUrns;
}

