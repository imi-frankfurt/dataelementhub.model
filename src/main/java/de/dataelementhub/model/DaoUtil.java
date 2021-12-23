package de.dataelementhub.model;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.HIERARCHY;
import static de.dataelementhub.dal.jooq.Tables.USER_NAMESPACE_ACCESS;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.AccessLevelType;
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
  public static final List<AccessLevelType> READ_ACCESS_TYPES =
      Collections.unmodifiableList(Arrays.asList(AccessLevelType.READ, AccessLevelType.WRITE,
          AccessLevelType.ADMIN));
  public static final List<AccessLevelType> WRITE_ACCESS_TYPES =
      Collections.unmodifiableList(Arrays.asList(AccessLevelType.WRITE, AccessLevelType.ADMIN));
  public static final List<AccessLevelType> ADMIN_ACCESS_TYPES =
      Collections.unmodifiableList(Arrays.asList(AccessLevelType.ADMIN));

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   */
  public static Condition accessibleByUserId(CloseableDSLContext ctx, int userId) {
    return ELEMENT.HIDDEN.isNull()
        .or(ELEMENT.HIDDEN.eq(false))
        .or(ELEMENT.ID.in(getUserNamespaceAccessQuery(ctx, userId, READ_ACCESS_TYPES)));
  }

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   * Requires the Element table for check as parameter.
   */
  public static Condition accessibleByUserId(CloseableDSLContext ctx, int userId, Element element) {
    return element.HIDDEN.isNull()
        .or(element.HIDDEN.eq(false))
        .or(element.ID.in(getUserNamespaceAccessQuery(ctx, userId, READ_ACCESS_TYPES)));
  }

  /**
   * Returns a condition which checks whether a user has the required access level for a namespace.
   */
  public static SelectConditionStep<Record1<Integer>> getUserNamespaceAccessQuery(
      CloseableDSLContext ctx, int userId, List<AccessLevelType> accessLevels) {
    return ctx.select(USER_NAMESPACE_ACCESS.NAMESPACE_ID)
        .from(USER_NAMESPACE_ACCESS)
        .where(USER_NAMESPACE_ACCESS.USER_ID.eq(userId))
        .and(USER_NAMESPACE_ACCESS.ACCESS_LEVEL.in(accessLevels));
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

}
