package de.dataelementhub.model.handler.element;

import static java.util.stream.Collectors.toList;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

/**
 * Record Handler.
 */
@Slf4j
public class RecordHandler extends ElementHandler {

  /**
   * Get a record by its urn.
   */
  public static Record get(DSLContext ctx, int userId, Identification identification) {
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);

    Record record = new Record();
    record.setIdentification(identification);
    record.setDefinitions(element.getDefinitions());
    record.setMembers(MemberHandler.get(ctx, identification));
    record.setSlots(element.getSlots());
    return record;
  }


  /** Create a new Record and return its new Scoped identifier. */
  public static ScopedIdentifier create(
      DSLContext ctx, int userId, Record record)
      throws IllegalArgumentException {

    // Check if all member urns are present
    List<Element> members = ElementHandler
        .fetchByUrns(ctx, userId, record.getMembers().stream().map(Member::getElementUrn)
            .collect(toList()));
    if (members.size() != record.getMembers().size()) {
      throw new IllegalArgumentException();
    }

    if (record.getIdentification().getStatus() == Status.RELEASED) {
      if (members.stream().anyMatch(m -> m.getIdentification().getStatus() == Status.DRAFT)) {
        throw new IllegalArgumentException(
            "Released Record may not contain draft members.");
      }
    }

    de.dataelementhub.dal.jooq.tables.pojos.Element element =
        new de.dataelementhub.dal.jooq.tables.pojos.Element();
    element.setElementType(ElementType.RECORD);
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(saveElement(ctx, element));
    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(
            ctx, userId, record.getIdentification(), element.getId());
    DefinitionHandler.create(
        ctx, record.getDefinitions(), element.getId(), scopedIdentifier.getId());
    if (record.getSlots() != null) {
      SlotHandler.create(ctx, record.getSlots(), scopedIdentifier.getId());
    }
    if (record.getMembers() != null) {
      MemberHandler.create(ctx, userId, members, scopedIdentifier.getId());
    }
    return scopedIdentifier;
  }

  /**
   * Update an identifier.
   */
  public static Identification update(DSLContext ctx, int userId,
      Record record, Record previousRecord) throws IllegalAccessException {

    // If the members changed in any way and the status is not draft, an update is not allowed
    if (!record.getMembers().equals(previousRecord.getMembers())
        && previousRecord.getIdentification().getStatus() != Status.DRAFT) {
      throw new IllegalArgumentException("Updating members is only allowed for DRAFT records");
    }

    //update scopedIdentifier if status != DRAFT
    if (previousRecord.getIdentification().getStatus() != Status.DRAFT) {
      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, record.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, record.getIdentification()).getId());
      record.setIdentification(IdentificationHandler.convert(ctx, scopedIdentifier));

      record.getIdentification().setNamespaceId(IdentificationHandler.getScopedIdentifier(ctx,
          previousRecord.getIdentification().getUrn()).getNamespaceId());
    }

    delete(ctx, userId, previousRecord.getIdentification().getUrn());
    create(ctx, userId, record);

    return record.getIdentification();
  }

  /**
   * Update record members.
   *
   * @return the new record identification if at least one member has new version -
   *     otherwise the old identification is returned
   */
  public static Identification updateMembers(DSLContext ctx, int userId,
      ScopedIdentifier scopedIdentifier) {
    Identification identification = IdentificationHandler.convert(ctx, scopedIdentifier);
    if (MemberHandler.newMemberVersionExists(ctx, scopedIdentifier)) {
      Record record = get(ctx, userId, identification);
      if (record.getIdentification().getStatus() != Status.DRAFT) {
        ScopedIdentifier newsScopedIdentifier =
            IdentificationHandler.update(ctx, userId, identification,
                scopedIdentifier.getElementId());
        record.setIdentification(IdentificationHandler
            .convert(ctx, newsScopedIdentifier));
        record.getIdentification()
            .setNamespaceId(scopedIdentifier.getNamespaceId());
      }
      try {
        delete(ctx, userId, identification.getUrn());
        ScopedIdentifier si = create(ctx, userId, record);
        MemberHandler.updateMembers(ctx, si);
      } catch (IllegalStateException e) {
        log.debug("No need to delete already outdated element.");
      }
    }
    return identification;
  }
}
