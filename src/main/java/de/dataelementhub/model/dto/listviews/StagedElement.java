package de.dataelementhub.model.dto.listviews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.dal.jooq.enums.ElementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class StagedElement {
  private String stagedElementId;
  private String elementUrn;
  private ElementType elementType;
  private String designation;
}
