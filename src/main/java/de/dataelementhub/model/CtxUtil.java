package de.dataelementhub.model;

import org.jooq.DSLContext;

/**
 * CtxUtil.
 */
public class CtxUtil {

  /**
   * Disable the auto commit of the given DSLContext and return the previous autoCommit
   * setting.
   */
  public static boolean disableAutoCommit(DSLContext ctx) {
    return ctx.connectionResult(c -> {
      boolean commit = c.getAutoCommit();
      c.setAutoCommit(false);
      boolean foo = c.getAutoCommit();
      return commit;
    });
  }

  /**
   * Commit transactions of the given DSLContext and set the auto commit back to the given
   * value.
   */
  public static void commitAndSetAutoCommit(DSLContext ctx, boolean autoCommit) {
    ctx.connection(c -> {
      c.commit();
      c.setAutoCommit(autoCommit);
    });
  }


  /**
   * Rollback transactions of the given DSLContext and set the auto commit back to the
   * given value.
   */
  public static void rollbackAndSetAutoCommit(DSLContext ctx, boolean autoCommit) {
    ctx.connection(c -> {
      c.rollback();
      c.setAutoCommit(autoCommit);
    });
  }

}
