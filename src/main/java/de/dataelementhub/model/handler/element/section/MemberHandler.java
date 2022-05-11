package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.tables.ScopedIdentifier.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.tables.ScopedIdentifierHierarchy.SCOPED_IDENTIFIER_HIERARCHY;

import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.CloseableDSLContext;

/**
 * Member Handler.
 */
public class MemberHandler {

  /**
   * Create Members (DataElements/DataElementGroups/Records) for a given DataElementGroup or
   * Record.
   **/
  public static void create(
      CloseableDSLContext ctx, int userId, List<Element> members, int superScopedIdentifierId)
      throws IllegalArgumentException {
    if (members != null || !(members.isEmpty())) {
      saveElementsAsMembers(ctx, members, superScopedIdentifierId, userId);
    }
    //TODO: check member.status
  }

  /**
   * get all Members (DataElements/DataElementGroups/Records) for a given DataElementGroup, Record.
   **/
  public static List<Member> get(
      CloseableDSLContext ctx, Identification identification) {
    List<Member> members = new ArrayList<>();
    List<Integer> subIds = getSubIds(ctx, identification);
    List<de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        getScopedIdentifiers(ctx, subIds/*, identification.getElementType()*/);
    scopedIdentifiers.forEach(
        (scopedIdentifier) -> {
          Member member = new Member();
          member.setElementUrn(IdentificationHandler.toUrn(scopedIdentifier));
          member.setStatus(scopedIdentifier.getStatus());
          members.add(member);
        });
    return members;
  }

  /**
   * Update Members (DataElements/Records) for a DataElementGroup or Record.
   **/
  public static void update(
      CloseableDSLContext ctx, int userId, Member members, int superScopedIdentifierId)  {
    // TODO
  }

  /**
   * Save the relationship between DataElements and their parent DataElementGroup/Record.
   **/
  public static void saveElementsAsMembers(
      CloseableDSLContext ctx, List<Element> members, int superScopedIdentifierId, int userId)
      throws IllegalArgumentException {
    members.forEach(
        (member) -> saveElementAsMember(ctx, member, superScopedIdentifierId, userId));
  }

  /**
   * Save the relationship between DataElements and their parent DataElementGroup/Record.
   **/
  public static void saveElementAsMember(
      CloseableDSLContext ctx, Element member, int superScopedIdentifierId, int userId)
      throws IllegalArgumentException {
    saveScopedIdentifierHierarchy(ctx, superScopedIdentifierId, member.getIdentification(), userId);
  }

  /**
   * Save relationship between SuperScopedIdentifierId & SubScopedIdentifierId into DB.
   **/
  public static void saveScopedIdentifierHierarchy(CloseableDSLContext ctx,
      Integer superScopedIdentifierId, Identification identification, int userId) {
    int scopedIdentifierId = IdentificationHandler.getScopedIdentifier(ctx, identification)
        .getId();
    ctx.insertInto(SCOPED_IDENTIFIER_HIERARCHY)
        .set(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID, superScopedIdentifierId)
        .set(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID, scopedIdentifierId)
        .execute();
  }

