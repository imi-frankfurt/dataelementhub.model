package de.dataelementhub.model.handler.element;

import static java.util.stream.Collectors.toList;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import java.util.List;
import java.util.UUID;
import org.jooq.CloseableDSLContext;

/**
 * Dataelement Group Handler.
 */
public class DataElementGroupHandler extends ElementHandler {

  /**
   *  Create a new DataElementGroup and return its new ID.
   */
  public static DataElementGroup get(
      CloseableDSLContext ctx, int userId, Identification identification) {
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);

    DataElementGroup newDataElementGroup = new DataElementGroup();
    newDataElementGroup.setIdentification(identification);
    newDataElementGroup.setDefinitions(element.getDefinitions());
    newDataElementGroup.setMembers(MemberHandler.get(ctx, identification));
    newDataElementGroup.setSlots(element.getSlots());
    return newDataElementGroup;
  }

  /** Create a new DataElementGroup and return its new ID. */
  public static ScopedIdentifier create(
      CloseableDSLContext ctx, int userId, DataElementGroup dataElementGroup)
      throws IllegalArgumentException {

    // Check if all member urns are present
    List<Element> members = ElementHandler
        .fetchByUrns(ctx, userId, dataElementGroup.getMembers().stream().map(Member::getElementUrn)
            .collect(toList()));
    if (members.size() != dataElementGroup.getMembers().size()) {
      throw new IllegalArgumentException();
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.dataelementhub.dal.jooq.tables.pojos.Element element =
        new de.dataelementhub.dal.jooq.tables.pojos.Element();
    element.setElementType(ElementType.DATAELEMENTGROUP);
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(saveElement(ctx, element));
    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(
            ctx, userId, dataElementGroup.getIdentification(), element.getId());
    DefinitionHandler.create(
        ctx, dataElementGroup.getDefinitions(), element.getId(), scopedIdentifier.getId());
    if (dataElementGroup.getSlots() != null) {
      SlotHandler.create(ctx, dataElementGroup.getSlots(), scopedIdentifier.getId());
    }
    if (dataElementGroup.getMembers() != null) {
      MemberHandler.create(ctx, userId, members, scopedIdentifier.getId());
    }
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * Update a dataelementgroup.
   */
  public static Identification update(CloseableDSLContext ctx, int userId,
      DataElementGroup dataElementGroup, DataElementGroup previousDataElementGroup)
      throws IllegalAccessException {

    //update scopedIdentifier if status != DRAFT
    if (previousDataElementGroup.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, dataElementGroup.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, dataElementGroup.getIdentification())
                  .getId());
      dataElementGroup.setIdentification(IdentificationHandler.convert(ctx, scopedIdentifier));
      dataElementGroup.getIdentification().setNamespaceId(
          Integer.parseInt(previousDataElementGroup.getIdentification().getUrn().split(":")[1]));
    }

    delete(ctx, userId, previousDataElementGroup.getIdentification().getUrn());
    create(ctx, userId, dataElementGroup);

    return dataElementGroup.getIdentification();
  }

  /**
   * Update dataElementGroup members.
   *
   * @return the new dataElementGroup identification if at least one member has new version -
   *     otherwise the old identification is returned
   */
  public static Identification updateMembers(CloseableDSLContext ctx, int userId,
      ScopedIdentifier scopedIdentifier) {
    Identification identification = IdentificationHandler.convert(ctx, scopedIdentifier);
    DataElementGroup dataElementGroup = get(ctx, userId, identification);
    if (MemberHandler.newMemberVersionExists(ctx, scopedIdentifier)) {
      if (dataElementGroup.getIdentification().getStatus() != Status.DRAFT) {
        ScopedIdentifier newsScopedIdentifier =
            IdentificationHandler.update(ctx, userId, identification,
                scopedIdentifier.getElementId());
        dataElementGroup.setIdentification(IdentificationHandler
            .convert(ctx, newsScopedIdentifier));
        dataElementGroup.getIdentification()
            .setNamespaceId(scopedIdentifier.getNamespaceId());
      }
      delete(ctx, userId, identification.getUrn());
      ScopedIdentifier si = create(ctx, userId, dataElementGroup);
      MemberHandler.updateMembers(ctx, si);
    }
    return dataElementGroup.getIdentification();
  }
}
