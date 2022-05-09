package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.Tables.CONCEPTS;
import static de.dataelementhub.dal.jooq.Tables.CONCEPT_ELEMENT_ASSOCIATIONS;
import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;

import de.dataelementhub.dal.jooq.tables.Element;
import de.dataelementhub.dal.jooq.tables.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations;
import de.dataelementhub.dal.jooq.tables.records.ConceptElementAssociationsRecord;
import de.dataelementhub.dal.jooq.tables.records.ConceptsRecord;
import de.dataelementhub.model.dto.element.section.ConceptAssociation;
import de.dataelementhub.model.dto.element.section.Identification;
import java.util.ArrayList;
import java.util.List;
import org.jooq.CloseableDSLContext;

/**
 * Concept Association Handler.
 */
public class ConceptAssociationHandler {

  /**
   * Get a list of concept associations from a given urn from the database.
   *
   * @param identifier of the element associated to the concept association to get
   */
  public static List<ConceptAssociation> get(CloseableDSLContext ctx, Identification identifier) {
    de.dataelementhub.dal.jooq.tables.ConceptElementAssociations conceptElementAssociations
        = CONCEPT_ELEMENT_ASSOCIATIONS.as("ceassociation");
    de.dataelementhub.dal.jooq.tables.Concepts concepts = CONCEPTS.as("concepts");
    ScopedIdentifier si = SCOPED_IDENTIFIER.as("si");
    Element ns = ELEMENT.as("ns");
    ScopedIdentifier nsSi = SCOPED_IDENTIFIER.as("ns_si");

    List<ConceptElementAssociations> conceptElementAssociationsList =
        ctx.select()
            .from(conceptElementAssociations)
            .leftJoin(si).on(conceptElementAssociations.SCOPEDIDENTIFIER_ID.eq(si.ID))
            .leftJoin(ns).on(ns.ID.eq(si.NAMESPACE_ID))
            .leftJoin(nsSi).on(nsSi.ELEMENT_ID.eq(ns.ID))
            .leftJoin(concepts).on(conceptElementAssociations.CONCEPT_ID.eq(concepts.ID))
            .where(si.IDENTIFIER.eq(identifier.getIdentifier()))
            .and(si.VERSION.eq(identifier.getRevision()))
            .and(si.ELEMENT_TYPE.eq(identifier.getElementType()))
            .and(nsSi.IDENTIFIER.eq(
                IdentificationHandler.getNamespaceIdentifierFromUrn(identifier.getNamespaceUrn())))
            .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations.class);

    List<de.dataelementhub.dal.jooq.tables.pojos.Concepts> conceptList =
        new ArrayList<>(ctx.select()
            .from(concepts)
            .leftJoin(conceptElementAssociations)
            .on(concepts.ID.eq(conceptElementAssociations.CONCEPT_ID))
            .fetch()
            .into(concepts)
            .into(de.dataelementhub.dal.jooq.tables.pojos.Concepts.class));

    List<ConceptAssociation> conceptAssociations = new ArrayList<>();

    for (de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations cea :
        conceptElementAssociationsList) {
      ConceptAssociation conceptAssociation = new ConceptAssociation();
      conceptAssociation.setConceptId(cea.getConceptId());
      conceptAssociation.setScopedIdentifierId(cea.getScopedidentifierId());
      conceptAssociation.setLinktype(cea.getLinktype());

      de.dataelementhub.dal.jooq.tables.pojos.Concepts concept = conceptList.stream()
          .filter(c -> c.getId().equals(conceptAssociation.getConceptId())).findAny().get();
      conceptAssociation.setSystem(concept.getSystem());
      conceptAssociation.setText(concept.getText());
      conceptAssociation.setTerm(concept.getTerm());
      conceptAssociation.setVersion(concept.getVersion());
      conceptAssociation.setSourceId(concept.getSourceId());

      conceptAssociations.add(conceptAssociation);
    }

    return conceptAssociations;
  }

  /**
   * Get a List of concept association.
   */
  public static List<ConceptAssociation> get(CloseableDSLContext ctx, String urn) {
    return get(ctx, IdentificationHandler.fromUrn(urn));
  }

  /**
   * Saves the given list of concept associations in the database.
   */
  public static void save(CloseableDSLContext ctx,
      List<ConceptAssociation> conceptAssociations, Integer userId, int scopedIdentifierId) {
    if (conceptAssociations != null) {
      for (ConceptAssociation conceptAssociation : conceptAssociations) {
        save(ctx, conceptAssociation, userId, scopedIdentifierId);
      }
    }
  }


  /**
   * Saves the given concept association in the database. Expects a concept association linked to an
   * element.
   *
   * @param conceptAssociation the concept association to store
   */
  public static void save(CloseableDSLContext ctx, ConceptAssociation conceptAssociation,
      Integer userId, int scopedIdentifier) {
    de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations conceptElementAssociations =
        new de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations();
    conceptElementAssociations.setScopedidentifierId(scopedIdentifier);
    conceptElementAssociations.setLinktype(conceptAssociation.getLinktype());
    conceptElementAssociations.setCreatedBy(userId);

    de.dataelementhub.dal.jooq.tables.pojos.Concepts concepts =
        new de.dataelementhub.dal.jooq.tables.pojos.Concepts();
    concepts.setCreatedBy(userId);

    ConceptsRecord conceptRecord = ctx
        .fetchOne(CONCEPTS, CONCEPTS.TERM.eq(conceptAssociation.getTerm()));
    if (conceptRecord == null) {
      conceptRecord = ctx.newRecord(CONCEPTS, concepts);
    }

    conceptRecord.setVersion(conceptAssociation.getVersion());
    conceptRecord.setText(conceptAssociation.getText());
    conceptRecord.setTerm(conceptAssociation.getTerm());
    conceptRecord.setSystem(conceptAssociation.getSystem());
    conceptRecord.setSourceId(conceptAssociation.getSourceId());
    conceptRecord.store();

    Integer conceptId = conceptRecord.getId();

    conceptElementAssociations.setConceptId(conceptId);

    ConceptElementAssociationsRecord ceaRecord = ctx.fetchOne(CONCEPT_ELEMENT_ASSOCIATIONS,
        CONCEPT_ELEMENT_ASSOCIATIONS.SCOPEDIDENTIFIER_ID
            .eq(scopedIdentifier)
            .and(CONCEPT_ELEMENT_ASSOCIATIONS.CONCEPT_ID.eq(conceptId)));
    if (ceaRecord == null) {
      ceaRecord = ctx.newRecord(CONCEPT_ELEMENT_ASSOCIATIONS, conceptElementAssociations);
    }

    ceaRecord.store();
  }

  /**
   * Copy concept associations from one scoped identifier to another.
   */
  public static void copyConceptElementAssociations(CloseableDSLContext ctx, Integer userId,
      Integer sourceId, Integer targetId) {
    List<de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations>
        conceptElementAssociations =
        ctx.selectFrom(CONCEPT_ELEMENT_ASSOCIATIONS)
            .where(CONCEPT_ELEMENT_ASSOCIATIONS.SCOPEDIDENTIFIER_ID.eq(sourceId))
            .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.ConceptElementAssociations.class);

    conceptElementAssociations.forEach(cea -> {
      cea.setScopedidentifierId(targetId);
      cea.setCreatedBy(userId);
      cea.setCreatedAt(null);
      ctx.newRecord(CONCEPT_ELEMENT_ASSOCIATIONS, cea).store();
    });
  }

}
