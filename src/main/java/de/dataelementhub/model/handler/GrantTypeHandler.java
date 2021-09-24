package de.dataelementhub.model.handler;

import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.USER_NAMESPACE_GRANTS;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.dal.jooq.tables.pojos.UserNamespaceGrants;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.List;
import org.jooq.CloseableDSLContext;

public class GrantTypeHandler {

  /**
   * Returns highest grant type of the given user and namespace.
   * Namespace is given by its identifier.
   */
  public static GrantType getGrantTypeByUserAndNamespaceIdentifier(CloseableDSLContext ctx,
      int userId, int namespaceSiIdentifier) {
    List<GrantType> grantTypes =
        ctx.select(USER_NAMESPACE_GRANTS.GRANT_TYPE).from(USER_NAMESPACE_GRANTS)
            .leftJoin(SCOPED_IDENTIFIER)
            .on(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
            .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceSiIdentifier))
            .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
            .and(USER_NAMESPACE_GRANTS.USER_ID.eq(userId)).fetchInto(GrantType.class);

    return getHighestGrantType(grantTypes);
  }


  /**
   * Returns highest grant type of the given user and namespace.
   * Namespace is given by its urn.
   */
  public static GrantType getGrantTypeByUserAndNamespaceUrn(CloseableDSLContext ctx,
      int userId, String namespaceUrn) {
    Integer namespaceIdentifier = IdentificationHandler.getNamespaceIdentifierFromUrn(
        namespaceUrn);
    return getGrantTypeByUserAndNamespaceIdentifier(ctx, userId, namespaceIdentifier);
  }

  /**
   * Returns highest grant type of the given user and namespace.
   * Namespace is given by its database id.
   */
  public static GrantType getGrantTypeByUserAndNamespaceId(CloseableDSLContext ctx,
      int userId, int namespaceId) {
    List<GrantType> grantTypes =
        ctx.select(USER_NAMESPACE_GRANTS.GRANT_TYPE).from(USER_NAMESPACE_GRANTS)
            .where(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(namespaceId))
            .and(USER_NAMESPACE_GRANTS.USER_ID.eq(userId)).fetchInto(GrantType.class);

    return getHighestGrantType(grantTypes);
  }

  /**
   * Returns all grants for a given namespace.
   * Namespace is given by its identifier.
   */
  public static List<UserNamespaceGrants> getGrantsForNamespaceByIdentifier(CloseableDSLContext ctx,
      int namespaceSiIdentifier) {
    return
        ctx.select(USER_NAMESPACE_GRANTS.GRANT_TYPE).from(USER_NAMESPACE_GRANTS)
            .leftJoin(SCOPED_IDENTIFIER)
            .on(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
            .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceSiIdentifier))
            .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
            .fetchInto(UserNamespaceGrants.class);
  }

  /**
   * Returns all grants for a given namespace.
   * Namespace is given by its database id.
   */
  public static List<UserNamespaceGrants> getGrantsForNamespaceById(CloseableDSLContext ctx,
      int namespaceId) {
    return
        ctx.select(USER_NAMESPACE_GRANTS.fields()).from(USER_NAMESPACE_GRANTS)
            .where(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(namespaceId))
            .fetchInto(UserNamespaceGrants.class);
  }


  /**
   * Insert grants for a namespace.
   */
  public static void setGrantsForNamespace(CloseableDSLContext ctx,
      List<UserNamespaceGrants> grants) {
    grants.forEach(g -> ctx.insertInto(USER_NAMESPACE_GRANTS, USER_NAMESPACE_GRANTS.USER_ID,
            USER_NAMESPACE_GRANTS.NAMESPACE_ID, USER_NAMESPACE_GRANTS.GRANT_TYPE)
        .values(g.getUserId(), g.getNamespaceId(), g.getGrantType()).onConflictDoNothing()
        .execute());
  }

  private static GrantType getHighestGrantType(List<GrantType> grantTypes) {
    GrantType highestGrantType = null;
    for (GrantType grantType : grantTypes) {
      if (grantType.equals(GrantType.ADMIN)) {
        highestGrantType = grantType;
        break;
      } else if (grantType.equals(GrantType.WRITE)) {
        highestGrantType = grantType;
      } else if (grantType == GrantType.READ && highestGrantType == null) {
        highestGrantType = grantType;
      }
    }
    return highestGrantType;
  }
}
