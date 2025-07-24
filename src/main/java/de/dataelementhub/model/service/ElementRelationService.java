package de.dataelementhub.model.service;

import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.model.dto.ElementRelation;
import de.dataelementhub.model.handler.ElementRelationHandler;
import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * Element Relation Service.
 */
@Service
public class ElementRelationService {

  /**
   * Get a list of all ElementRelations.
   */
  public List<ElementRelation> list(DSLContext ctx) {
    return ElementRelationHandler.getElementRelations(ctx);
  }

  /**
   * Get a list of all ElementRelations of the provided type.
   */
  public List<ElementRelation> listByType(DSLContext ctx, RelationType relationType) {
    return listByTypes(ctx, Collections.singletonList(relationType));
  }

  /**
   * Get a list of all ElementRelations of the provided types.
   */
  public List<ElementRelation> listByTypes(
      DSLContext ctx, List<RelationType> relationTypes) {
    return ElementRelationHandler.getElementRelations(ctx, relationTypes);
  }

  /**
   * Insert a new dataelement relation.
   */
  public void createDataElementRelation(DSLContext ctx, int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    ElementRelationHandler.insertRelation(ctx, userId, elementRelation);
  }

  /**
   * Update an existing dataelement relation.
   */
  public void updateDataElementRelation(DSLContext ctx, int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    ElementRelationHandler.updateElementRelation(ctx, userId, elementRelation);
  }

  /**
   * Delete an existing dataelement relation.
   */
  public void deleteDataElementRelation(DSLContext ctx, int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    ElementRelationHandler.deleteElementRelation(ctx, userId, elementRelation);
  }
}
