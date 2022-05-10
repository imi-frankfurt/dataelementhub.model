package de.dataelementhub.model.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Build Information DTO.
 */
@Getter
@Setter
@EqualsAndHashCode
public class BuildInformation {

  private String buildVersion;
  private String buildDate;
  private String buildBranch;
  private String buildHash;

}
