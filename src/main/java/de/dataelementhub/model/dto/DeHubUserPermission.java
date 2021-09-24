package de.dataelementhub.model.dto;

import de.dataelementhub.dal.jooq.enums.GrantType;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class DeHubUserPermission {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private GrantType grantType;
}
