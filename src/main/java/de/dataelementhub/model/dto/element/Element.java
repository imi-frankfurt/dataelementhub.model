package de.dataelementhub.model.dto.element;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Slot;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Element DTO.
 */
@Data
@EqualsAndHashCode
@JsonInclude(Include.NON_NULL)
public class Element implements Serializable {

  private Identification identification;
  private List<Definition> definitions;
  private List<Slot> slots;

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
