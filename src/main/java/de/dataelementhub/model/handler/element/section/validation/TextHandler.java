package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Text;

public class TextHandler {

  /**
   * Convert a ValueDomain object of DataElementHub DAL to a Text object of DataElementHub Model.
   */
  public static Text convert(de.dataelementhub.dal.jooq.tables.pojos.Element valueDomain) {
    Text text = new Text();
    text.setUseRegEx(!valueDomain.getFormat().isEmpty());
    text.setRegEx(valueDomain.getFormat());

    if (valueDomain.getMaximumCharacters() > 0) {
      text.setUseMaximumLength(true);
      text.setMaximumLength(valueDomain.getMaximumCharacters());
    } else {
      text.setUseMaximumLength(false);
    }

    return text;
  }

  /**
   * Convert a Validation object of DataElementHub Model to a DescribedValueDomain object of
   * DataElementHub DAL.
   */
  public static de.dataelementhub.dal.jooq.tables.pojos.Element convert(ValueDomain validation) {
    de.dataelementhub.dal.jooq.tables.pojos.Element domain
        = new de.dataelementhub.dal.jooq.tables.pojos.Element();

    String regEx;
    if (validation.getText().getUseRegEx()) {
      regEx = validation.getText().getRegEx();
      domain.setValidationType(ValidationType.REGEX);
      domain.setValidationData(regEx);
    } else {
      regEx = "";
      domain.setValidationType(ValidationType.NONE);
      domain.setValidationData(null);
    }

    if (validation.getText().getUseMaximumLength()) {
      domain.setMaximumCharacters(validation.getText().getMaximumLength());
    } else {
      domain.setMaximumCharacters(0);
    }

    domain.setDescription(regEx);
    domain.setFormat(regEx);
    domain.setDatatype(ValueDomain.TYPE_STRING);
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }
}
