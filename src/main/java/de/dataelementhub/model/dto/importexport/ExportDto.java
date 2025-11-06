package de.dataelementhub.model.dto.importexport;

import java.util.List;
import lombok.Data;

/**
 * Export DTO.
 */
@Data
public class ExportDto {
  private String export;
  private List<String> elements;
}
