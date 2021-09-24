package de.dataelementhub.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.dataelementhub.dal.jooq.enums.Status;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class Member implements Serializable {

  private String elementUrn;
  private Status status;
  private Integer order;
}
