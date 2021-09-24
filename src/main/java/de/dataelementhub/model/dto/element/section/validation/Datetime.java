package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class Datetime implements Serializable {

  private String date;
  private String time;
  private String hourFormat;
}
