package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.NumericFloat;

/**
 * NumericFloat Handler.
 */
public class NumericFloatHandler {

  /**
   * Convert a ValueDomain object of DataElementHub DAL to a Numeric object of DataElementHub
   * Model.
   */
  public static NumericFloat convert(Element valueDomain) {
    NumericFloat numericFloat = new NumericFloat();
    numericFloat.setUnitOfMeasure(valueDomain.getUnitOfMeasure());

    String[] parts = valueDomain.getFormat().split("<=");
    if (parts.length == 1) {
      numericFloat.setUseMinimum(false);
      numericFloat.setUseMaximum(false);
    } else if (parts.length == 2) {
      if (parts[0].equals("x")) {
        numericFloat.setUseMinimum(false);
        numericFloat.setUseMaximum(true);
        numericFloat.setMaximum(Double.valueOf(parts[1]));
      } else {
        numericFloat.setUseMinimum(true);
        numericFloat.setUseMaximum(false);
        numericFloat.setMinimum(Double.valueOf(parts[0]));
      }
    } else if (parts.length == 3) {
      numericFloat.setUseMinimum(true);
      numericFloat.setUseMaximum(true);
      numericFloat.setMinimum(Double.valueOf(parts[0]));
      numericFloat.setMaximum(Double.valueOf(parts[2]));
    }

    return numericFloat;
  }

  /**
   * Convert a Validation object of DataElementHub Model to a DescribedValueDomain object of
   * DataElementHub DAL.
   */
  public static Element convert(ValueDomain validation) {
    Element domain = new Element();
    NumericFloat numericFloat = (NumericFloat) validation.getNumeric();

    if (numericFloat == null) {
      numericFloat = new NumericFloat();
    }

    String min = String.valueOf(numericFloat.getMinimum());
    String max = String.valueOf(numericFloat.getMaximum());
    String validationData = "x";

    // Convert null in use min and use max to false. Also set use minimum to false if the minimum is
    // null. vice versa for max.
    if (numericFloat.getUseMaximum() == null || numericFloat.getMaximum() == null) {
      numericFloat.setUseMaximum(Boolean.FALSE);
    }
    if (numericFloat.getUseMinimum() == null || numericFloat.getMinimum() == null) {
      numericFloat.setUseMinimum(Boolean.FALSE);
    }

    if (numericFloat.getUseMinimum() || numericFloat.getUseMaximum()) {
      domain.setValidationType(ValidationType.valueOf(validation.getNumeric().getType() + "RANGE"));
      domain.setMaximumCharacters(Math.max(min.length(), max.length()));

      if (numericFloat.getUseMinimum()) {
        validationData = min + "<=" + validationData;
      }

      if (numericFloat.getUseMaximum()) {
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
    domain.setUnitOfMeasure(numericFloat.getUnitOfMeasure());
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }

}
