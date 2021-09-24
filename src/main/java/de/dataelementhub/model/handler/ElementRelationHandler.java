package de.dataelementhub.model.handler;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT_RELATION;
import static de.dataelementhub.dal.jooq.tables.Source.SOURCE;
import static org.jooq.impl.DSL.trueCondition;

import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.dal.jooq.tables.pojos.ElementRelation;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import de.dataelementhub.dal.jooq.tables.records.ElementRelationRecord;
import de.dataelementhub.dal.jooq.tables.records.SourceRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.jooq.Condition;
import org.jooq.lambda.tuple.Tuple3;
import org.simpleflatmapper.jdbc.JdbcMapper;
import org.simpleflatmapper.jdbc.JdbcMapperFactory;
import org.simpleflatmapper.util.TypeReference;

public class ElementRelationHandler {

  /**
   * Insert a new relation between two local elements.
   */
  public static void insertLocalRelation(CloseableDSLContext ctx, int userId, String leftUrn,
      String rightUrn,
      RelationType relationType) {

    Source localDeHubSource = SourceHandler.getLocalDeHubSource(ctx, userId);
    insertRelation(ctx, userId, leftUrn, localDeHubSource.getId(), rightUrn,
        localDeHubSource.getId(),
        relationType);
  }

  /**
   * Insert a new relation between two elements.
   */
  public static void insertRelation(CloseableDSLContext ctx, int userId, String leftUrn,
      int leftSourceId, String rightUrn, int rightSourceId, RelationType relationType) {

    ElementRelation elementRelation = new ElementRelation();
    elementRelation.setLeftUrn(leftUrn);
    elementRelation.setLeftSource(leftSourceId);
    elementRelation.setRightUrn(rightUrn);
    elementRelation.setRightSource(rightSourceId);
    elementRelation.setRelation(relationType);
    elementRelation.setCreatedBy(userId);

    insertRelation(ctx, userId, elementRelation);
  }

  /**
   * Insert a new relation between two elements.
   */
  public static void insertRelation(CloseableDSLContext ctx, int userId,
      ElementRelation elementRelation) {

    elementRelation.setCreatedBy(userId);
    ctx.newRecord(ELEMENT_RELATION, elementRelation).store();
  }

  public static List<de.dataelementhub.model.dto.ElementRelation> getElementRelations(
      CloseableDSLContext ctx) {
    return getElementRelations(ctx, null, null);
  }

  public static List<de.dataelementhub.model.dto.ElementRelation> getElementRelations(
      CloseableDSLContext ctx, List<RelationType> relationTypes) {
    return getElementRelations(ctx, null, relationTypes);
  }

  public static List<de.dataelementhub.model.dto.ElementRelation> getElementRelations(
      CloseableDSLContext ctx, String elementUrn) {
    return getElementRelations(ctx, elementUrn, null);
  }

