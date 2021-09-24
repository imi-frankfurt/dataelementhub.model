package de.dataelementhub.model.dto.element.section;

import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations;
import de.dataelementhub.dal.jooq.tables.pojos.Concepts;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ConceptAssociation implements Serializable {

  private Integer conceptId;
  private String system;
  private Integer sourceId;
  private String version;
  private String term;
  private String text;
  private RelationType linktype;
  private Integer scopedIdentifierId;

  /**
   * Construct a new ConceptAssociation.
   */
  public ConceptAssociation(ConceptElementAssociations cea, Concepts concepts) {
    setConceptId(concepts.getId());
    setSystem(concepts.getSystem());
    setSourceId(concepts.getSourceId());
    setVersion(concepts.getVersion());
    setTerm(concepts.getTerm());
    setText(concepts.getText());
    setLinktype(cea.getLinktype());
    setScopedIdentifierId(cea.getScopedidentifierId());
  }
}
