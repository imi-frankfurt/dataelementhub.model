package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Datetime;

public class DatetimeHandler {

  /**
   * Convert an Element object of DataElementHub DAL to a Datetime object of DataElementHub Model.
   */
  public static Datetime convert(Element valueDomain) {
    Datetime datetime = new Datetime();
    datetime.setHourFormat(valueDomain.getValidationData().contains("HOURS_24") ? "24h" : "12h");

    String type = valueDomain.getDatatype();
    if (type.equals(ValueDomain.TYPE_DATETIME)) {
      String[] parts = valueDomain.getFormat().split(" ");
      datetime.setDate(parts[0]);
      datetime.setTime(parts[1]);
    } else if (type.equals(ValueDomain.TYPE_DATE)) {
      datetime.setDate(valueDomain.getFormat());
      datetime.setTime(null);
      datetime.setHourFormat("");
    } else {
      datetime.setDate(null);
      datetime.setTime(valueDomain.getFormat());
    }

    return datetime;
  }

  /**
   * Convert a Validation object of DataElementHub Model to an Element object of DataElementHub DAL.
   */
  public static Element convert(ValueDomain validation) {
    Element domain = new Element();
    Datetime datetime = validation.getDatetime();
    String format = "";
    String validationData = "";

    switch (validation.getType()) {
      case ValueDomain.TYPE_DATE:
        format = datetime.getDate();
        break;
      case ValueDomain.TYPE_DATETIME:
        format = datetime.getDate() + " " + datetime.getTime();
        break;
      case ValueDomain.TYPE_TIME:
        format = datetime.getTime();
        break;
      default:
        break;
    }

    domain.setDatatype(validation.getType());
    domain.setValidationType(ValidationType.valueOf(validation.getType()));
    domain.setFormat(format);
    domain.setDescription(format);
    domain.setMaximumCharacters(format.length());
    domain.setValidationData(buildValidationData(validation));
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }

  private static String buildValidationData(ValueDomain validation) {
    String validationData = "";
    Datetime datetime = validation.getDatetime();

    if (validation.getType().equals(ValueDomain.TYPE_DATE)
        || validation.getType().equals(ValueDomain.TYPE_DATETIME)) {
      validationData = datetime.getDate().contains("YYYY-MM") ? "ISO_8601" : "DIN_5008";
      validationData += datetime.getDate().contains("DD") ? "_WITH_DAYS" : "";
    }

    if (validation.getType().equals(ValueDomain.TYPE_DATETIME)) {
      validationData += ";";
    }

    if (validation.getType().equals(ValueDomain.TYPE_TIME)
        || validation.getType().equals(ValueDomain.TYPE_DATETIME)) {
      validationData += datetime.getHourFormat().equals("24h") ? "HOURS_24" : "HOURS_12";
      validationData += datetime.getTime().contains("ss") ? "_WITH_SECONDS" : "";
    }

    return validationData;
  }

}
