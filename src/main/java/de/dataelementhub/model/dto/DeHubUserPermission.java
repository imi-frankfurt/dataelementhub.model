package de.dataelementhub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class DeHubUserPermission {

  private String userAuthId;
  private String grantType;
}
