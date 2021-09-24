package de.dataelementhub.model.dto.element;

import de.dataelementhub.model.dto.element.section.Member;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Record extends Element implements Serializable {

  private List<Member> members;
}
