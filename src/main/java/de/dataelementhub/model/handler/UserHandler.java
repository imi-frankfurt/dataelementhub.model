package de.dataelementhub.model.handler;

import static de.dataelementhub.dal.jooq.tables.DehubUser.DEHUB_USER;
import static de.dataelementhub.dal.jooq.tables.UserNamespaceAccess.USER_NAMESPACE_ACCESS;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.Keys;
import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.dal.jooq.tables.pojos.UserNamespaceAccess;
import de.dataelementhub.dal.jooq.tables.records.DehubUserRecord;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.dal.jooq.tables.records.UserNamespaceAccessRecord;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import java.util.List;
import org.jooq.CloseableDSLContext;

/**
 * User Handler.
 */
public class UserHandler {

  /**
   * Get a user by auth id.
   */
  public static DehubUser getUserByIdentity(CloseableDSLContext ctx, String identity) {
    if ("anonymousUser".equals(identity)) {
      DehubUser anon = new DehubUser();
      anon.setId(-1);
      return anon;
    }
    try {
      return ctx.fetchOne(DEHUB_USER, DEHUB_USER.AUTH_ID.equal(identity)).into(DehubUser.class);
    } catch (NullPointerException npe) {
      return null;
    }
  }

  /**
   * Get a list of users by auth ids.
   */
  public static List<DehubUser> getUsersByIdentity(CloseableDSLContext ctx, List<String> identity) {
    try {
      return ctx.fetch(DEHUB_USER, DEHUB_USER.AUTH_ID.in(identity)).into(DehubUser.class);
    } catch (NullPointerException npe) {
      return null;
    }
  }

  /**
   * Get a user by database id.
   */
  public static DehubUser getUserById(CloseableDSLContext ctx, int userId) {
    return ctx.fetchOne(DEHUB_USER, DEHUB_USER.ID.equal(userId)).into(DehubUser.class);
  }

  /**
   * Store a user in the database.
   */
  public static int saveUser(CloseableDSLContext ctx, DehubUser dehubUser) {
    DehubUserRecord dehubUserRecord = ctx.newRecord(DEHUB_USER, dehubUser);
    dehubUserRecord.store();
    return dehubUserRecord.getId();
  }

  /**
   * Create a Default user if not exists otherwise update user.
   */
  public static void upsertUser(CloseableDSLContext ctx, DehubUser dehubUser) {
    ctx.insertInto(DEHUB_USER)
        .set(DEHUB_USER.AUTH_ID, dehubUser.getAuthId())
        .set(DEHUB_USER.USER_NAME, dehubUser.getUserName())
        .set(DEHUB_USER.EMAIL, dehubUser.getEmail())
        .onConflict(Keys.DEHUB_USER_AUTH_ID_KEY.getFieldsArray())
        .doUpdate()
        .set(DEHUB_USER.USER_NAME, dehubUser.getUserName())
        .set(DEHUB_USER.EMAIL, dehubUser.getEmail())
        .where(DEHUB_USER.AUTH_ID.eq(dehubUser.getAuthId())).execute();
  }

  public static DehubUser createDefaultUser(CloseableDSLContext ctx, String authId, String email,
      String userName) {
    return createDefaultUser(ctx, authId, email, userName, null, null);
  }

  /**
   * Create a default user.
   */
  public static DehubUser createDefaultUser(CloseableDSLContext ctx, String authId, String email,
      String userName, String firstName, String lastName) {
    DehubUser dehubUser = new DehubUser();
    dehubUser.setAuthId(authId);
    dehubUser.setEmail(email);
    dehubUser.setUserName(userName);
    dehubUser.setFirstName(firstName);
    dehubUser.setLastName(lastName);
    int userId = saveUser(ctx, dehubUser);
    dehubUser.setId(userId);
    return dehubUser;
  }

  /**
   * Give a user access to a namespace.
   */
  public static void setUserAccessToNamespace(int userId, int namespaceIdentifier,
      AccessLevelType accessLevel) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      IdentifiedElementRecord namespaceRecord = NamespaceHandler
          .getLatestNamespaceRecord(ctx, userId, namespaceIdentifier);

      UserNamespaceAccessRecord userNamespaceAccessRecord =
          AccessLevelHandler.getUserNamespaceAccessTypeRecordByUserAndNamespaceId(
          ctx, userId, namespaceRecord.getId());
      if (userNamespaceAccessRecord == null) {
        UserNamespaceAccess userNamespaceAccess = new UserNamespaceAccess();
        userNamespaceAccess.setUserId(userId);
        userNamespaceAccess.setNamespaceId(namespaceRecord.getId());
        userNamespaceAccess.setAccessLevel(accessLevel);
        ctx.newRecord(USER_NAMESPACE_ACCESS, userNamespaceAccess).insert();
      } else {
        ctx.update(USER_NAMESPACE_ACCESS).set(USER_NAMESPACE_ACCESS.ACCESS_LEVEL, accessLevel)
            .where(USER_NAMESPACE_ACCESS.USER_ID.eq(userId))
            .and(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(namespaceRecord.getId()))
            .execute();
      }
    }
  }

  /**
   * Revoke user access from a namespace.
   */
  public static void removeUserAccessFromNamespace(int userId, int namespaceIdentifier) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      IdentifiedElementRecord namespaceRecord = NamespaceHandler
          .getLatestNamespaceRecord(ctx, userId, namespaceIdentifier);

      ctx.deleteFrom(USER_NAMESPACE_ACCESS)
          .where(USER_NAMESPACE_ACCESS.USER_ID.eq(userId))
          .and(USER_NAMESPACE_ACCESS.NAMESPACE_ID.eq(namespaceRecord.getId()))
          .execute();
    }
  }
}
