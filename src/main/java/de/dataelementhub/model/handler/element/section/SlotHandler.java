package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.tables.Slot.SLOT;

import de.dataelementhub.dal.jooq.Tables;
import de.dataelementhub.dal.jooq.tables.Element;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Slot;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.CloseableDSLContext;

public class SlotHandler {

  /**
   * Get slots for an identifier.
   */
  public static List<Slot> get(CloseableDSLContext ctx, Identification identifier) {
    de.dataelementhub.dal.jooq.tables.Slot slot = Tables.SLOT.as("slot");
    de.dataelementhub.dal.jooq.tables.ScopedIdentifier si = SCOPED_IDENTIFIER.as("si");
    Element ns = ELEMENT.as("ns");

    List<de.dataelementhub.dal.jooq.tables.pojos.Slot> slots =
        ctx.select()
            .from(slot)
            .leftJoin(si).on(slot.SCOPED_IDENTIFIER_ID.equal(si.ID))
            .leftJoin(ns).on(ns.ID.eq(si.NAMESPACE_ID))
            .where(si.IDENTIFIER.eq(identifier.getIdentifier()))
            .and(si.VERSION.eq(identifier.getRevision()))
            .and(si.ELEMENT_TYPE.eq(identifier.getElementType()))
            .and(si.NAMESPACE_ID.eq(identifier.getNamespaceId()))
            .fetchInto(slot).into(de.dataelementhub.dal.jooq.tables.pojos.Slot.class);

    return convert(slots);
  }


  /**
   * Get a Slot.
   */
  public static List<Slot> get(CloseableDSLContext ctx, String urn) {
    return get(ctx, IdentificationHandler.fromUrn(ctx, urn));
  }

  /**
   * Convert a List of Slot POJOs from DataElementHub DAL to a List of Slot object of DataElementHub
   * Model.
   */
  public static List<Slot> convert(List<de.dataelementhub.dal.jooq.tables.pojos.Slot> slotPojos) {
    return slotPojos.stream().map(SlotHandler::convert).collect(Collectors.toList());
  }

  /**
   * Convert a Slot POJO from DataElementHub DAL to a Slot object of DataElementHub Model.
   */
  public static Slot convert(de.dataelementhub.dal.jooq.tables.pojos.Slot slot) {
    Slot newSlot = new Slot();
    newSlot.setName(slot.getKey());
    newSlot.setValue(slot.getValue());
    return newSlot;
  }

  /**
   * Convert a Slot object of DataElementHub Model to a Slot object of DataElementHub DAL.
   */
  public static de.dataelementhub.dal.jooq.tables.pojos.Slot convert(Slot slot,
      int scopedIdentifierId) {
    de.dataelementhub.dal.jooq.tables.pojos.Slot newSlot
        = new de.dataelementhub.dal.jooq.tables.pojos.Slot();
    newSlot.setKey(slot.getName());
    newSlot.setValue(slot.getValue());
    newSlot.setScopedIdentifierId(scopedIdentifierId);
    return newSlot;
  }

  /**
   * Convert a list of slots.
   */
  public static List<de.dataelementhub.dal.jooq.tables.pojos.Slot> convert(List<Slot> slots,
      int scopedIdentifierId) {
    if (slots == null) {
      return new ArrayList<>();
    } else {
      return slots.stream().map(s -> convert(s, scopedIdentifierId)).collect(Collectors.toList());
    }
  }

  /**
   * Create a list of Slots.
   */
  public static void create(CloseableDSLContext ctx, List<Slot> slots, int scopedIdentifierId) {
    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    List<de.dataelementhub.dal.jooq.tables.pojos.Slot> dalSlots = SlotHandler
        .convert(slots, scopedIdentifierId);
    saveSlots(ctx, dalSlots);
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
  }

  /**
   * Save a list of slots.
   */
  public static void saveSlots(CloseableDSLContext ctx,
      List<de.dataelementhub.dal.jooq.tables.pojos.Slot> slots) {
    slots.forEach(d -> ctx.newRecord(SLOT, d).store());
  }

  /**
   * Save a slot.
   */
  public static void saveSlot(CloseableDSLContext ctx,
      de.dataelementhub.dal.jooq.tables.pojos.Slot slot) {
    ctx.newRecord(SLOT, slot).store();
  }

  /**
   * Copy slots from one scoped identifier to another.
   */
  public static void copySlots(CloseableDSLContext ctx, Integer sourceId, Integer targetId) {
    List<de.dataelementhub.dal.jooq.tables.pojos.Slot> slots = ctx.selectFrom(SLOT)
        .where(SLOT.SCOPED_IDENTIFIER_ID.eq(sourceId))
        .fetchInto(de.dataelementhub.dal.jooq.tables.pojos.Slot.class);

    slots.forEach(s -> {
      s.setId(null);
      s.setScopedIdentifierId(targetId);
      ctx.newRecord(SLOT, s).store();
    });
  }

  /**
   * Delete all Slots belonging to a given element (by element scoped identifier).
   */
  public static void deleteSlotsByElementUrnId(CloseableDSLContext ctx, int userId,
      ScopedIdentifier scopedIdentifier) {
    ctx.deleteFrom(SLOT).where(SLOT.SCOPED_IDENTIFIER_ID.eq(scopedIdentifier.getId()))
        .execute();
  }

  /**
   * Update the slots of an existing element.
   */
  public static void updateSlots(CloseableDSLContext ctx, int userId, String urn,
      List<Slot> slots) {
    ScopedIdentifier elementScopedIdentifier = IdentificationHandler.getScopedIdentifier(ctx, urn);
    // Delete the old slots first because updating can also remove slots.
    deleteSlotsByElementUrnId(ctx, userId, elementScopedIdentifier);
    create(ctx, slots, elementScopedIdentifier.getId());
  }
}
