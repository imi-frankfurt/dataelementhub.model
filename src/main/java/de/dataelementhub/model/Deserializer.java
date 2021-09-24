package de.dataelementhub.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dataelementhub.model.adapter.NumericValidationAdapter;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Deserializer {

  /**
   * Deserialize JSON elements into the corresponding class.
   * Expect Identification Object in JSON
   */
  public static Element getElement(String content) {
    Gson gson = new GsonBuilder().create();
    try {
      return getElement(content, gson.fromJson(content, Element.class).getIdentification());
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /**
   * Deserialize JSON elements into the corresponding class.
   * Supply Identification object externally.
   */
  public static Element getElement(String content, Identification identification) {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeHierarchyAdapter(Numeric.class, new NumericValidationAdapter());
    gsonBuilder.disableHtmlEscaping();
    Gson gson = gsonBuilder.create();


    switch (identification.getElementType()) {
      case NAMESPACE:
        return gson.fromJson(content, Namespace.class);
      case DATAELEMENT:
        return gson.fromJson(content, DataElement.class);
      case DATAELEMENTGROUP:
        return gson.fromJson(content, DataElementGroup.class);
      case RECORD:
        return gson.fromJson(content, Record.class);
      case DESCRIBED_VALUE_DOMAIN:
      case ENUMERATED_VALUE_DOMAIN:
        return gson.fromJson(content, ValueDomain.class);
      case PERMISSIBLE_VALUE:
        return gson.fromJson(content, PermittedValue.class);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }
}
