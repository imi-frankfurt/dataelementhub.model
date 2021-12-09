package de.dataelementhub.model;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.HIERARCHY;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.USER_NAMESPACE_GRANTS;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.dal.jooq.tables.Element;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.jooq.Condition;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

public class DaoUtil {

  private static final String[] materializedViews = new String[] {HIERARCHY.getName()};
  public static final List<GrantType> READ_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.READ, GrantType.WRITE, GrantType.ADMIN));
  public static final List<GrantType> WRITE_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.WRITE, GrantType.ADMIN));
  public static final List<GrantType> ADMIN_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.ADMIN));

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   */
  public static Condition accessibleByUserId(CloseableDSLContext ctx, int userId) {
    return ELEMENT.HIDDEN.isNull()
        .or(ELEMENT.HIDDEN.eq(false))
        .or(ELEMENT.ID.in(getUserNamespaceGrantsQuery(ctx, userId, READ_ACCESS_GRANTS)));
  }

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   * Requires the Element table for check as parameter.
   */
  public static Condition accessibleByUserId(CloseableDSLContext ctx, int userId, Element element) {
    return element.HIDDEN.isNull()
        .or(element.HIDDEN.eq(false))
        .or(element.ID.in(getUserNamespaceGrantsQuery(ctx, userId, READ_ACCESS_GRANTS)));
  }

  /**
   * Returns a condition which checks whether a user has the required grant to a namespace or not.
   */
  public static SelectConditionStep<Record1<Integer>> getUserNamespaceGrantsQuery(
      CloseableDSLContext ctx, int userId, List<GrantType> grantTypes) {
    return ctx.select(USER_NAMESPACE_GRANTS.NAMESPACE_ID)
        .from(USER_NAMESPACE_GRANTS)
        .where(USER_NAMESPACE_GRANTS.USER_ID.eq(userId))
        .and(USER_NAMESPACE_GRANTS.GRANT_TYPE.in(grantTypes));
  }

  /** Refreshes all materialized views. */
  public static void refreshMaterializedViews() {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      for (String view : materializedViews) {
        // TODO: Maybe there is a better way to do this with JOOQ?
        ctx.execute("REFRESH MATERIALIZED VIEW " + view);
      }
    }
  }

  /** returns if the user has one of the given grants for a namespace identifier. */
  public static Boolean checkGrants(Integer namespaceIdentifier, Integer userId,
      List<GrantType> grantTypes) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return ctx.fetchExists(ctx.select()
          .from(USER_NAMESPACE_GRANTS)
          .join(SCOPED_IDENTIFIER)
          .on(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.ELEMENT_ID))
          .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceIdentifier))
          .and(USER_NAMESPACE_GRANTS.USER_ID.eq(userId))
          .and(USER_NAMESPACE_GRANTS.GRANT_TYPE.in(grantTypes)));
    }
  }

}
