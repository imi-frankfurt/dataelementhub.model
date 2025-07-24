package de.dataelementhub.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Dehub User Permission DTO.
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
public class DeHubUserPermission {

  private String userAuthId;
  private String accessLevel;
}
