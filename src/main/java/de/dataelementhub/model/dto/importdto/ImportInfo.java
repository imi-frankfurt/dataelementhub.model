package de.dataelementhub.model.dto.importdto;

import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class ImportInfo {
  private Integer id;
  private String namespaceUrn;
  private ProcessStatus status;
  private Timestamp timestamp;
}
