package de.dataelementhub.model.handler.importhandler;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.STAGING;
import static org.jooq.impl.DSL.currentLocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.handler.element.DataElementGroupHandler;
import de.dataelementhub.model.handler.element.DataElementHandler;
import de.dataelementhub.model.handler.element.RecordHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValueHandler;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.jooq.CloseableDSLContext;
import org.jooq.Result;

public class StagedElementHandler {

  /** Get stagedElement Members by ID. */
  public static List<de.dataelementhub.model.dto.listviews.StagedElement> getStagedElementMembers(
      CloseableDSLContext ctx, int importId, int userId, String stagedElementId) {
    List<String> stagedElementMembersIds = List.of(ctx.select(STAGING.MEMBERS)
            .from(STAGING)
            .where(STAGING.IMPORT_ID.eq(importId))
            .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId)).fetchOne().into(String.class)
            .split(";"));
    Result<org.jooq.Record> stagingRecords = ctx
        .select()
        .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
        .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
            .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
        .and(STAGING.STAGED_ELEMENT_ID.in(stagedElementMembersIds))
        .fetch();
    List<de.dataelementhub.model.dto.listviews.StagedElement> stagedElementMembers =
        new ArrayList<>();
    for (org.jooq.Record sr : stagingRecords) {
      de.dataelementhub.model.dto.listviews.StagedElement stagedElement =
          new de.dataelementhub.model.dto.listviews.StagedElement();
      stagedElement.setStagedElementId(sr.getValue(STAGING.STAGED_ELEMENT_ID));
      if (sr.getValue(STAGING.SCOPED_IDENTIFIER_ID) != null) {
        stagedElement.setElementUrn(IdentificationHandler
            .toUrn(ctx, sr.getValue(STAGING.SCOPED_IDENTIFIER_ID)));
      }
      stagedElement.setElementType(sr.getValue(STAGING.ELEMENT_TYPE));
      stagedElement.setDesignation(sr.getValue(STAGING.DESIGNATION));
      stagedElementMembers.add(stagedElement);
    }
    return stagedElementMembers;
  }

  /** Get stagedElement by ID. */
  public static de.dataelementhub.model.dto.element.StagedElement getStagedElement(
      CloseableDSLContext ctx, int importId, int userId, String stagedElementId) {
    String stagedElementAsString = String.valueOf(ctx.select(STAGING.DATA)
        .from(STAGING)
        .where(STAGING.IMPORT_ID.eq(importId))
        .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId))
        .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
            .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
        .fetchOneInto(String.class));
    try {
      return stagedElementAsString != null ? new ObjectMapper().readValue(stagedElementAsString,
              StagedElement.class) : null;
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  /** Converts StagedElement to draft. */
  public static String stagedElementToElement(CloseableDSLContext ctx, StagedElement stagedElement,
      String namespaceUrn, int userId, int importId) throws IllegalAccessException {
    Identification identification = new Identification();
    identification.setNamespaceUrn(namespaceUrn);
    identification.setStatus(Status.DRAFT);
    ElementType elementType = stagedElement.getIdentification().getElementType();
    identification.setElementType(elementType);
    ScopedIdentifier scopedIdentifier;
    List<Member> members;
    String convertedUrn = getUrnIfConvertedAndDraftStillAvailable(ctx,
        importId, stagedElement.getValueDomainUrn());
    if (!convertedUrn.equals("")) { // if element already exist return urn
      return convertedUrn;
    }
    switch (elementType) {
      case DATAELEMENT:
        DataElement dataElement = new DataElement();
        dataElement.setDefinitions(stagedElement.getDefinitions());
        dataElement.setSlots(stagedElement.getSlots());
        dataElement.setConceptAssociations(stagedElement.getConceptAssociations());
        dataElement.setIdentification(identification);
        String valueDomainUrn = getUrnIfConvertedAndDraftStillAvailable(ctx, importId,
            stagedElement.getValueDomainUrn());
        if (valueDomainUrn.equals("")) {
          StagedElement memberAsStagedElement = getStagedElement(ctx, importId, userId,
              stagedElement.getValueDomainUrn());
          valueDomainUrn = stagedElementToElement(ctx, memberAsStagedElement,
              namespaceUrn, userId, importId);
        }
        dataElement.setValueDomainUrn(valueDomainUrn);
        scopedIdentifier = DataElementHandler.create(ctx, userId, dataElement);
        markAsConverted(ctx, userId, importId, stagedElement.getIdentification().getUrn(),
            scopedIdentifier);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case DATAELEMENTGROUP:
        DataElementGroup dataElementGroup = new DataElementGroup();
        dataElementGroup.setDefinitions(stagedElement.getDefinitions());
        dataElementGroup.setSlots(stagedElement.getSlots());
        dataElementGroup.setIdentification(identification);
        members =
            handleMembers(ctx, importId, namespaceUrn, userId, stagedElement.getMembers());
        dataElementGroup.setMembers(members);
        scopedIdentifier = DataElementGroupHandler.create(ctx, userId, dataElementGroup);
        markAsConverted(ctx, userId, importId, stagedElement.getIdentification().getUrn(),
            scopedIdentifier);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case RECORD:
        Record record = new Record();
        record.setDefinitions(stagedElement.getDefinitions());
        record.setSlots(stagedElement.getSlots());
        record.setIdentification(identification);
        members =
            handleMembers(ctx, importId, namespaceUrn, userId, stagedElement.getMembers());
        record.setMembers(members);
        scopedIdentifier = RecordHandler.create(ctx, userId, record);
        markAsConverted(ctx, userId, importId, stagedElement.getIdentification().getUrn(),
            scopedIdentifier);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case ENUMERATED_VALUE_DOMAIN:
      case DESCRIBED_VALUE_DOMAIN:
        ValueDomain valueDomain = new ValueDomain();
        valueDomain.setDefinitions(stagedElement.getDefinitions());
        valueDomain.setSlots(stagedElement.getSlots());
        valueDomain.setType(stagedElement.getType());
        valueDomain.setText(stagedElement.getText());
        valueDomain.setIdentification(identification);
        valueDomain.setConceptAssociations(stagedElement.getConceptAssociations());
        valueDomain.setDatetime(stagedElement.getDatetime());
        valueDomain.setPermittedValues(stagedElement.getPermittedValues());
        scopedIdentifier = ValueDomainHandler.create(ctx, userId, valueDomain);
        markAsConverted(ctx, userId, importId, stagedElement.getIdentification().getUrn(),
            scopedIdentifier);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case PERMISSIBLE_VALUE:
        PermittedValue permittedValue = new PermittedValue();
        permittedValue.setIdentification(identification);
        permittedValue.setDefinitions(stagedElement.getDefinitions());
        permittedValue.setSlots(stagedElement.getSlots());
        permittedValue.setUrn(stagedElement.getUrn());
        permittedValue.setConceptAssociations(stagedElement.getConceptAssociations());
        scopedIdentifier = PermittedValueHandler.create(ctx, userId, permittedValue);
        markAsConverted(ctx, userId, importId, stagedElement.getIdentification().getUrn(),
            scopedIdentifier);
        return IdentificationHandler.toUrn(scopedIdentifier);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /** Check if members are already converted to Drafts. */
  public static List<Member> handleMembers(
      CloseableDSLContext ctx, int importId, String namespaceUrn,
      int userId, List<Member> members) {
    members.forEach(
        (member -> {
          String memberUrn;
          memberUrn = getUrnIfConvertedAndDraftStillAvailable(ctx, importId,
              member.getElementUrn());
          if (memberUrn.equals("")) {
            StagedElement memberAsStagedElement = getStagedElement(ctx, importId, userId,
                member.getElementUrn());
            try {
              memberUrn = stagedElementToElement(ctx, memberAsStagedElement,
                  namespaceUrn, userId, importId);
            } catch (IllegalAccessException e) {
              throw new IllegalArgumentException();
            }
          }
          member.setElementUrn(memberUrn);
        })
    );
    return members;
  }

  /** Check if stagedElement is already converted to Draft and was not deleted. */
  public static String getUrnIfConvertedAndDraftStillAvailable(
      CloseableDSLContext ctx, int importId, String stagedElementId) {
    try {
      Integer scopedIdentifierId = ctx.selectFrom(STAGING).where(STAGING.IMPORT_ID.eq(importId))
          .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId))
          .fetchOne().getValue(STAGING.SCOPED_IDENTIFIER_ID);
      if (scopedIdentifierId != null) {
        ScopedIdentifier scopedIdentifier = ctx.selectFrom(SCOPED_IDENTIFIER)
            .where(SCOPED_IDENTIFIER.ID.eq(scopedIdentifierId))
            .fetchOne().into(ScopedIdentifier.class);
        if (scopedIdentifier != null) {
          return IdentificationHandler.toUrn(scopedIdentifier);
        }
      }
    } catch (NullPointerException e) {
      return "";
    }
    return "";
  }

  /** Mark a stagedElement as converted and save conversion details. */
  public static void markAsConverted(
      CloseableDSLContext ctx, int userId, int importId, String stagedElementId,
      ScopedIdentifier scopedIdentifier) {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis()
        - TimeZone.getDefault().getOffset(new Date().getTime()));
    ctx.update(STAGING)
        .set(STAGING.SCOPED_IDENTIFIER_ID, scopedIdentifier.getId())
        .set(STAGING.CONVERTED_AT, currentLocalDateTime())
        .set(STAGING.CONVERTED_BY, userId)
        .where(STAGING.IMPORT_ID.eq(importId))
        .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId))
        .execute();
  }
}
