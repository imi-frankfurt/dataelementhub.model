package de.dataelementhub.model.handler.element;

import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER_HIERARCHY;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.listviews.SimplifiedElementIdentification;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.CloseableDSLContext;

/**
 * Element Path Handler.
 */
public class ElementPathHandler {

  /**
   * Get all available paths for a given element.
   */
  public static List<List<SimplifiedElementIdentification>> getElementPaths(
      CloseableDSLContext ctx, int userId, String urn, String languages) {
    List<List<SimplifiedElementIdentification>> elementPaths = new ArrayList<>();
    List<List<String>> pathUrnsList = completePaths(ctx, userId,
        Collections.singletonList(Collections.singletonList(urn)));
    List<List<String>> pathDesignationsList = getDesignations(ctx, pathUrnsList, languages);
    for (int i = 0; i < pathUrnsList.size(); i++) {
      List<SimplifiedElementIdentification> elementPath = new ArrayList<>();
      for (int e = 0; e < pathUrnsList.get(i).size(); e++) {
        SimplifiedElementIdentification simplifiedElementIdentification =
            new SimplifiedElementIdentification();
        simplifiedElementIdentification.setUrn(pathUrnsList.get(i).get(e));
        simplifiedElementIdentification.setDesignation(pathDesignationsList.get(i).get(e));
        elementPath.add(simplifiedElementIdentification);
      }
      elementPaths.add(elementPath);
    }
    return elementPaths;
  }

  /**
   * Complete a given path until the namespace.
   */
  public static List<List<String>> completePaths(CloseableDSLContext ctx, int userId,
      List<List<String>> startPartialPaths) {
    List<List<String>> partialPaths = new ArrayList<List<String>>(startPartialPaths);
    for (int i = 0; i < partialPaths.size(); i++) {
      List<String> partialPath = partialPaths.get(i);
      if (partialPath.stream().anyMatch(n ->
          n.toUpperCase().contains(ElementType.NAMESPACE.getLiteral()))) {
        continue;
      }
      List<ScopedIdentifier> parentScopedIdentifiers = getParentScopedIdentifiers(ctx,
          partialPath.get(0)).stream().filter(p ->
          p.getStatus() != Status.OUTDATED).collect(Collectors.toList());
      partialPaths.remove(partialPath);
      if (parentScopedIdentifiers.size() > 0) {
        for (ScopedIdentifier parentScopedIdentifier : parentScopedIdentifiers) {
          String parentUrn = IdentificationHandler.toUrn(ctx, parentScopedIdentifier);
          List<String> newPartialPath = new ArrayList<>();
          newPartialPath.add(parentUrn);
          newPartialPath.addAll(partialPath);
          partialPaths.add(newPartialPath);
        }
      } else {
        Identification namespaceIdentification = IdentificationHandler
            .getNamespaceIdentification(ctx, partialPath.get(0));
        if (!DaoUtil.accessLevelGranted(namespaceIdentification.getIdentifier(),
            userId, DaoUtil.READ_ACCESS_TYPES)) {
          partialPaths.remove(partialPath);
        } else {
          List<String> newPartialPath = new ArrayList<>();
          newPartialPath.add(namespaceIdentification.getNamespaceUrn());
          newPartialPath.addAll(partialPath);
          partialPaths.add(newPartialPath);
        }
      }
    }
    if (!pathsCompleted(partialPaths)) {
      partialPaths = completePaths(ctx, userId, partialPaths);
    }
    return partialPaths;
  }

  /**
   * Return true if all paths are completed and reached the namespace otherwise return false.
   */
  public static boolean pathsCompleted(List<List<String>> paths) {
    for (List<String> path : paths) {
      if (path.stream().anyMatch(n ->
          n.toUpperCase().contains(ElementType.NAMESPACE.getLiteral()))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get the designations for all urns in a path.
   */
  public static List<List<String>> getDesignations(CloseableDSLContext ctx,
      List<List<String>> paths, String languages) {
    List<List<String>> allDesignations = new ArrayList<>();
    for (int pathIndex = 0; pathIndex < paths.size(); pathIndex++) {
      List<String> pathDesignations = new ArrayList<>();
      for (int urnIndex = 0; urnIndex < paths.get(pathIndex).size(); urnIndex++) {
        String urn = paths.get(pathIndex).get(urnIndex);
        ScopedIdentifier scopedIdentifier =
            IdentificationHandler.getScopedIdentifier(ctx, urn);
        pathDesignations.add(getDesignation(ctx, scopedIdentifier.getId(), languages));
      }
      allDesignations.add(pathDesignations);
    }
    return allDesignations;
  }

  /**
   * Get element designation by scopedIdentifierId.
   */
  public static String getDesignation(CloseableDSLContext ctx,
      int scopedIdentifierId, String languages) {
    Element element = new Element();
    element.setDefinitions(DefinitionHandler.get(ctx, scopedIdentifierId));
    element.applyLanguageFilter(languages);
    return element.getDefinitions().get(0).getDesignation();
  }

  /**
   * Return list of scopedIdentifiers for all parent elements.
   */
  public static List<ScopedIdentifier> getParentScopedIdentifiers(
      CloseableDSLContext ctx, String urn) {
    return ctx.select()
        .from(SCOPED_IDENTIFIER_HIERARCHY)
        .leftJoin(SCOPED_IDENTIFIER)
        .on(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID))
        .where(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID
            .eq(IdentificationHandler.getScopedIdentifier(ctx, urn).getId()))
        .fetchInto(ScopedIdentifier.class);
  }
}
