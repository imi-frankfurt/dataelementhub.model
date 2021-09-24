package de.dataelementhub.model.dto.element.section;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class Definition implements Serializable {

  private String designation;
  private String definition;
  private String language;
}
