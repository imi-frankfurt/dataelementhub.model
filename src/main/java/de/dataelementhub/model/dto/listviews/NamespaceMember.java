package de.dataelementhub.model.dto.listviews;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Namespace Member Listview DTO.
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class NamespaceMember {

  private ElementType elementType;
  private int identifier;
  private List<Definition> definitions;
  private int revision;
  private String validationType;
  private Status status;

  /**
   * Construct a listview dto of namespace member from an element.
   * The element can be either dataelement (requires another valuedomain element in this case),
   * dataelementgroup or record.
   */
  @Deprecated
  public NamespaceMember(Element fullElement, Element valueDomainElement) {
    if (fullElement.getClass() == DataElement.class) {
      DataElement fullDataElement = (DataElement) fullElement;
      this.elementType = fullDataElement.getIdentification().getElementType();
      this.identifier = fullDataElement.getIdentification().getIdentifier();
      this.revision = fullDataElement.getIdentification().getRevision();
      this.status = fullDataElement.getIdentification().getStatus();
      if (valueDomainElement != null) {
        this.validationType = ((ValueDomain) valueDomainElement).getType();
      }
      this.definitions = fullDataElement.getDefinitions();
    } else if (fullElement.getClass() == DataElementGroup.class) {
      DataElementGroup fullDataelementgroup = (DataElementGroup) fullElement;
      this.elementType = fullDataelementgroup.getIdentification().getElementType();
      this.identifier = fullDataelementgroup.getIdentification().getIdentifier();
      this.revision = fullDataelementgroup.getIdentification().getRevision();
      this.status = fullDataelementgroup.getIdentification().getStatus();
      this.definitions = fullDataelementgroup.getDefinitions();
    } else if (fullElement.getClass() == Record.class) {
      Record fullRecord = (Record) fullElement;
      this.elementType = fullRecord.getIdentification().getElementType();
      this.identifier = fullRecord.getIdentification().getIdentifier();
      this.revision = fullRecord.getIdentification().getRevision();
      this.status = fullRecord.getIdentification().getStatus();
      this.definitions = fullRecord.getDefinitions();
    }
  }


  /**
   * Filter definitions to only contain the requested languages.
   */
  public void applyLanguageFilter(String languages) {
    if (languages == null || languages.isEmpty()) {
      return;
    }
    List<Locale> locales = LanguageRange.parse(languages).stream()
        .map(range -> new Locale(range.getRange())).collect(
            Collectors.toList());

    List<String> requestedLanguages = locales.stream()
        .map(locale -> {
          String language;
          try {
            language = locale.getLanguage().split("-")[0].toLowerCase();
          } catch (Exception e) {
            language = locale.getLanguage().toLowerCase();
          }
          return language;
        })
        .collect(Collectors.toList());

    // If requested languages contains wildcard character - don't change anything
    if (requestedLanguages.contains("*")) {
      return;
    }

    List<String> availableLanguages = definitions.stream()
        .map(def -> def.getLanguage().toLowerCase())
        .collect(Collectors.toList());

    availableLanguages.retainAll(requestedLanguages);

    // If no matching languages are found - don't change anything
    if (availableLanguages.isEmpty()) {
      return;
    }

    List<Definition> filteredDefinitions = new ArrayList<>();

    definitions.forEach(def -> {
      if (availableLanguages.contains(def.getLanguage())) {
        filteredDefinitions.add(def);
      }
    });

    setDefinitions(filteredDefinitions);
  }
}
