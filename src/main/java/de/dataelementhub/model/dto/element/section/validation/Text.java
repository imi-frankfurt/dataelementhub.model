package de.dataelementhub.model.dto.element.section.validation;

import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

@Data
public class Text implements Serializable {

  private Boolean useRegEx;
  private String regEx;
  private Boolean useMaximumLength;
  private Integer maximumLength;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Text text = (Text) o;

    boolean regexEquals;
    if (regEx == null || "".equals(regEx)) {
      regexEquals = (text.regEx == null || "".equals(text.regEx));
    } else if (text.regEx == null || "".equals(text.regEx)) {
      regexEquals = (regEx == null || "".equals(regEx));
    } else {
      regexEquals = Objects.equals(regEx, text.regEx);
    }

    return Objects.equals(useRegEx, text.useRegEx) && regexEquals && Objects
        .equals(useMaximumLength, text.useMaximumLength) && Objects
        .equals(maximumLength, text.maximumLength);
  }

  @Override
  public int hashCode() {
    return Objects.hash(useRegEx, regEx, useMaximumLength, maximumLength);
  }
}
