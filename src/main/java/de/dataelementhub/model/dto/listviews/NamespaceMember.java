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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class NamespaceMember {

  private ElementType elementType;
  private int identifier;
  private List<Definition> definitions;
  private Definition definition;
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
        this.validationType = ((ValueDomain)valueDomainElement).getType();
      }
      this.definitions = fullDataElement.getDefinitions();
      this.definition = fullDataElement.getDefinition();
    } else if (fullElement.getClass() == DataElementGroup.class) {
      DataElementGroup fullDataelementgroup = (DataElementGroup) fullElement;
      this.elementType = fullDataelementgroup.getIdentification().getElementType();
      this.identifier = fullDataelementgroup.getIdentification().getIdentifier();
      this.revision = fullDataelementgroup.getIdentification().getRevision();
      this.status = fullDataelementgroup.getIdentification().getStatus();
      this.definitions = fullDataelementgroup.getDefinitions();
      this.definition = fullDataelementgroup.getDefinition();
    } else if (fullElement.getClass() == Record.class) {
      Record fullRecord = (Record) fullElement;
      this.elementType = fullRecord.getIdentification().getElementType();
      this.identifier = fullRecord.getIdentification().getIdentifier();
      this.revision = fullRecord.getIdentification().getRevision();
      this.status = fullRecord.getIdentification().getStatus();
      this.definitions = fullRecord.getDefinitions();
      this.definition = fullRecord.getDefinition();
    }
  }


  /**
   * Limit the amount of definitions to 1.
   * Check which of the wanted languages is present and keep that one. If none of those is present,
   * return the first one.
   */
  public void applyLanguageFilter(String languages) {
    if (languages == null || languages.isEmpty()) {
      return;
    }
    List<Locale> locales = LanguageRange.parse(languages).stream().sorted(
            Comparator.comparing(LanguageRange::getWeight).reversed())
        .map(range -> new Locale(range.getRange())).collect(
            Collectors.toList());

    Definition preferredDefinition = null;
    for (Locale locale : locales) {
      String language;
      try {
        language = locale.getLanguage().split("-")[0];
      } catch (Exception e) {
        language = locale.getLanguage();
      }

      // If the wildcard symbol is next in line, leave the for loop and return the first language
      // found.
      if (language.equals("*")) {
        break;
      }

      // Variable used in lambda expression should be final or effectively final
      final String finalLanguage = language;
      preferredDefinition = definitions.stream()
          .filter(d -> d.getLanguage().equalsIgnoreCase(finalLanguage))
          .findFirst().orElse(null);
      if (preferredDefinition != null) {
        setDefinition(preferredDefinition);
        break;
      }
    }

    // If none of the requested languages was found, keep the first preferredDefinition instead
    if (preferredDefinition == null && !definitions.isEmpty()) {
      setDefinition(definitions.get(0));
    }
    definitions = null;
  }
}
