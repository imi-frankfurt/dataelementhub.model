package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.tables.Element;
import de.dataelementhub.dal.jooq.tables.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.pojos.CodeSystem;
import de.dataelementhub.dal.jooq.tables.pojos.ValueDomainReference;
import de.dataelementhub.dal.jooq.tables.records.CodeSystemRecord;
import de.dataelementhub.dal.jooq.tables.records.ValueDomainReferenceRecord;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.ValueDomainReferenceDTO;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import org.jooq.DSLContext;

import static de.dataelementhub.dal.jooq.Tables.*;

/**
 * ValueDomain Reference Handler.
 */
public class ValueDomainReferencesHandler {

  /**
   * Get ValueDomainReference from a given urn from the database.
   *
   * @param identifier of the element associated to the ValueDomain Reference to get
   */
  public static ValueDomainReferenceDTO getValueDomainReference(DSLContext ctx, Identification identifier) {

    // Define table aliases
    de.dataelementhub.dal.jooq.tables.ValueDomainReference vdr = VALUE_DOMAIN_REFERENCE.as("vdr");
    de.dataelementhub.dal.jooq.tables.CodeSystem cs = CODE_SYSTEM.as("cs");
    de.dataelementhub.dal.jooq.tables.Source src = SOURCE.as("src");
    ScopedIdentifier si = SCOPED_IDENTIFIER.as("si");
    Element ns = ELEMENT.as("ns");
    ScopedIdentifier nsSi = SCOPED_IDENTIFIER.as("ns_si");

    // Query: load ValueDomainReference + CodeSystem  in a query and map directly into the DTO
    ValueDomainReferenceDTO dto = ctx.select(
                    vdr.SUBSET_URI.as("subsetUri"),
                    vdr.SCOPEDIDENTIFIER_ID.as("scopedIdentifierId"),
                    cs.ID.as("codeSystemId"),
                    cs.VERSION.as("codeSystemVersion"),
                    cs.SOURCE_ID.as("codeSystemSourceId")
            )
            .from(vdr)
            .leftJoin(si).on(vdr.SCOPEDIDENTIFIER_ID.eq(si.ID))
            .leftJoin(ns).on(ns.ID.eq(si.NAMESPACE_ID))
            .leftJoin(nsSi).on(nsSi.ELEMENT_ID.eq(ns.ID))
            .leftJoin(cs).on(vdr.CODE_SYSTEM_ID.eq(cs.ID))
            .leftJoin(src).on(cs.SOURCE_ID.eq(src.ID))
            .where(si.IDENTIFIER.eq(identifier.getIdentifier()))
            .and(si.VERSION.eq(identifier.getRevision()))
            .and(si.ELEMENT_TYPE.eq(identifier.getElementType()))
            .and(nsSi.IDENTIFIER.eq(
                    IdentificationHandler.getNamespaceIdentifierFromUrn(identifier.getNamespaceUrn())))
            .fetchOneInto(ValueDomainReferenceDTO.class);

    if (dto == null) {
      return null;
    }

    // Mapping DTO -> Domain Objects

    ValueDomainReference vdrDomain = new ValueDomainReference();
    vdrDomain.setSubsetUri(dto.getSubsetUri());
    vdrDomain.setScopedidentifierId(dto.getScopedIdentifierId());

    CodeSystem csDomain = new CodeSystem();
    csDomain.setId(dto.getCodeSystemId());
    csDomain.setVersion(dto.getVersion());
    csDomain.setSourceId(dto.getSourceId());
    // create final ValueDomainReferences Object
    return new ValueDomainReferenceDTO(csDomain, vdrDomain);
  }

  /**
   * Get a valueDomainReference.
   * overloaded method that accepts the URN directly without the caller having to build an identification object first.
   * Code remains consistent
   */
  public static ValueDomainReferenceDTO getValueDomainReference(DSLContext ctx, String urn) {
    return getValueDomainReference(ctx, IdentificationHandler.fromUrn(ctx, urn));
  }

