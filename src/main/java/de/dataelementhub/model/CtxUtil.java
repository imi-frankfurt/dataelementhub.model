package de.dataelementhub.model;

import org.jooq.CloseableDSLContext;

public class CtxUtil {

  /**
   * Disable the auto commit of the given CloseableDSLContext and return the previous autoCommit
   * setting.
   */
  public static boolean disableAutoCommit(CloseableDSLContext ctx) {
    return ctx.connectionResult(c -> {
      boolean commit = c.getAutoCommit();
      c.setAutoCommit(false);
      return commit;
    });
  }

  /**
   * Commit transactions of the given CloseableDSLContext and set the auto commit back to the given
   * value.
   */
  public static void commitAndSetAutoCommit(CloseableDSLContext ctx, boolean autoCommit) {
    ctx.connection(c -> {
      c.commit();
      c.setAutoCommit(autoCommit);
    });
  }


  /**
   * Rollback transactions of the given CloseableDSLContext and set the auto commit back to the
   * given value.
   */
  public static void rollbackAndSetAutoCommit(CloseableDSLContext ctx, boolean autoCommit) {
    ctx.connection(c -> {
      c.rollback();
      c.setAutoCommit(autoCommit);
    });
  }

}
