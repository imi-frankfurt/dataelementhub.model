package de.dataelementhub.model.service;

import static de.dataelementhub.dal.jooq.Routines.getDefinitionByUrn;
import static de.dataelementhub.dal.jooq.Routines.getSlotByUrn;
import static de.dataelementhub.dal.jooq.Routines.getValueDomainScopedIdentifierByDataelementUrn;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.Slot;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.listviews.SimplifiedElementIdentification;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.ElementRelationHandler;
import de.dataelementhub.model.handler.element.DataElementGroupHandler;
import de.dataelementhub.model.handler.element.DataElementHandler;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.ElementPathHandler;
import de.dataelementhub.model.handler.element.RecordHandler;
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
import org.springframework.stereotype.Service;

@Service
public class ElementService {

  /**
   * Create a new Element and return its new ID.
   */
  public ScopedIdentifier create(CloseableDSLContext ctx, int userId, Element element)
      throws IllegalAccessException, IllegalArgumentException {

    // When creating new elements, remove user submitted values for identifier and revision. They
    // will be assigned by the backend.
    element.setIdentification(IdentificationHandler.removeUserSubmittedIdentifierAndRevision(
        element.getIdentification()));
    if (element.getIdentification().getElementType() != ElementType.NAMESPACE) {
      // check if namespace status and element status are compatible
      if (ElementHandler.statusMismatch(ctx, userId, element)) {
        throw new IllegalStateException("Unreleased namespaces can't contain released elements");
      }
    }
    switch (element.getIdentification().getElementType()) {
      case DATAELEMENT:
        return DataElementHandler.create(ctx, userId, (DataElement) element);
      case DATAELEMENTGROUP:
        return DataElementGroupHandler.create(ctx, userId, (DataElementGroup) element);
      case RECORD:
        return RecordHandler.create(ctx, userId, (Record) element);
      case ENUMERATED_VALUE_DOMAIN:
      case DESCRIBED_VALUE_DOMAIN:
        return ValueDomainHandler.create(ctx, userId, (ValueDomain) element);
      case PERMISSIBLE_VALUE:
        return PermittedValueHandler.create(ctx, userId, (PermittedValue) element);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /**
   * Get an Element by its urn.
   */
  public Element read(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(ctx, urn);
    if (identification == null) {
      throw new NoSuchElementException(urn);
    }
    switch (identification.getElementType()) {
      case DATAELEMENT:
        return DataElementHandler.get(ctx, userId, identification);
      case DATAELEMENTGROUP:
        return DataElementGroupHandler.get(ctx, userId, identification);
      case RECORD:
        return RecordHandler.get(ctx, userId, identification);
      case ENUMERATED_VALUE_DOMAIN:
      case DESCRIBED_VALUE_DOMAIN:
        return ValueDomainHandler.get(ctx, userId, identification);
      case PERMISSIBLE_VALUE:
        return PermittedValueHandler.get(ctx, userId, identification);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /**
   * Get the ValueDomain of an Element by the elements urn.
   */
  public Element readValueDomain(CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      ScopedIdentifier valueDomainScopedIdentifier = ctx
          .selectQuery(getValueDomainScopedIdentifierByDataelementUrn(elementUrn))
          .fetchOneInto(ScopedIdentifier.class);
      return read(ctx, userId, IdentificationHandler.toUrn(ctx, valueDomainScopedIdentifier));
    }
  }


  /**
   * Get the Definitions of an Element by the elements urn.
   */
  public List<Definition> readDefinitions(CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitions = ctx
          .selectQuery(getDefinitionByUrn(elementUrn))
          .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.Definition.class);
      List<Definition> definitionsDto = new ArrayList<>();
      definitions.forEach(d -> definitionsDto.add(DefinitionHandler.convert(d)));
      return definitionsDto;
    }
  }

  /**
   * Get the Slots of an Element by the elements urn.
   */
  public List<Slot> readSlots(CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      List<de.dataelementhub.dal.jooq.tables.pojos.Slot> slots = ctx
          .selectQuery(getSlotByUrn(elementUrn))
          .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.Slot.class);
      List<Slot> slotsDto = new ArrayList<>();
      slots.forEach(s -> slotsDto.add(SlotHandler.convert(s)));
      return slotsDto;
    }
  }

  /**
   * Get the Identification of an Element by the elements urn.
   */
  public Identification readIdentification(CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      ScopedIdentifier scopedIdentifier = IdentificationHandler
          .getScopedIdentifier(ctx, elementUrn);
      return IdentificationHandler.convert(ctx, scopedIdentifier);
    }
  }

  /**
   * Get the Concept Associations of an Element by the elements urn.
   */
  public List<ConceptAssociation> readConceptAssociations(
      CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      return ConceptAssociationHandler.get(ctx, elementUrn);
    }
  }


  /**
   * Get the Relations of an Element by the elements urn.
   */
  public List<de.dataelementhub.model.dto.ElementRelation> readRelations(
      CloseableDSLContext ctx, int userId, String elementUrn) {
    if (!IdentificationHandler.isUrn(elementUrn)) {
      throw new IllegalArgumentException("Not a URN: " + elementUrn);
    } else {
      List<de.dataelementhub.model.dto.ElementRelation> elementRelations =
          ElementRelationHandler.getElementRelations(ctx, elementUrn, null);
      return elementRelations;
    }
  }

  /**
   * Get dataElementGroup or record members.
   */
  public List<Member> readMembers(CloseableDSLContext ctx, int userId, String urn) {
    try {
      Identification identification = IdentificationHandler.fromUrn(ctx, urn);
      List<Member> members = MemberHandler.get(ctx, identification);
      return members;
    } catch (NumberFormatException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Update an Element and return its urn.
   */
  public Identification update(CloseableDSLContext ctx, int userId, Element element)
      throws IllegalAccessException, NoSuchMethodException {
    if (element.getIdentification().getElementType() != ElementType.NAMESPACE) {
      // check if namespace status and element status are compatible
      if (ElementHandler.statusMismatch(ctx, userId, element)) {
        throw new IllegalStateException("Unreleased namespaces can't contain released elements");
      }
    }
    switch (element.getIdentification().getElementType()) {
      case DATAELEMENT:
        return DataElementHandler.update(ctx, userId, (DataElement) element);
      case DATAELEMENTGROUP:
        return DataElementGroupHandler.update(ctx, userId, (DataElementGroup) element);
      case RECORD:
        return RecordHandler.update(ctx, userId, (Record) element);
      case DESCRIBED_VALUE_DOMAIN:
      case ENUMERATED_VALUE_DOMAIN:
        return ValueDomainHandler.update(ctx, userId, (ValueDomain) element);
      case PERMISSIBLE_VALUE:
        return PermittedValueHandler.update(ctx, userId, (PermittedValue) element);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /**
   * Delete an identifier with the given urn.
   */
  public void delete(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(ctx, urn);
    if (identification == null) {
      throw new NoSuchElementException(urn);
    }
    ElementHandler.delete(ctx, userId, urn);
  }

  /**
   * Release an Element.
   */
  public void release(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(ctx, urn);

    if (IdentificationHandler.canBeReleased(ctx, userId, identification)) {
      switch (identification.getElementType()) {
        case DATAELEMENT:
          IdentificationHandler.updateStatus(ctx, userId, identification, Status.RELEASED);
          break;
        case DATAELEMENTGROUP:
        case RECORD:
          if (MemberHandler.allSubIdsAreReleased(ctx, identification)) {
            IdentificationHandler.updateStatus(ctx, userId, identification, Status.RELEASED);
          } else {
            throw new IllegalStateException("Not all members are released");
          }
          break;
        default:
          throw new IllegalArgumentException(
              "Element Type is not supported: " + identification.getElementType());
      }
    } else {
      throw new IllegalStateException("Namespace is not released. Element can not be released.");
    }
  }

  /**
   * Update dataElementGroup or record members and return urn.
   *
   * @return the new urn if at least one member has new version - otherwise return the old one.
   */
  public String updateMembers(CloseableDSLContext ctx, int userId, String urn)
      throws IllegalAccessException, IllegalArgumentException {
    ScopedIdentifier scopedIdentifier = IdentificationHandler.getScopedIdentifier(ctx, urn);
    AccessLevelType accessLevel = AccessLevelHandler
        .getAccessLevelByUserAndNamespaceId(ctx, userId, scopedIdentifier.getNamespaceId());
    if (!DaoUtil.WRITE_ACCESS_TYPES.contains(accessLevel)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }
    switch (scopedIdentifier.getElementType()) {
      case DATAELEMENTGROUP:
        return DataElementGroupHandler.updateMembers(ctx, userId, scopedIdentifier).getUrn();
      case RECORD:
        return RecordHandler.updateMembers(ctx, userId, scopedIdentifier).getUrn();
      default:
        throw new IllegalArgumentException("Element Type is not supported. "
            + "Only dataELementGroup and record are accepted!");
    }
  }

  /**
   * Get all available paths for a given element.
   */
  public List<List<SimplifiedElementIdentification>> getElementPaths(
      CloseableDSLContext ctx, int userId, String urn, String languages)
      throws IllegalArgumentException, IllegalStateException {
    if (IdentificationHandler.getScopedIdentifier(ctx, urn).getStatus() == Status.OUTDATED) {
      throw new IllegalStateException(urn + " is OUTDATED.");
    }
    return ElementPathHandler.getElementPaths(ctx, userId, urn, languages);
  }
}
