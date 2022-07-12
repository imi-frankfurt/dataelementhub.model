package de.dataelementhub.model.service;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.listviews.NamespaceMember;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * Namespace Service.
 */
@Service
public class NamespaceService {

  /**
   * Create a new Namespace and return its new ID.
   */
  public ScopedIdentifier create(DSLContext ctx, int userId, Element element)
      throws IllegalAccessException, IllegalArgumentException {
    // When creating new elements, remove user submitted values for identifier and revision. They
    // will be assigned by the backend.
    element.setIdentification(IdentificationHandler.removeUserSubmittedIdentifierAndRevision(
        element.getIdentification()));
    if (element.getIdentification().getElementType() == ElementType.NAMESPACE) {
      return NamespaceHandler.create(ctx, userId, (Namespace) element);
    }
    throw new IllegalArgumentException("Element Type is not supported");
  }

  /**
   * Get a Namespace by its identifier.
   */
  public Element read(DSLContext ctx, int userId, String identifier) {
    try {
      Namespace namespace = NamespaceHandler.getByIdentifier(ctx, userId,
          Integer.parseInt(identifier));
      if (namespace == null) {
        throw new NoSuchElementException();
      } else {
        return namespace;
      }
    } catch (NumberFormatException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get all Namespaces a user has access to.
   */
  public Map<AccessLevelType, List<Namespace>> readNamespaces(DSLContext ctx, int userId) {
    Map<AccessLevelType, List<Namespace>> namespaceMap = new HashMap<>();

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

    return namespaceMap;
  }

  /**
   * Get all Namespaces a user has the given access right to.
   */
  public Map<AccessLevelType, List<Namespace>> readNamespaces(DSLContext ctx, int userId,
      AccessLevelType accessLevel) throws IllegalAccessException {
    Map<AccessLevelType, List<Namespace>> namespaceMap = new HashMap<>();

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

    return namespaceMap;
  }

  /**
   * Get all Members of a given type from the given Namespace Scoped Identifier.
   */
  public List<Member> readNamespaceMembers(
      DSLContext ctx, int userId, Integer namespaceSiIdentifier,
      List<String> elementTypesString, Boolean hideSubElements) {
    try {

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
  public List<NamespaceMember> getNamespaceMembersListview(DSLContext ctx, int userId,
      Integer namespaceSiIdentifier, List<String> elementTypesString, Boolean hideSubElements) {
    try {

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
   * Read the list of users that have access to a given namespace.
   */
  public List<DeHubUserPermission> readUserAccessList(
      DSLContext ctx, int userId, int namespaceIdentifier)
      throws IllegalAccessException {
    return NamespaceHandler.getUserAccessForNamespace(ctx, userId, namespaceIdentifier);
  }

  /**
   * Update a Namespace and return its identification.
   */
  public Identification update(DSLContext ctx, int userId, Element element)
      throws IllegalAccessException, NoSuchMethodException {
    if (element.getIdentification().getElementType() == ElementType.NAMESPACE) {
      return NamespaceHandler.update(ctx, userId, (Namespace) element);
    }
    throw new IllegalArgumentException("Element Type is not supported");
  }

  /**
   * Delete an identifier with the given urn.
   */
  public void delete(DSLContext ctx, int userId, String urn) {
    if (!IdentificationHandler.isUrn(urn)) {
      throw new IllegalArgumentException();
    } else {
      Identification identification = IdentificationHandler.fromUrn(ctx, urn);
      if (identification == null) {
        throw new NoSuchElementException(urn);
      }
      ElementHandler.delete(ctx, userId, urn);
    }
  }

  /**
   * Release a Namespace.
   */
  public void release(DSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(ctx, urn);

    IdentificationHandler.canBeReleased(ctx, userId, identification);

    if (identification.getElementType() == ElementType.NAMESPACE) {
      IdentificationHandler.updateStatus(ctx, userId, identification, Status.RELEASED);
    } else {
      throw new IllegalArgumentException(
          "Element Type is not supported: " + identification.getElementType());
    }
  }
}
