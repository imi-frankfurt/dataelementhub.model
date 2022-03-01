package de.dataelementhub.model.dto.element;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonInclude(Include.NON_NULL)
public class ElementPath {
  private String urns;
  private String designations;
}
