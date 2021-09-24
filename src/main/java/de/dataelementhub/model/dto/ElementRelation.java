package de.dataelementhub.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode
public class ElementRelation {

  private String leftUrn;
  private String rightUrn;
  private RelationType relation;
  private Integer createdBy;
  private LocalDateTime createdAt;
  private Source leftSource;
  private Source rightSource;
}