  /**
   * Get a list of element relations. Can be filtered by elementUrn and/or relation type
   *
   * @param elementUrn restrict to relations for one element or set to null for all
   * @param relationTypes restrict to certain types of relation types or set to null for all
   */
  public static List<de.dataelementhub.model.dto.ElementRelation> getElementRelations(
      CloseableDSLContext ctx, String elementUrn, List<RelationType> relationTypes) {
    de.dataelementhub.dal.jooq.tables.Source leftSourceTable = SOURCE.as("left_source");
    de.dataelementhub.dal.jooq.tables.Source rightSourceTable = SOURCE.as("right_source");
    List<de.dataelementhub.model.dto.ElementRelation> elementRelations = new ArrayList<>();

    Condition typeCondition = trueCondition();
    Condition urnCondition = trueCondition();

    if (relationTypes != null && !relationTypes.isEmpty()) {
      typeCondition = ELEMENT_RELATION.RELATION.in(relationTypes);
    }

    if (elementUrn != null && !elementUrn.isEmpty()) {
      urnCondition = ELEMENT_RELATION.LEFT_URN.eq(elementUrn)
          .or(ELEMENT_RELATION.RIGHT_URN.eq(elementUrn));
    }

    JdbcMapper<Tuple3<ElementRelationRecord, SourceRecord, SourceRecord>> mapper =
        JdbcMapperFactory.newInstance()
            .addKeys("left_source", "right_source")
            .newMapper(
                new TypeReference<Tuple3<ElementRelationRecord, SourceRecord, SourceRecord>>() {
                });
    try (ResultSet rs =
        ctx
            .select(
                ELEMENT_RELATION.LEFT_URN,
                ELEMENT_RELATION.RIGHT_URN,
                ELEMENT_RELATION.RELATION,
                ELEMENT_RELATION.CREATED_AT,
                ELEMENT_RELATION.CREATED_BY,
                leftSourceTable.ID,
                leftSourceTable.TYPE,
                leftSourceTable.NAME,
                leftSourceTable.PREFIX,
                leftSourceTable.BASE_URL,
                rightSourceTable.ID,
                rightSourceTable.TYPE,
                rightSourceTable.NAME,
                rightSourceTable.PREFIX,
                rightSourceTable.BASE_URL)
            .from(ELEMENT_RELATION)
            .leftJoin(leftSourceTable)
            .on(leftSourceTable.ID.eq(ELEMENT_RELATION.LEFT_SOURCE))
            .leftJoin(rightSourceTable)
            .on(rightSourceTable.ID.eq(ELEMENT_RELATION.RIGHT_SOURCE))
            .where(urnCondition)
            .and(typeCondition)
            .fetchResultSet()) {

      mapper.stream(rs).forEach(t3 -> {
        Source leftSource = new Source();
        leftSource.setId(t3.v2().getId());
        leftSource.setType(t3.v2().getType());
        leftSource.setName(t3.v2().getName());
        leftSource.setPrefix(t3.v2().getPrefix());
        leftSource.setBaseUrl(t3.v2().getBaseUrl());

        Source rightSource = new Source();
        rightSource.setId(t3.v3().getId());
        rightSource.setType(t3.v3().getType());
        rightSource.setName(t3.v3().getName());
        rightSource.setPrefix(t3.v3().getPrefix());
        rightSource.setBaseUrl(t3.v3().getBaseUrl());

        de.dataelementhub.model.dto.ElementRelation elementRelation =
            new de.dataelementhub.model.dto.ElementRelation();
        elementRelation.setLeftUrn(t3.v1().getLeftUrn());
        elementRelation.setLeftSource(leftSource);
        elementRelation.setRightUrn(t3.v1().getRightUrn());
        elementRelation.setRightSource(rightSource);
        elementRelation.setRelation(t3.v1().getRelation());
        elementRelation.setCreatedAt(t3.v1().getCreatedAt());
        elementRelation.setCreatedBy(t3.v1().getCreatedBy());
        elementRelations.add(elementRelation);
      });
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return elementRelations;
  }

  /**
   * Update the supplied element relation to the new relation type.
   * TODO: Add access check for user
   */
  public static boolean updateElementRelation(CloseableDSLContext ctx, int userId,
      ElementRelation elementRelation) {

    int count = ctx.update(ELEMENT_RELATION)
        .set(ELEMENT_RELATION.RELATION, elementRelation.getRelation())
        .where(ELEMENT_RELATION.LEFT_URN.eq(elementRelation.getLeftUrn()))
        .and(ELEMENT_RELATION.RIGHT_URN.eq(elementRelation.getRightUrn()))
        .and(ELEMENT_RELATION.LEFT_SOURCE.eq(elementRelation.getLeftSource()))
        .and(ELEMENT_RELATION.RIGHT_SOURCE.eq(elementRelation.getRightSource()))
        .execute();
    return count > 0;
  }

  /**
   * Delete an element relation from the database.
   * TODO: Add access check for user
   */
  public static boolean deleteElementRelation(CloseableDSLContext ctx, int userId,
      ElementRelation elementRelation) {

    int count = ctx.deleteFrom(ELEMENT_RELATION)
        .where(ELEMENT_RELATION.LEFT_URN.eq(elementRelation.getLeftUrn()))
        .and(ELEMENT_RELATION.RIGHT_URN.eq(elementRelation.getRightUrn()))
        .and(ELEMENT_RELATION.LEFT_SOURCE.eq(elementRelation.getLeftSource()))
        .and(ELEMENT_RELATION.RIGHT_SOURCE.eq(elementRelation.getRightSource()))
        .execute();
    return count > 0;
  }
}
