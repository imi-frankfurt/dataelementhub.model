package de.dataelementhub.model.dto.importexport;

import lombok.Data;

import java.util.List;

@Data
public class ExportDto {
  private String export;
  private List<String> elements;
}
