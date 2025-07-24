package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import java.util.Objects;

/**
 * Numeric Handler.
 */
public class NumericHandler {

  /**
   * Convert a ValueDomain object of DataElementHub DAL to a Numeric object of DataElementHub
   * Model.
   */
  public static Numeric convert(Element valueDomain) {
    if (valueDomain.getValidationType() == ValidationType.FLOAT
        || valueDomain.getValidationType() == ValidationType.FLOATRANGE) {
      return NumericFloatHandler.convert(valueDomain);
    } else if (valueDomain.getValidationType() == ValidationType.INTEGER
        || valueDomain.getValidationType() == ValidationType.INTEGERRANGE) {
      return NumericIntegerHandler.convert(valueDomain);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Convert a Validation object of DataElementHub Model to a DescribedValueDomain object of
   * DataElementHub DAL.
   */
  public static Element convert(ValueDomain validation) {
    if (Objects.equals(validation.getNumeric().getType(), Numeric.TYPE_FLOAT)) {
      return NumericFloatHandler.convert(validation);
    } else if (Objects.equals(validation.getNumeric().getType(), Numeric.TYPE_INTEGER)) {
      return NumericIntegerHandler.convert(validation);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
