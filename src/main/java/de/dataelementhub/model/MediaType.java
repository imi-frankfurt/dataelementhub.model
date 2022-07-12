package de.dataelementhub.model;

/**
 * Media Type.
 */
public enum MediaType {

  JSON_DETAIL_VIEW("application/vnd+de.dataelementhub.detailview+json"),

  JSON_LIST_VIEW("application/vnd+de.dataelementhub.listview+json");

  private final String literal;

  MediaType(String literal) {
    this.literal = literal;
  }

  public String getLiteral() {
    return literal;
  }
}
