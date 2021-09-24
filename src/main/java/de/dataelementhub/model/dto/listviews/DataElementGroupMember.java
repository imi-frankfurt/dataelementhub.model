package de.dataelementhub.model.dto.listviews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Definition;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class DataElementGroupMember {
  private String urn;
  private List<Definition> definitions;
  private Definition definition;

  /**
   * Construct a listview dto of dataElementGroup member from an element.
   * The element can be either dataelement, dataelementgroup or record.
   */
  public DataElementGroupMember(Element element) {
    this.urn = element.getIdentification().getUrn();
    this.definitions = element.getDefinitions();
    this.definition = element.getDefinition();
  }
}


