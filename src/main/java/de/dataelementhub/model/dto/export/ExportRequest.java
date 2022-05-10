package de.dataelementhub.model.dto.export;

import java.util.List;
import lombok.Data;

/**
 * Export Request DTO.
 */
@Data
public class ExportRequest {
  private String label;
  private List<String> elementUrns;
}