  /**
   * get subIds for a given superScopedIdentifierId.
   **/
  public static List<Integer> getSubIds(CloseableDSLContext ctx, Identification identification) {
    return ctx.select()
        .from(
            de.dataelementhub.dal.jooq.tables.Element.ELEMENT
                .join(SCOPED_IDENTIFIER)
                .on(
                    de.dataelementhub.dal.jooq.tables.Element.ELEMENT.ID.eq(
                        SCOPED_IDENTIFIER.ELEMENT_ID))
                .join(SCOPED_IDENTIFIER_HIERARCHY)
                .on(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID)))
        .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
        .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
        .and(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(identification.getNamespaceId()))
        .fetch()
        .getValues(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID);
  }

  /**
   * get scopedIdentifiers for a given list of subIds (just one ElementType).
   **/
  public static List<de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier> getScopedIdentifiers(
      CloseableDSLContext ctx, List<Integer> subIds) {
    return ctx.select()
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ID.in(subIds))
        .fetchInto(SCOPED_IDENTIFIER)
        .into(de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier.class);
  }

  /**
   * Get members scopedIdentifiers.
   **/
  public static List<ScopedIdentifier> getMembersScopedIdentifiers(
      CloseableDSLContext ctx, ScopedIdentifier scopedIdentifier) {
    return ctx.selectFrom(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ID.in(ctx.select().from(SCOPED_IDENTIFIER_HIERARCHY)
            .where(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID.eq(scopedIdentifier.getId()))
            .fetch().getValues(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID)))
        .fetchInto(ScopedIdentifier.class);
  }

  /**
   * Check if all SubIds are Released.
   **/
  public static Boolean allSubIdsAreReleased(CloseableDSLContext ctx,
      Identification identification) {
    List<Integer> subIds = getSubIds(ctx, identification);
    AtomicReference<Boolean> allReleased = new AtomicReference<>(true);
    List<de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        new ArrayList<>();
    scopedIdentifiers.addAll(getScopedIdentifiers(ctx, subIds));
    scopedIdentifiers.forEach(
        (scopedIdentifier) -> {
          if (scopedIdentifier.getStatus() != Status.RELEASED) {
            allReleased.set(false);
          }
        });
    return allReleased.get();
  }

  /**
   * remove unwanted relationships between a given superScopedIdentifierId &
   * subScopedIdentifierIds.
   **/
  public static void deleteUnmentionedSubIds(
      CloseableDSLContext ctx, List<Integer> newSubIds, int superScopedIdentifierId) {
    // TODO
  }

  /**
   *  Check if any members have new versions.
   **/
  public static Boolean newMemberVersionExists(CloseableDSLContext ctx,
      ScopedIdentifier scopedIdentifier) {
    List<ScopedIdentifier> currentMembersScopedIdentifiers =
        getMembersScopedIdentifiers(ctx, scopedIdentifier);
    Map<Integer, Integer> oldNewSiId = new HashMap<Integer, Integer>();
    for (ScopedIdentifier memberSi : currentMembersScopedIdentifiers) {
      Integer newestVersionSiId = ctx.selectFrom(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(memberSi.getNamespaceId()))
          .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(memberSi.getElementType()))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(memberSi.getIdentifier()))
          .orderBy(SCOPED_IDENTIFIER.VERSION.desc()).limit(1)
          .fetchOne().getValue(SCOPED_IDENTIFIER.ID);
      oldNewSiId.put(memberSi.getId(), newestVersionSiId);
    }
    boolean changesAcquired = false;
    for (Map.Entry<Integer, Integer> entry : oldNewSiId.entrySet()) {
      if (!entry.getKey().equals(entry.getValue())) {
        changesAcquired = true;
        break;
      }
    }
    return changesAcquired;
  }

  /**
   *  Check if any members have new versions, update them and
   *  return true if there were changes otherwise return false.
   **/
  public static Map<Integer, Integer> updateMembers(
      CloseableDSLContext ctx, ScopedIdentifier scopedIdentifier) {
    List<ScopedIdentifier> currentMembersScopedIdentifiers =
        getMembersScopedIdentifiers(ctx, scopedIdentifier);
    Map<Integer, Integer> oldNewSiId = new HashMap<Integer, Integer>();
    for (ScopedIdentifier memberSi : currentMembersScopedIdentifiers) {
      Integer newestVersionSiId = ctx.selectFrom(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(memberSi.getNamespaceId()))
          .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(memberSi.getElementType()))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(memberSi.getIdentifier()))
          .orderBy(SCOPED_IDENTIFIER.VERSION.desc()).limit(1)
          .fetchOne().getValue(SCOPED_IDENTIFIER.ID);
      oldNewSiId.put(memberSi.getId(), newestVersionSiId);
    }
    for (Map.Entry<Integer, Integer> entry : oldNewSiId.entrySet()) {
      if (!entry.getKey().equals(entry.getValue())) {
        ctx.update(SCOPED_IDENTIFIER_HIERARCHY)
            .set(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID, entry.getValue())
            .where(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID.eq(scopedIdentifier.getId()))
            .and(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID.eq(entry.getKey()))
            .execute();
      }
    }
    return oldNewSiId;
  }
}
