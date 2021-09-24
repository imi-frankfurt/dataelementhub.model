package de.dataelementhub.model.dto.element;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Slot;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonInclude(Include.NON_NULL)
public class Element implements Serializable {

  private Identification identification;
  private List<Definition> definitions;
  private Definition definition;
  private List<Slot> slots;

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
    if (preferredDefinition == null) {
      setDefinition(definitions.get(0));
    }
    definitions = null;
  }
}
