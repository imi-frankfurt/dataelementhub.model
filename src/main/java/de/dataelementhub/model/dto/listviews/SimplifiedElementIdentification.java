package de.dataelementhub.model.dto.listviews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonInclude(Include.NON_NULL)
public class SimplifiedElementIdentification {
  private String urn;
  private String designation;
}