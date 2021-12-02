package de.dataelementhub.model.service;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.GrantType;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.handler.GrantTypeHandler;
import de.dataelementhub.model.handler.UserHandler;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  /**
   * Give a user access to a namespace
   */
  public void grantAccessToNamespace(int executingUserId, int namespaceIdentifier,
      List<DeHubUserPermission> userPermissions) throws IllegalAccessException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (GrantTypeHandler.getGrantTypeByUserAndNamespaceIdentifier(ctx, executingUserId,
          namespaceIdentifier) != GrantType.ADMIN) {
        throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
      }

      userPermissions.forEach(up -> {
        DehubUser user = UserHandler.getUserByIdentity(ctx, up.getUserAuthId());

        if (up.getGrantType() == null || up.getGrantType().isEmpty()) {
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
              GrantType.valueOf(up.getGrantType().toUpperCase()));
        }
      });
    }
  }

}
