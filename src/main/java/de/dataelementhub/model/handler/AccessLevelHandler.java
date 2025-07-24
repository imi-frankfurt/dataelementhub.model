package de.dataelementhub.model.handler;

import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.USER_NAMESPACE_ACCESS;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.tables.pojos.UserNamespaceAccess;
import de.dataelementhub.dal.jooq.tables.records.UserNamespaceAccessRecord;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

/**
 * Access Level Handler.
 */
public class AccessLevelHandler {

  /**
   * Returns the access level of the given user and namespace.
   * Namespace is given by its identifier.
   */
  public static AccessLevelType getAccessLevelByUserAndNamespaceIdentifier(DSLContext ctx,
      int userId, int namespaceSiIdentifier) {
    return ctx.select(USER_NAMESPACE_ACCESS.ACCESS_LEVEL)
        .from(USER_NAMESPACE_ACCESS)
        .leftJoin(SCOPED_IDENTIFIER)
        .on(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
        .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceSiIdentifier))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
        .and(USER_NAMESPACE_ACCESS.USER_ID.eq(userId))
        .orderBy(SCOPED_IDENTIFIER.VERSION.desc())
        .limit(1)
        .fetchOneInto(AccessLevelType.class);
  }

  /**
   * Returns the access level of the given user and namespace.
   * Namespace is given by its urn.
   */
  public static AccessLevelType getAccessLevelByUserAndNamespaceUrn(DSLContext ctx,
      int userId, String namespaceUrn) {
    Integer namespaceIdentifier = IdentificationHandler.getNamespaceIdentifierFromUrn(
        namespaceUrn);
    return getAccessLevelByUserAndNamespaceIdentifier(ctx, userId, namespaceIdentifier);
  }

  /**
   * Returns the access level of the given user and namespace.
   * Namespace is given by its database id.
   */
  public static AccessLevelType getAccessLevelByUserAndNamespaceId(DSLContext ctx,
      int userId, int namespaceId) {
    return ctx.select(USER_NAMESPACE_ACCESS.ACCESS_LEVEL).from(USER_NAMESPACE_ACCESS)
        .where(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(namespaceId))
        .and(USER_NAMESPACE_ACCESS.USER_ID.eq(userId)).fetchOneInto(AccessLevelType.class);
  }

  /**
   * Returns access record of the given user and namespace.
   * Namespace is given by its id.
   */
  public static UserNamespaceAccessRecord getUserNamespaceAccessTypeRecordByUserAndNamespaceId(
      DSLContext ctx, int userId, int namespaceId) {

    return
        ctx.selectFrom(USER_NAMESPACE_ACCESS)
            .where(USER_NAMESPACE_ACCESS.USER_ID.eq(userId))
            .and(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(namespaceId))
            .fetchOne();
  }

  /**
   * Returns all access rights for a given namespace.
   * Namespace is given by its identifier.
   */
  public static List<UserNamespaceAccess> getAccessForNamespaceByIdentifier(DSLContext ctx,
      int namespaceSiIdentifier) {
    return
        ctx.select(USER_NAMESPACE_ACCESS.fields()).from(USER_NAMESPACE_ACCESS)
            .leftJoin(SCOPED_IDENTIFIER)
            .on(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
            .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceSiIdentifier))
            .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
            .and(SCOPED_IDENTIFIER.VERSION.eq(
                ctx.select(DSL.max(SCOPED_IDENTIFIER.VERSION)).from(SCOPED_IDENTIFIER)
                    .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
                    .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceSiIdentifier))
            ))
            .fetchInto(UserNamespaceAccess.class);
  }

  /**
   * Returns all access rights for a given namespace.
   * Namespace is given by its database id.
   */
  public static List<UserNamespaceAccess> getAccessForNamespaceById(DSLContext ctx,
      int namespaceId) {
    return
        ctx.select(USER_NAMESPACE_ACCESS.fields()).from(USER_NAMESPACE_ACCESS)
            .where(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(namespaceId))
            .fetchInto(UserNamespaceAccess.class);
  }


  /**
   * Insert access for a namespace.
   */
  public static void setAccessForNamespace(DSLContext ctx,
      List<UserNamespaceAccess> accessLevels) {
    accessLevels.forEach(al -> ctx.insertInto(USER_NAMESPACE_ACCESS, USER_NAMESPACE_ACCESS.USER_ID,
            USER_NAMESPACE_ACCESS.NAMESPACE_ID, USER_NAMESPACE_ACCESS.ACCESS_LEVEL)
        .values(al.getUserId(), al.getNamespaceId(), al.getAccessLevel()).onConflictDoNothing()
        .execute());
  }
}
