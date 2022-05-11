package de.dataelementhub.model.dto.search;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SearchRequest DTO.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {
  private String searchText;
  private List<ElementType> type;
  private List<Status> status;
  private List<String> elementParts;
}
