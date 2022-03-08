package de.dataelementhub.model.service;

import static de.dataelementhub.dal.jooq.Routines.getDefinitionByUrn;
import static de.dataelementhub.dal.jooq.Routines.getSlotByUrn;
import static de.dataelementhub.dal.jooq.Routines.getValueDomainScopedIdentifierByDataelementUrn;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.Slot;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.listviews.NamespaceMember;
import de.dataelementhub.model.dto.listviews.SimplifiedElementIdentification;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.ElementRelationHandler;
import de.dataelementhub.model.handler.element.DataElementGroupHandler;
import de.dataelementhub.model.handler.element.DataElementHandler;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.ElementPathHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.RecordHandler;
import de.dataelementhub.model.handler.element.section.ConceptAssociationHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValueHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.jooq.CloseableDSLContext;
import org.springframework.stereotype.Service;

@Service
public class ElementService {

  /**
   * Create a new Element and return its new ID.
   */
  public ScopedIdentifier create(int userId, Element element)
      throws IllegalAccessException, IllegalArgumentException {

    // When creating new elements, remove user submitted values for identifier and revision. They
    // will be assigned by the backend.
    element.setIdentification(IdentificationHandler.removeUserSubmittedIdentifierAndRevision(
        element.getIdentification()));
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (element.getIdentification().getElementType() != ElementType.NAMESPACE) {
        // check if namespace status and element status are compatible
        if (ElementHandler.statusMismatch(ctx, userId, element)) {
          throw new IllegalStateException("Unreleased namespaces can't contain released elements");
        }
      }
      switch (element.getIdentification().getElementType()) {
        case NAMESPACE:
          return NamespaceHandler.create(ctx, userId, (Namespace) element);
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
  }

  /**
   * Get an Element by its urn.
   */
  public Element read(int userId, String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(urn)) {
        try {
          Namespace namespace = NamespaceHandler.getByIdentifier(ctx, userId,
              Integer.parseInt(urn));
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
  }

  /**
   * Get the ValueDomain of an Element by the elements urn.
   */
  public Element readValueDomain(int userId, String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(elementUrn)) {
        throw new IllegalArgumentException("Not a URN: " + elementUrn);
      } else {
        ScopedIdentifier valueDomainScopedIdentifier = ctx
            .selectQuery(getValueDomainScopedIdentifierByDataelementUrn(elementUrn))
            .fetchOneInto(ScopedIdentifier.class);
        return read(userId, IdentificationHandler.toUrn(valueDomainScopedIdentifier));
      }
    }
  }


  /**
   * Get the Definitions of an Element by the elements urn.
   */
  public List<Definition> readDefinitions(int userId, String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
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
  }

  /**
   * Get the Slots of an Element by the elements urn.
   */
  public List<Slot> readSlots(int userId, String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
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
  }

