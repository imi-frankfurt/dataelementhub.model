package de.dataelementhub.model.service;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.UserHandler;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * User Service.
 */
@Service
public class UserService {

  /**
   * Give a user access to a namespace.
   */
  public void grantAccessToNamespace(
      DSLContext ctx, int executingUserId, int namespaceIdentifier,
      List<DeHubUserPermission> userPermissions) throws IllegalAccessException {
    if (AccessLevelHandler.getAccessLevelByUserAndNamespaceIdentifier(ctx, executingUserId,
        namespaceIdentifier) != AccessLevelType.ADMIN) {
      throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
    }


    // First get all users from the db to have the database ids as well as the auth ids
    List<String> authIds = new ArrayList<>();
    userPermissions.forEach(up -> authIds.add(up.getUserAuthId()));
    List<DehubUser> userList = UserHandler.getUsersByIdentity(ctx, authIds);

    // If not all users were found, throw an error
    if (userList.size() != userPermissions.size()) {
      throw new IllegalArgumentException("One or more unknown users");
    }

    userPermissions.forEach(up -> {
      DehubUser user = userList.stream()
          .filter(u -> u.getAuthId().equalsIgnoreCase(up.getUserAuthId())).findFirst()
          .orElseThrow(IllegalArgumentException::new);

      if (up.getAccessLevel() == null || up.getAccessLevel().isEmpty()  || user.getId() < 0) {
        throw new IllegalArgumentException("Empty access level not allowed");
      } else {
        UserHandler.setUserAccessToNamespace(ctx, user.getId(), namespaceIdentifier,
            AccessLevelType.valueOf(up.getAccessLevel().toUpperCase()));
      }
    });
  }

  /**
   * Remove a users access to a namespace.
   */
  public void revokeAccessToNamespace(
      DSLContext ctx, int executingUserId, int namespaceIdentifier,
      String userAuthId) throws IllegalAccessException {
    if (AccessLevelHandler.getAccessLevelByUserAndNamespaceIdentifier(ctx, executingUserId,
        namespaceIdentifier) != AccessLevelType.ADMIN) {
      throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
    }
    DehubUser user = UserHandler.getUserByIdentity(ctx, userAuthId);
    if (executingUserId == user.getId()) {
      throw new IllegalArgumentException("You can not remove your own access.");
    }
    UserHandler.removeUserAccessFromNamespace(ctx, user.getId(), namespaceIdentifier);
  }

}
