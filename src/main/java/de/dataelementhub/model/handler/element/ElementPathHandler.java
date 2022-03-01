package de.dataelementhub.model.handler.element;

import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER_HIERARCHY;

import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.ElementPath;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jooq.CloseableDSLContext;

public class ElementPathHandler {

  /**
   * Get all available paths for a given element.
   */
  public static List<ElementPath> getElementPaths(CloseableDSLContext ctx,
      int userId, String urn, String languages) {
    List<ElementPath> elementPaths = new ArrayList<>();
    List<String> pathUrnsList = completePaths(ctx, userId, Collections.singletonList(urn));
    List<String> pathDesignationsList = getDesignations(ctx, pathUrnsList, languages);
    for (int i = 0; i < pathUrnsList.size(); i++) {
      ElementPath elementPath = new ElementPath();
      elementPath.setUrns(pathUrnsList.get(i));
      elementPath.setDesignations(pathDesignationsList.get(i));
      elementPaths.add(elementPath);
    }
    return elementPaths;
  }

  /**
   * Complete a given path until the namespace.
   */
  public static List<String> completePaths(CloseableDSLContext ctx, int userId,
      List<String> startPartialPaths) {
    List<String> partialPaths = new ArrayList<String>(startPartialPaths);
    for (int i = 0; i < partialPaths.size(); i++) {
      String partialPath = partialPaths.get(i);
      String urn;
      if (partialPath.toLowerCase().contains("namespace")) {
        continue;
      } else if (partialPath.contains("/")) {
        urn = partialPath.split("/")[0];
      } else {
        urn = partialPath;
      }
      List<ScopedIdentifier> parentScopedIdentifiers = getParentScopedIdentifiers(ctx,urn);
      partialPaths.remove(partialPath);
      if (parentScopedIdentifiers.size() > 0) {
        for (ScopedIdentifier parentScopedIdentifier : parentScopedIdentifiers) {
          String parentUrn = IdentificationHandler.toUrn(ctx, parentScopedIdentifier);
          partialPaths.add(parentUrn + "/" + partialPath);
        }
      } else {
        Identification namespaceIdentification = IdentificationHandler
            .getNamespaceIdentification(ctx, urn);
        if (!DaoUtil.accessLevelGranted(namespaceIdentification.getIdentifier(),
            userId, DaoUtil.READ_ACCESS_TYPES)) {
          partialPaths.remove(partialPath);
        } else {
          partialPaths.add(namespaceIdentification.getNamespaceUrn() + "/" + partialPath);
        }
      }
    }
    boolean pathsCompleted = partialPaths.stream().allMatch(n -> n.contains("namespace"));
    if (!pathsCompleted) {
      partialPaths = completePaths(ctx, userId, partialPaths);
    }
    return partialPaths;
  }

  /**
   * Get the designations for all urns in a path.
   */
  public static List<String> getDesignations(CloseableDSLContext ctx,
      List<String> paths, String languages) {
    List<String> pathDesignationsList = new ArrayList<>();
    for (String path : paths) {
      String[] pathUrns = path.split("/");
      String pathDesignations = "";
      for (int i = 0; i < pathUrns.length; i++) {
        if (i == pathUrns.length - 1) {
          ScopedIdentifier scopedIdentifier =
              IdentificationHandler.getScopedIdentifier(ctx, pathUrns[i]);
          pathDesignations =
              pathDesignations + getDesignation(ctx, scopedIdentifier.getId(), languages);
        } else {
          ScopedIdentifier scopedIdentifier =
              IdentificationHandler.getScopedIdentifier(ctx, pathUrns[i]);
          pathDesignations = pathDesignations
              + getDesignation(ctx, scopedIdentifier.getId(), languages) + "/";
        }
      }
      pathDesignationsList.add(pathDesignations);
    }
    return pathDesignationsList;
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
    int scopedIdentifierId = IdentificationHandler.getScopedIdentifier(ctx, urn).getId();
    List<ScopedIdentifier> parentElements =
        ctx.select()
        .from(SCOPED_IDENTIFIER_HIERARCHY)
        .leftJoin(SCOPED_IDENTIFIER)
        .on(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID))
        .where(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID.eq(scopedIdentifierId))
        .fetchInto(ScopedIdentifier.class);
    return parentElements;
  }
}