  /**
   * Get the Identification of an Element by the elements urn.
   */
  public Identification readIdentification(int userId, String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(elementUrn)) {
        throw new IllegalArgumentException("Not a URN: " + elementUrn);
      } else {
        ScopedIdentifier scopedIdentifier = IdentificationHandler
            .getScopedIdentifier(ctx, elementUrn);
        return IdentificationHandler.convert(scopedIdentifier);
      }
    }
  }

  /**
   * Get the Concept Associations of an Element by the elements urn.
   */
  public List<ConceptAssociation> readConceptAssociations(int userId, String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(elementUrn)) {
        throw new IllegalArgumentException("Not a URN: " + elementUrn);
      } else {
        return ConceptAssociationHandler.get(ctx, elementUrn);
      }
    }
  }


  /**
   * Get the Relations of an Element by the elements urn.
   */
  public List<de.dataelementhub.model.dto.ElementRelation> readRelations(int userId,
      String elementUrn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(elementUrn)) {
        throw new IllegalArgumentException("Not a URN: " + elementUrn);
      } else {
        List<de.dataelementhub.model.dto.ElementRelation> elementRelations =
            ElementRelationHandler.getElementRelations(ctx, elementUrn, null);
        return elementRelations;
      }
    }
  }

  /**
   * Get all Namespaces a user has access to.
   */
  public Map<AccessLevelType, List<Namespace>> readNamespaces(int userId) {
    Map<AccessLevelType, List<Namespace>> namespaceMap = new HashMap<>();

    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (userId < 0) {
        // Unauthorized user can only get read access on public (as in "non-hidden") namespaces
        namespaceMap.put(AccessLevelType.READ, NamespaceHandler.getPublicNamespaces(ctx));
      } else {
        List<Namespace> publicNamespaces = NamespaceHandler.getPublicNamespaces(ctx);
        List<Namespace> readableNamespaces = NamespaceHandler
            .getExplicitlyReadableNamespaces(ctx, userId);
        List<Namespace> writableNamespaces = NamespaceHandler.getWritableNamespaces(ctx, userId);
        List<Namespace> adminNamespaces = NamespaceHandler.getAdministrableNamespaces(ctx, userId);

        // Add public namespaces that are not in any other list to the readable list
        for (Namespace pn : publicNamespaces) {
          if (readableNamespaces.stream().anyMatch(
              ns -> ns.getIdentification().getUrn().equals(pn.getIdentification().getUrn()))
              || writableNamespaces.stream().anyMatch(
                ns -> ns.getIdentification().getUrn().equals(pn.getIdentification().getUrn()))
              || adminNamespaces.stream().anyMatch(
                ns -> ns.getIdentification().getUrn().equals(pn.getIdentification().getUrn()))) {
            continue;
          }
          readableNamespaces.add(pn);
        }

        namespaceMap.put(AccessLevelType.READ, readableNamespaces);
        namespaceMap.put(AccessLevelType.WRITE, writableNamespaces);
        namespaceMap.put(AccessLevelType.ADMIN, adminNamespaces);
      }
    }

    return namespaceMap;
  }

  /**
   * Get all Namespaces a user has the given access right to.
   */
  public Map<AccessLevelType, List<Namespace>> readNamespaces(int userId,
      AccessLevelType accessLevel) throws IllegalAccessException {
    Map<AccessLevelType, List<Namespace>> namespaceMap = new HashMap<>();

    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (userId < 0) {
        if (accessLevel != AccessLevelType.READ) {
          throw new IllegalAccessException("User not logged in");
        }
        // Unauthorized user can only get read access on public (as in "non-hidden") namespaces
        namespaceMap.put(AccessLevelType.READ, NamespaceHandler.getPublicNamespaces(ctx));
      } else {
        switch (accessLevel) {
          case ADMIN:
            namespaceMap.put(accessLevel, NamespaceHandler.getAdministrableNamespaces(ctx, userId));
            break;
          case WRITE:
            namespaceMap.put(accessLevel, NamespaceHandler.getWritableNamespaces(ctx, userId));
            break;
          case READ:
          default:
            List<Namespace> publicNamespaces = NamespaceHandler.getPublicNamespaces(ctx);
            List<Namespace> readableNamespaces = NamespaceHandler
                .getExplicitlyReadableNamespaces(ctx, userId);

            // Add public namespaces that are not in any other list to the readable list
            for (Namespace pn : publicNamespaces) {
              if (readableNamespaces.stream().anyMatch(
                  ns -> ns.getIdentification().getUrn().equals(pn.getIdentification().getUrn()))) {
                continue;
              }
              readableNamespaces.add(pn);
            }
            namespaceMap.put(accessLevel, readableNamespaces);
            break;
        }
      }
    }

    return namespaceMap;
  }

  /**
   * Get all Members of a given type from the given Namespace Scoped Identifier Identifier.
   */
  public List<Member> readNamespaceMembers(int userId, Integer namespaceSiIdentifier,
      List<String> elementTypesString, Boolean hideSubElements) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {

      List<ElementType> elementTypes = new ArrayList<>();
      if (elementTypesString != null && !elementTypesString.isEmpty()) {
        elementTypesString.forEach(et -> elementTypes.add(ElementType.valueOf(et.toUpperCase())));
      }
      return NamespaceHandler.getNamespaceMembers(ctx, userId, namespaceSiIdentifier,
          elementTypes, hideSubElements);
    } catch (NumberFormatException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get Namespace members in listview representation.
   */
  public List<NamespaceMember> getNamespaceMembersListview(int userId,
      Integer namespaceSiIdentifier, List<String> elementTypesString, Boolean hideSubElements) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {

      List<ElementType> elementTypes = new ArrayList<>();
      if (elementTypesString != null && !elementTypesString.isEmpty()) {
        elementTypesString.forEach(et -> elementTypes.add(ElementType.valueOf(et.toUpperCase())));
      }
      return NamespaceHandler.getNamespaceMembersListview(ctx, userId, namespaceSiIdentifier,
          elementTypes, hideSubElements);
    } catch (NumberFormatException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get dataElementGroup or record members.
   */
  public List<Member> readMembers(int userId, String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      Identification identification = IdentificationHandler.fromUrn(urn);
      List<Member> members = MemberHandler.get(ctx, identification);
      return members;
    } catch (NumberFormatException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Read the list of users that have access to a given namespace.
   */
  public List<DeHubUserPermission> readUserAccessList(int userId, int namespaceIdentifier)
      throws IllegalAccessException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return NamespaceHandler.getUserAccessForNamespace(ctx, userId, namespaceIdentifier);
    }
  }

  /**
   * Update an Element and return its urn.
   */
  public Identification update(int userId, Element element)
      throws IllegalAccessException, NoSuchMethodException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (element.getIdentification().getElementType() != ElementType.NAMESPACE) {
        // check if namespace status and element status are compatible
        if (ElementHandler.statusMismatch(ctx, userId, element)) {
          throw new IllegalStateException("Unreleased namespaces can't contain released elements");
        }
      }
      switch (element.getIdentification().getElementType()) {
        case NAMESPACE:
          return NamespaceHandler.update(ctx, userId, (Namespace) element);
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
  }

  /**
   * Delete an identifier with the given urn.
   */
  public void delete(int userId, String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(urn)) {
        throw new IllegalArgumentException();
      } else {
        Identification identification = IdentificationHandler.fromUrn(urn);
        if (identification == null) {
          throw new NoSuchElementException(urn);
        }
        ElementHandler.delete(ctx, userId, urn);
      }
    }
  }

  /**
   * Release an Element.
   */
  public void release(int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);

    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (IdentificationHandler.canBeReleased(ctx, userId, identification)) {
        switch (identification.getElementType()) {
          case NAMESPACE:
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
  }

  /**
   * Update dataElementGroup or record members and return urn.
   *
   * @return the new urn if at least one member has new version - otherwise return the old one.
   */
  public String updateMembers(int userId, String urn)
      throws IllegalAccessException, IllegalArgumentException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
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
  }

  /**
   * Get all available paths for a given element.
   */
  public List<List<SimplifiedElementIdentification>> getElementPaths(
      int userId, String urn, String languages)
      throws IllegalArgumentException, IllegalStateException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (IdentificationHandler.getScopedIdentifier(ctx, urn).getStatus() == Status.OUTDATED) {
        throw new IllegalStateException(urn + " is OUTDATED.");
      }
      return ElementPathHandler.getElementPaths(ctx, userId, urn, languages);
    }
  }
}
