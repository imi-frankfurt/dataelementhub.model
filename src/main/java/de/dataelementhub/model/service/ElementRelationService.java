package de.dataelementhub.model.service;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.model.dto.ElementRelation;
import de.dataelementhub.model.handler.ElementRelationHandler;
import java.util.Collections;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.springframework.stereotype.Service;

/**
 * Element Relation Service.
 */
@Service
public class ElementRelationService {

  /**
   * Get a list of all ElementRelations.
   */
  public List<ElementRelation> list() {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return ElementRelationHandler.getElementRelations(ctx);
    }
  }

  /**
   * Get a list of all ElementRelations of the provided type.
   */
  public List<ElementRelation> listByType(RelationType relationType) {
    return listByTypes(Collections.singletonList(relationType));
  }

  /**
   * Get a list of all ElementRelations of the provided types.
   */
  public List<ElementRelation> listByTypes(List<RelationType> relationTypes) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return ElementRelationHandler.getElementRelations(ctx, relationTypes);
    }
  }

  /**
   * Insert a new dataelement relation.
   */
  public void createDataElementRelation(int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ElementRelationHandler.insertRelation(ctx, userId, elementRelation);
    }
  }

  /**
   * Update an existing dataelement relation.
   */
  public void updateDataElementRelation(int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ElementRelationHandler.updateElementRelation(ctx, userId, elementRelation);
    }
  }

  /**
   * Delete an existing dataelement relation.
   */
  public void deleteDataElementRelation(int userId,
      de.dataelementhub.dal.jooq.tables.pojos.ElementRelation elementRelation) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ElementRelationHandler.deleteElementRelation(ctx, userId, elementRelation);
    }
  }
}
