package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.Tables.DEFINITION;

import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.DefinitionRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.dto.element.section.Definition;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.CloseableDSLContext;

public class DefinitionHandler {

  /**
   * Get all definitions for a scoped identifier.
   */
  public static List<Definition> get(CloseableDSLContext ctx, int elementId,
      int scopedIdentifierId) {
    List<DefinitionRecord> definitionRecords =
        ctx.fetch(DEFINITION,
            DEFINITION.SCOPED_IDENTIFIER_ID.equal(scopedIdentifierId)
                .and(DEFINITION.ELEMENT_ID.equal(elementId)));

    List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitions =
        definitionRecords.stream()
            .map(definitionRecord -> definitionRecord.into(
                de.dataelementhub.dal.jooq.tables.pojos.Definition.class))
            .collect(Collectors.toList());
    return convert(definitions);
  }

  /**
   * Convert a list of Definition objects of DataElementHub DAL to a list of Definition objects of
   * DataElementHub Model.
   */
  public static List<Definition> convert(
      List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitions) {
    return definitions.stream().map(DefinitionHandler::convert).collect(Collectors.toList());
  }

  /**
   * Convert a Definition object of DataElementHub DAL to a Definition object of DataElementHub
   * Model.
   */
  public static Definition convert(de.dataelementhub.dal.jooq.tables.pojos.Definition definition) {
    Definition newDefinition = new Definition();
    newDefinition.setLanguage(definition.getLanguage());
    newDefinition.setDesignation(definition.getDesignation());
    newDefinition.setDefinition(definition.getDefinition());
    return newDefinition;
  }

  /**
   * Convert a list of Definition objects of DataElementHub Model to a list of Definition objects of
   * DataElementHub DAL.
   */
  public static List<de.dataelementhub.dal.jooq.tables.pojos.Definition> convert(
      List<Definition> definitions, Integer elementId) {
    return convert(definitions, elementId, null);
  }

  /**
   * Convert a list of Definition objects of DataElementHub Model to a list of Definition objects of
   * DataElementHub DAL.
   */
  public static List<de.dataelementhub.dal.jooq.tables.pojos.Definition> convert(
      List<Definition> definitions, Integer elementId, Integer scopedIdentifierId) {
    return definitions.stream().map(d -> convert(d, elementId, scopedIdentifierId))
        .collect(Collectors.toList());
  }

  /**
   * Convert a Definition object of DataElementHub Model to a Definition object of DataElementHub
   * DAL.
   */
  public static de.dataelementhub.dal.jooq.tables.pojos.Definition convert(Definition definition,
      Integer elementId) {
    return convert(definition, elementId, null);
  }

  /**
   * Convert a Definition object of DataElementHub Model to a Definition object of DataElementHub
   * DAL.
   */
  public static de.dataelementhub.dal.jooq.tables.pojos.Definition convert(Definition definition,
      Integer elementId, Integer scopedIdentifierId) {
    de.dataelementhub.dal.jooq.tables.pojos.Definition newDefinition =
        new de.dataelementhub.dal.jooq.tables.pojos.Definition();
    newDefinition.setLanguage(definition.getLanguage());
    newDefinition.setDesignation(definition.getDesignation());
    newDefinition.setDefinition(definition.getDefinition());
    newDefinition.setElementId(elementId);
    newDefinition.setScopedIdentifierId(scopedIdentifierId);
    return newDefinition;
  }

  /**
   * Insert a list of definitions for an element.
   */
  public static void create(CloseableDSLContext ctx, List<Definition> definitions,
      Integer elementId) {
    create(ctx, definitions, elementId, null);
  }

  /**
   * Insert a list of definitions for an element / scoped identifier.
   */
  public static void create(CloseableDSLContext ctx, List<Definition> definitions,
      Integer elementId, Integer scopedIdentifierId) {
    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitionPojos = DefinitionHandler
        .convert(definitions, elementId);
    definitionPojos.forEach(d -> {
      d.setElementId(elementId);
      d.setScopedIdentifierId(scopedIdentifierId);
    });
    saveDefinitions(ctx, definitionPojos);
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
  }

  /**
   * Save definitions.
   */
  public static void saveDefinitions(CloseableDSLContext ctx,
      List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitions) {
    definitions.forEach(d -> ctx.newRecord(DEFINITION, d).store());
  }

  /**
   * Save definition.
   */
  public static void saveDefinition(CloseableDSLContext ctx,
      de.dataelementhub.dal.jooq.tables.pojos.Definition definition) {
    ctx.newRecord(DEFINITION, definition).store();
  }

  /**
   * Copy definitions from one scoped identifier to another.
   */
  public static void copyDefinitions(CloseableDSLContext ctx, Integer sourceId, Integer targetId) {
    List<de.dataelementhub.dal.jooq.tables.pojos.Definition> definitions = ctx.selectFrom(
            DEFINITION)
        .where(DEFINITION.SCOPED_IDENTIFIER_ID.eq(sourceId))
        .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.Definition.class);

    definitions.forEach(d -> {
      d.setId(null);
      d.setScopedIdentifierId(targetId);
      ctx.newRecord(DEFINITION, d).store();
    });
  }

  /**
   * Delete all Definitions belonging to a given element (by element scoped identifier).
   */
  public static void deleteDefinitionsByElementUrnId(CloseableDSLContext ctx, int userId,
      ScopedIdentifier scopedIdentifier) {
    ctx.deleteFrom(DEFINITION).where(DEFINITION.SCOPED_IDENTIFIER_ID.eq(scopedIdentifier.getId()))
        .execute();
  }

  /**
   * Update the definitions of an existing element.
   */
  public static void updateDefinitions(CloseableDSLContext ctx, int userId, String urn,
      List<Definition> definitions) {
    ScopedIdentifier elementScopedIdentifier = IdentificationHandler.getScopedIdentifier(ctx, urn);
    // Delete the old definitions first because updating can also remove definitions.
    deleteDefinitionsByElementUrnId(ctx, userId, elementScopedIdentifier);
    create(ctx, definitions, elementScopedIdentifier.getElementId(),
        elementScopedIdentifier.getId());
  }
}
