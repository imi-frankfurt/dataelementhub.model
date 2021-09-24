package de.dataelementhub.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Identification implements Serializable {

  private String urn;
  private String namespaceUrn;
  private Integer namespaceId;
  private ElementType elementType;
  private Integer identifier;
  private Integer revision;
  private Status status;
  private Boolean hideNamespace;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Identification that = (Identification) o;
    boolean namespaceEquals = Objects.equals(namespaceId, that.namespaceId)
        || Objects.equals(namespaceUrn, that.namespaceUrn);
    return elementType == that.elementType && namespaceEquals && status == that.status
        && Objects.equals(identifier, that.identifier) && Objects.equals(revision,
        that.revision) && Objects.equals(urn, that.urn) && Objects.equals(
        hideNamespace, that.hideNamespace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(elementType, namespaceId, namespaceUrn, status, identifier, revision, urn,
        hideNamespace);
  }
}