  /**
   * Saves the given valueDomainReference in the database.
   */
  public static void save(DSLContext ctx,
                          ValueDomainReferenceDTO valueDomainReferences, Integer userId, int scopedIdentifierId){
    if (valueDomainReferences != null) {
      System.out.println(valueDomainReferences);
      saveVDR(ctx, valueDomainReferences, userId, scopedIdentifierId);

    }
  }


  /**
   * Saves the given valueDomainReference in the database. Expects a ValueDomain Reference linked to an
   * element.
   *
   * @param valueDomainReference the valueDomainReference to store
   */
  public static void saveVDR(DSLContext ctx, ValueDomainReferenceDTO valueDomainReference,
                          Integer userId, int scopedIdentifierId){

    if (valueDomainReference == null) {
      System.out.println("no vdr submitted");
      return;
    }

    // Table Alias
    de.dataelementhub.dal.jooq.tables.pojos.ValueDomainReference vdrPojo =
            new de.dataelementhub.dal.jooq.tables.pojos.ValueDomainReference();
    // Prepare ValueDomainReferences POJO
    vdrPojo.setScopedidentifierId(scopedIdentifierId);
    vdrPojo.setSubsetUri(valueDomainReference.getSubsetUri());
    vdrPojo.setCreatedBy(userId);

    de.dataelementhub.dal.jooq.tables.pojos.CodeSystem csPojo =
            new de.dataelementhub.dal.jooq.tables.pojos.CodeSystem();

    // Prepare CodeSystem POJO
    csPojo.setVersion(valueDomainReference.getVersion());
    csPojo.setCreatedBy(userId);

    // Save or update CodeSystem
    CodeSystemRecord csRecord;
    if (valueDomainReference.getCodeSystemId() != null) {
      csRecord= ctx.fetchOne(CODE_SYSTEM,
              CODE_SYSTEM.ID.eq(valueDomainReference.getCodeSystemId()));
    } else {
      csRecord = ctx.fetchOne(CODE_SYSTEM,
              CODE_SYSTEM.SOURCE_ID.eq(valueDomainReference.getSourceId())
                      .and(CODE_SYSTEM.VERSION.eq(valueDomainReference.getVersion())));
    }
    if (csRecord == null) {
      csRecord = ctx.newRecord(CODE_SYSTEM, csPojo);
      csRecord.setVersion(valueDomainReference.getVersion());
      csRecord.setSourceId(valueDomainReference.getSourceId());
      csRecord.setCreatedBy(userId);
      csRecord.store();
    }


    Integer codeSystemId = csRecord.getId();

    // Save or update ValueDomainReferences
    vdrPojo.setCodeSystemId(codeSystemId);

    ValueDomainReferenceRecord vdrRecord = ctx.fetchOne(VALUE_DOMAIN_REFERENCE,
            VALUE_DOMAIN_REFERENCE.SCOPEDIDENTIFIER_ID.eq(scopedIdentifierId)
                    .and(VALUE_DOMAIN_REFERENCE.CODE_SYSTEM_ID.eq(codeSystemId)));

    if (vdrRecord == null) {
      vdrRecord = ctx.newRecord(VALUE_DOMAIN_REFERENCE, vdrPojo);
    } else {
      vdrRecord.setSubsetUri(valueDomainReference.getSubsetUri());
    }
    vdrRecord.store();
    System.out.println("CodeSystem stored with ID: " + csRecord.getId());
    System.out.println("ValueDomainReference stored with ID: " + vdrRecord.getScopedidentifierId());
  }


  /**
   * Copy a ValueDomainReference from one scoped identifier to another.
   */
  public static void copyValueDomainReferences(DSLContext ctx, Integer userId,
                                               Integer sourceId, Integer targetId) {
    ValueDomainReference
            valueDomainReference =
            (ValueDomainReference) ctx.selectFrom(VALUE_DOMAIN_REFERENCE)
                .where(VALUE_DOMAIN_REFERENCE.SCOPEDIDENTIFIER_ID.eq(sourceId))
                .fetchInto(ValueDomainReference.class);

    valueDomainReference.setScopedidentifierId(targetId);
    valueDomainReference.setCreatedBy(userId);
    valueDomainReference.setCreatedAt(null);
    ctx.newRecord(VALUE_DOMAIN_REFERENCE, valueDomainReference).store();
  }

}
