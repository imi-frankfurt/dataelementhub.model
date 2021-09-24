package de.dataelementhub.model.service;

import static de.dataelementhub.dal.jooq.Tables.CONCEPTS;
import static de.dataelementhub.dal.jooq.Tables.CONCEPT_ELEMENT_ASSOCIATIONS;
import static de.dataelementhub.dal.jooq.Tables.DEFINITION;
import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.SLOT;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.search.SearchRequest;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SearchService {

  private ElementService elementService;

  @Autowired
  public SearchService(ElementService elementService) {
    this.elementService = elementService;
  }

  /**
   * Returns search results matching all specifications in searchRequest as a list of
   * DataElementHubElements. If no results found an empty list will be returned.
   */
  public List<Element> search(SearchRequest searchRequest, int userId) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<ScopedIdentifier> scopedIdentifiers = new ArrayList<>();
      if (searchRequest.getElementParts().contains("definition")) {
        scopedIdentifiers.addAll(definitionSearch(ctx, searchRequest, "definition"));
      }
      if (searchRequest.getElementParts().contains("designation")) {
        scopedIdentifiers.addAll(definitionSearch(ctx, searchRequest, "designation"));
      }
      if (searchRequest.getElementParts().contains("slotKey")) {
        scopedIdentifiers.addAll(slotSearch(ctx, searchRequest, "key"));
      }
      if (searchRequest.getElementParts().contains("slotValue")) {
        scopedIdentifiers.addAll(slotSearch(ctx, searchRequest, "value"));
      }
      if (searchRequest.getElementParts().contains("conceptAssociationSystem")) {
        scopedIdentifiers.addAll(conceptsSearch(ctx, searchRequest, "system"));
      }
      if (searchRequest.getElementParts().contains("conceptAssociationTerm")) {
        scopedIdentifiers.addAll(conceptsSearch(ctx, searchRequest, "term"));
      }
      if (searchRequest.getElementParts().contains("conceptAssociationText")) {
        scopedIdentifiers.addAll(conceptsSearch(ctx, searchRequest, "text"));
      }
      if (searchRequest.getElementParts().contains("dataType")) {
        scopedIdentifiers.addAll(elementSearch(ctx, searchRequest, "datatype"));
      }
      if (searchRequest.getElementParts().contains("valueDomainDescription")) {
        scopedIdentifiers.addAll(elementSearch(ctx, searchRequest, "description"));
      }
      if (searchRequest.getElementParts().contains("unitOfMeasure")) {
        scopedIdentifiers.addAll(elementSearch(ctx, searchRequest, "unit_of_measure"));
      }
      if (searchRequest.getElementParts().contains("format")) {
        scopedIdentifiers.addAll(elementSearch(ctx, searchRequest, "format"));
      }
      return scopedIdentifiersToElements(userId, scopedIdentifiers);
    }
  }

  /**
   * Converts a list of scopedIdentifiers to a list of DataElementHub Elements and removes
   * duplicates.
   */
  public List<Element> scopedIdentifiersToElements(int userId,
      List<ScopedIdentifier> scopedIdentifiers) {
    List<String> idList = new ArrayList<>();
    List<Element> results = new ArrayList<>();
    for (ScopedIdentifier scopedIdentifier : scopedIdentifiers) {
      String urn = IdentificationHandler.toUrn(scopedIdentifier);
      if (scopedIdentifier.getElementType().equals(ElementType.NAMESPACE)
          && !idList.contains(urn)) {
        idList.add(urn);
        results.add(elementService.read(userId,
            IdentificationHandler.getIdentifierFromUrn(urn).toString()));
      } else {
        if (!idList.contains(urn)) {
          idList.add(urn);
          results.add(elementService.read(userId, urn));
        }
      }
    }
    return results;
  }


  /**
   * Get scopedIdentifiers for related search results in definition table.
   */
  public List<ScopedIdentifier> definitionSearch(CloseableDSLContext ctx,
      SearchRequest searchRequest, String column) {
    return ctx.select(SCOPED_IDENTIFIER.IDENTIFIER,
            SCOPED_IDENTIFIER.VERSION,
            SCOPED_IDENTIFIER.ELEMENT_TYPE,
            SCOPED_IDENTIFIER.NAMESPACE_ID)
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(searchRequest.getType()))
        .and(SCOPED_IDENTIFIER.STATUS.in(searchRequest.getStatus()))
        .and(SCOPED_IDENTIFIER.ELEMENT_ID
            .in(ctx.select(DEFINITION.ELEMENT_ID)
                .from(DEFINITION)
                .where("to_tsvector_multilang(" + column + ") @@ to_tsquery(?)",
                    searchRequest.getSearchText())))
        .fetch().into(ScopedIdentifier.class);
  }


  /**
   * Get scopedIdentifiers for related search results in slot table.
   */
  public List<ScopedIdentifier> slotSearch(CloseableDSLContext ctx,
      SearchRequest searchRequest, String column) {
    return ctx.select(SCOPED_IDENTIFIER.IDENTIFIER,
            SCOPED_IDENTIFIER.VERSION,
            SCOPED_IDENTIFIER.ELEMENT_TYPE,
            SCOPED_IDENTIFIER.NAMESPACE_ID)
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(searchRequest.getType()))
        .and(SCOPED_IDENTIFIER.STATUS.in(searchRequest.getStatus()))
        .and(SCOPED_IDENTIFIER.ID
            .in(ctx.select(SLOT.SCOPED_IDENTIFIER_ID)
                .from(SLOT)
                .where("to_tsvector_multilang(" + column + ") @@ to_tsquery(?)",
                    searchRequest.getSearchText())))
        .fetch().into(ScopedIdentifier.class);
  }


  /**
   * Get scopedIdentifiers for related search results in concepts table.
   */
  public List<ScopedIdentifier> conceptsSearch(CloseableDSLContext ctx,
      SearchRequest searchRequest, String column) {
    return ctx.select(SCOPED_IDENTIFIER.IDENTIFIER,
            SCOPED_IDENTIFIER.VERSION,
            SCOPED_IDENTIFIER.ELEMENT_TYPE,
            SCOPED_IDENTIFIER.NAMESPACE_ID)
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(searchRequest.getType()))
        .and(SCOPED_IDENTIFIER.STATUS.in(searchRequest.getStatus()))
        .and(SCOPED_IDENTIFIER.ID
            .in(ctx.select(CONCEPT_ELEMENT_ASSOCIATIONS.SCOPEDIDENTIFIER_ID)
                .from(CONCEPT_ELEMENT_ASSOCIATIONS)
                .leftJoin(CONCEPTS)
                .on(CONCEPT_ELEMENT_ASSOCIATIONS.CONCEPT_ID.eq(CONCEPTS.ID))
                .where("to_tsvector_multilang(" + column + ") @@ to_tsquery(?)",
                    searchRequest.getSearchText())))
        .fetch().into(ScopedIdentifier.class);
  }

  /**
   * Get scopedIdentifiers for related search results in element table.
   */
  public List<ScopedIdentifier> elementSearch(CloseableDSLContext ctx,
      SearchRequest searchRequest, String column) {
    return ctx.select(SCOPED_IDENTIFIER.IDENTIFIER,
            SCOPED_IDENTIFIER.VERSION,
            SCOPED_IDENTIFIER.ELEMENT_TYPE,
            SCOPED_IDENTIFIER.NAMESPACE_ID)
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(searchRequest.getType()))
        .and(SCOPED_IDENTIFIER.STATUS.in(searchRequest.getStatus()))
        .and(SCOPED_IDENTIFIER.ELEMENT_ID
            .in(ctx.select(ELEMENT.ID)
                .from(ELEMENT)
                .where("to_tsvector_multilang(" + column + ") @@ to_tsquery(?)",
                    searchRequest.getSearchText())))
        .fetch().into(ScopedIdentifier.class);
  }
}
