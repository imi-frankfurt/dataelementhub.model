package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.NumericInteger;

/**
 * Numeric Integer Handler.
 */
public class NumericIntegerHandler {

  /**
   * Convert a ValueDomain object of DataElementHub DAL to a Numeric object of DataElementHub
   * Model.
   */
  public static Numeric convert(Element valueDomain) {
    NumericInteger numericInteger = new NumericInteger();
    numericInteger.setUnitOfMeasure(valueDomain.getUnitOfMeasure());

    String[] parts = valueDomain.getFormat().split("<=");
    if (parts.length == 1) {
      numericInteger.setUseMinimum(false);
      numericInteger.setUseMaximum(false);
    } else if (parts.length == 2) {
      if (parts[0].equals("x")) {
        numericInteger.setUseMinimum(false);
        numericInteger.setUseMaximum(true);
        numericInteger.setMaximum(Long.valueOf(parts[1]));
      } else {
        numericInteger.setUseMinimum(true);
        numericInteger.setUseMaximum(false);
        numericInteger.setMinimum(Long.valueOf(parts[0]));
      }
    } else if (parts.length == 3) {
      numericInteger.setUseMinimum(true);
      numericInteger.setUseMaximum(true);
      numericInteger.setMinimum(Long.valueOf(parts[0]));
      numericInteger.setMaximum(Long.valueOf(parts[2]));
    }

    return numericInteger;
  }

  /**
   * Convert a Validation object of DataElementHub Model to a DescribedValueDomain object of
   * DataElementHub DAL.
   */
  public static Element convert(ValueDomain validation) {
    Element domain = new Element();
    NumericInteger numericInteger = (NumericInteger) validation.getNumeric();

    if (numericInteger == null) {
      numericInteger = new NumericInteger();
    }

    String min = String.valueOf(numericInteger.getMinimum());
    String max = String.valueOf(numericInteger.getMaximum());
    String validationData = "x";

    // Convert null in use min and use max to false. Also set use minimum to false if the minimum is
    // null. vice versa for max.
    if (numericInteger.getUseMaximum() == null || numericInteger.getMaximum() == null) {
      numericInteger.setUseMaximum(Boolean.FALSE);
    }
    if (numericInteger.getUseMinimum() == null || numericInteger.getMinimum() == null) {
      numericInteger.setUseMinimum(Boolean.FALSE);
    }

    if (numericInteger.getUseMinimum() || numericInteger.getUseMaximum()) {
      domain.setValidationType(ValidationType.valueOf(validation.getNumeric().getType() + "RANGE"));
      domain.setMaximumCharacters(Math.max(min.length(), max.length()));

      if (numericInteger.getUseMinimum()) {
        validationData = min + "<=" + validationData;
      }

      if (numericInteger.getUseMaximum()) {
        validationData = validationData + "<=" + max;
      }
    } else {
      domain.setValidationType(ValidationType.valueOf(validation.getNumeric().getType()));
      domain.setMaximumCharacters(0);
    }

    domain.setValidationData(validationData);
    domain.setDescription(validationData);
    domain.setFormat(validationData);
    domain.setDatatype(validation.getType());
    domain.setUnitOfMeasure(numericInteger.getUnitOfMeasure());
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }

}
