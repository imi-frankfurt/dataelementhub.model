package de.dataelementhub.model.handler.element;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.IDENTIFIED_ELEMENT;

import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.handler.element.section.ConceptAssociationHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValueHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jooq.CloseableDSLContext;

public abstract class ElementHandler {

  /**
   * Get a Sub Element by its urn.
   */
  public static Element readSubElement(CloseableDSLContext ctx, int userId, String urn) {
    if (!IdentificationHandler.isUrn(urn)) {
      try {
        Namespace namespace = NamespaceHandler.getByIdentifier(ctx, userId, Integer.parseInt(urn));
        if (namespace == null) {
          throw new NoSuchElementException();
        } else {
          return namespace;
        }
      } catch (NumberFormatException e) {
        throw new NoSuchElementException();
      }
    } else {
      // Read other elements with proper urn
      Identification identification = IdentificationHandler.fromUrn(urn);
      if (identification == null) {
        throw new NoSuchElementException(urn);
      }
      switch (identification.getElementType()) {
        case DATAELEMENT:
          return DataElementHandler.get(ctx, userId, urn);
        case DATAELEMENTGROUP:
          return DataElementGroupHandler.get(ctx, userId, urn);
        case RECORD:
          return RecordHandler.get(ctx, userId, urn);
        case ENUMERATED_VALUE_DOMAIN:
        case DESCRIBED_VALUE_DOMAIN:
          return ValueDomainHandler.get(ctx, userId, urn);
        case PERMISSIBLE_VALUE:
          return PermittedValueHandler.get(ctx, userId, urn);
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
    }
  }

  /**
   * Get dataElementGroup or record members.
   */
  public static List<Member> readMembers(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    return MemberHandler.get(ctx, identification);
  }

  /**
   * Fetch a unique record that has <code>id = value</code>.
   */
  public static Element fetchOneByIdentification(CloseableDSLContext ctx, int userId,
      Identification identification) {

    if (identification == null) {
      return null;
    }

    IdentifiedElementRecord identifiedElementRecord = getIdentifiedElementRecord(
        ctx, identification);

    return convertToElement(ctx, identification, identifiedElementRecord);
  }

  /**
   * Get an identified element record by its identification.
   */
  public static IdentifiedElementRecord getIdentifiedElementRecord(CloseableDSLContext ctx,
      Identification identification) {
    return ctx.fetchOne(IDENTIFIED_ELEMENT,
        IDENTIFIED_ELEMENT.SI_IDENTIFIER.equal(identification.getIdentifier())
            .and(IDENTIFIED_ELEMENT.SI_VERSION
                .equal(identification.getRevision())
                .and(IDENTIFIED_ELEMENT.SI_NAMESPACE_ID.equal(identification.getNamespaceId()))
                .and(IDENTIFIED_ELEMENT.ELEMENT_TYPE.equal(identification.getElementType()))));
  }

  /**
   * Fetch a unique record that has <code>id = value</code>.
   */
  public static Element fetchOneByUrn(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    if (identification != null) {
      return fetchOneByIdentification(ctx, userId, identification);
    } else {
      return null;
    }
  }

  /**
   * Get an identified element record by its urn.
   */
  public static IdentifiedElementRecord getIdentifiedElementRecordByUrn(CloseableDSLContext ctx,
      String urn) {
    return getIdentifiedElementRecord(ctx, IdentificationHandler.fromUrn(urn));
  }

  /**
   * Get all elements from a list of urns.
   */
  public static List<Element> fetchByUrns(CloseableDSLContext ctx, int userId, List<String> urns) {
    List<Element> elements = new ArrayList<>();
    for (String urn : urns) {
      Element element = fetchOneByIdentification(ctx, userId, IdentificationHandler.fromUrn(urn));
      if (element != null) {
        elements.add(element);
      }
    }
    return elements;
  }

  /**
   * Convert an identified element record to an element.
   */
  public static Element convertToElement(CloseableDSLContext ctx, Identification identification,
      IdentifiedElementRecord identifiedElementRecord) {
    Element element = new Element();
    identification.setStatus(identifiedElementRecord.getSiStatus());
    element.setIdentification(identification);
    element.setDefinitions(DefinitionHandler
        .get(ctx, identifiedElementRecord.getId(), identifiedElementRecord.getSiId()));
    element.setSlots(SlotHandler.get(ctx, identification));
    return element;
  }

  /**
   * Save an element in the database.
   */
  public static int saveElement(CloseableDSLContext ctx,
                                de.dataelementhub.dal.jooq.tables.pojos.Element element) {

    return ctx.insertInto(ELEMENT)
            .set(ctx.newRecord(ELEMENT, element))
            .returning(ELEMENT.ID)
            .fetchOne().getId();
  }


  /**
   * Outdates or deletes the given element. Depending on the status of the element. Drafts are
   * deleted, released elements are outdated.
   */
  public static void delete(CloseableDSLContext ctx, int userId, String urn) {
    Element element = fetchOneByUrn(ctx, userId, urn);

    if (element == null) {
      throw new NoSuchElementException(urn);
    }

    switch (element.getIdentification().getStatus()) {
      case DRAFT:
      case STAGED:
        IdentificationHandler
            .deleteDraftIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case RELEASED:
        IdentificationHandler
            .outdateIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case OUTDATED:
      default:
        throw new IllegalStateException(
            "Can't delete/outdate element in state " + element.getIdentification().getStatus());
    }
  }


  /**
   * Outdates or deletes the given element. Depending on the status of the element. Drafts are
   * deleted, released elements are outdated.
   */
  public static void delete(CloseableDSLContext ctx, int userId, Identification identification) {
    Element element = fetchOneByIdentification(ctx, userId, identification);

    if (element == null) {
      throw new NoSuchElementException();
    }

    switch (element.getIdentification().getStatus()) {
      case DRAFT:
      case STAGED:
        IdentificationHandler
            .deleteDraftIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case RELEASED:
        IdentificationHandler
            .outdateIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case OUTDATED:
      default:
        throw new IllegalStateException();
    }
  }


  /**
   * Import the ScopedIdentifier and all related entries into the value domain namespace.
   * This imports the scoped identifier of the permitted value itself, all linked definitions,
   * slots and concept associations into the value domain namespace.
   */
  public static ScopedIdentifier importIntoParentNamespace(CloseableDSLContext ctx, int userId,
      int targetNamespaceId, String urn) {

    ScopedIdentifier sourceScopedIdentifier = IdentificationHandler
        .getScopedIdentifier(ctx, urn);

    ScopedIdentifier targetScopedIdentifier = IdentificationHandler
        .importToNamespace(ctx, userId, sourceScopedIdentifier, targetNamespaceId);

    // Copy definitions
    DefinitionHandler
        .copyDefinitions(ctx, sourceScopedIdentifier.getId(), targetScopedIdentifier.getId());

    // Copy Slots
    SlotHandler.copySlots(ctx, sourceScopedIdentifier.getId(), targetScopedIdentifier.getId());
    // Copy Concept associations
    ConceptAssociationHandler
        .copyConceptElementAssociations(ctx, userId, sourceScopedIdentifier.getId(),
            targetScopedIdentifier.getId());

    return targetScopedIdentifier;
  }

  /**
   * Check if the element can be created in this namespace.
   * Draft/staged namespaces can only contain draft/staged elements.
   */
  public static boolean statusMismatch(CloseableDSLContext ctx, int userId, Element element) {

    Namespace namespace = NamespaceHandler.getByUrn(ctx, userId,
        element.getIdentification().getNamespaceUrn());

    if (namespace == null || namespace.getIdentification() == null
        || namespace.getIdentification().getStatus() == null) {
      return true;
    }

    Status elementStatus = element.getIdentification().getStatus();
    Status namespaceStatus = namespace.getIdentification().getStatus();

    if (namespaceStatus.equals(Status.STAGED) || namespaceStatus.equals(Status.DRAFT)) {
      return !(elementStatus.equals(Status.STAGED) || elementStatus.equals(Status.DRAFT));
    } else {
      // released or outdated namespace can contain elements in any status
      return false;
    }
  }
}
