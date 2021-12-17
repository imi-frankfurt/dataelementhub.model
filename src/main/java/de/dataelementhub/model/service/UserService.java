package de.dataelementhub.model.service;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.UserHandler;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  /**
   * Give a user access to a namespace.
   */
  public void grantAccessToNamespace(int executingUserId, int namespaceIdentifier,
      List<DeHubUserPermission> userPermissions) throws IllegalAccessException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (AccessLevelHandler.getAccessLevelByUserAndNamespaceIdentifier(ctx, executingUserId,
          namespaceIdentifier) != AccessLevelType.ADMIN) {
        throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
      }

      userPermissions.forEach(up -> {
        DehubUser user = UserHandler.getUserByIdentity(ctx, up.getUserAuthId());

        if (up.getAccessLevel() == null || up.getAccessLevel().isEmpty()) {
          if (user == null) {
            // If the user is not known, there is nothing to do anyways here
            return;
          } else {
            UserHandler.removeUserAccessFromNamespace(user.getId(), namespaceIdentifier);
          }
        } else {
          if (user == null) {
            // If the user is not yet known, create a new entry.
            // TODO: Not sure about email and user name handling
            user = UserHandler.createDefaultUser(ctx, up.getUserAuthId(), "", "");
          }
          UserHandler.setUserAccessToNamespace(user.getId(), namespaceIdentifier,
              AccessLevelType.valueOf(up.getAccessLevel().toUpperCase()));
        }
      });
    }
  }

  /**
   * Remove a users access to a namespace.
   */
  public void revokeAccessToNamespace(int executingUserId, int namespaceIdentifier,
      String userAuthId) throws IllegalAccessException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (AccessLevelHandler.getAccessLevelByUserAndNamespaceIdentifier(ctx, executingUserId,
          namespaceIdentifier) != AccessLevelType.ADMIN) {
        throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
      }
      DehubUser user = UserHandler.getUserByIdentity(ctx, userAuthId);
      UserHandler.removeUserAccessFromNamespace(user.getId(), namespaceIdentifier);
    }
  }

}
