package de.dataelementhub.model.dto.listviews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.dal.jooq.enums.Status;
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
public class Namespace {

  private String urn;
  private int identifier;
  private List<Definition> definitions;
  private Definition definition;
  private int revision;
  private Status status;

  /**
   * Create a listview Namespace DTO from a regular Namespace DTO.
   * This method discards information and is not reversible.
   */
  public Namespace(de.dataelementhub.model.dto.element.Namespace fullNamespace) {
    this.urn = fullNamespace.getIdentification().getUrn();
    this.identifier = fullNamespace.getIdentification().getIdentifier();
    this.revision = fullNamespace.getIdentification().getRevision();
    this.status = fullNamespace.getIdentification().getStatus();
    this.definitions = fullNamespace.getDefinitions();
    this.definition = fullNamespace.getDefinition();
  }

}
