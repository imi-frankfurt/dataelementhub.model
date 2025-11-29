package de.dataelementhub.model.dto.element.section.validation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.persistence.oxm.annotations.XmlDiscriminatorNode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.io.Serializable;

/**
 * Numeric Validation DTO.
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type" // matches the JSON property "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = NumericInteger.class, name = "INTEGER"),
    @JsonSubTypes.Type(value = NumericFloat.class, name = "FLOAT")
})
@Data
@EqualsAndHashCode
@XmlAccessorType(XmlAccessType.FIELD)
@XmlDiscriminatorNode("type")
@XmlSeeAlso({NumericInteger.class, NumericFloat.class})
public abstract class Numeric implements Serializable {
  public static final String TYPE_INTEGER = "INTEGER";
  public static final String TYPE_FLOAT = "FLOAT";

  private String type;
  private Boolean useMinimum;
  private Boolean useMaximum;
  private String unitOfMeasure;
}
