package de.dataelementhub.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.dataelementhub.dal.jooq.enums.Status;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

/**
 * Member DTO.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Member implements Serializable {

  private String elementUrn;
  private Status status;
  private Integer order;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Member member = (Member) o;
    return Objects.equals(elementUrn, member.elementUrn) && Objects.equals(order,
        member.order);
  }

  @Override
  public int hashCode() {
    return Objects.hash(elementUrn, order);
  }
}
