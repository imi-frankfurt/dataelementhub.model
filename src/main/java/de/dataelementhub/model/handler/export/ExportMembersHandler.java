package de.dataelementhub.model.handler.export;

import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValuesHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;

/**
 * Handle export members.
 */
public class ExportMembersHandler {

  /**
   * Inside the function:
   *  1. Identify the elementType for every exportMember
   *  2. Add the related exportMembers according to the elementType
   *  3. Return the extended exportMembers list
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param userId represents the current user
   * @param elementUrns represent the super urns that have to be extended
   * @return a list of all exportMembers
   */
  public static List<Member> getAllExportMembers(
      DSLContext ctx, int userId, List<String> elementUrns) {
    List<Member> exportMembers = new ArrayList<>();
    for (String elementUrn : elementUrns) {
      ElementType elementType =
          IdentificationHandler.getScopedIdentifier(ctx, elementUrn).getElementType();
      switch (elementType) {
        case NAMESPACE: // All namespace members should be exported too.
          String[] parts = elementUrn.split(":");
          exportMembers.addAll(NamespaceHandler
              .getNamespaceMembers(ctx, userId, Integer.valueOf(parts[1]),
                  Arrays.stream(ElementType.values())
                      .filter(et -> !et.equals(ElementType.NAMESPACE)).collect(
                      Collectors.toList()), false));
          break;
        case DATAELEMENT: // The associated valueDomain should be exported too.
          handleDataElement(ctx, userId, exportMembers, elementUrn);
          break;
        case DATAELEMENTGROUP: // All group/record members should be exported too.
        case RECORD:
          handleGroupOrRecord(ctx, exportMembers, elementUrn);
          break;
        case ENUMERATED_VALUE_DOMAIN: // The associated permitted values have to be exported.
        case DESCRIBED_VALUE_DOMAIN: // Described value domain has no associated elements that
          // have to be exported.
          handleValueDomain(ctx, exportMembers, elementUrn);
          break;
        case PERMISSIBLE_VALUE: // Permitted value has no associated elements that have to
          // be exported.
          handlePermittedValue(ctx, exportMembers, elementUrn);
          break;
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
    }
    return exportMembers.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Inside the function:
   *  1. Add the specified permittedValue as exportMember
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param exportMembers is the list that has to be extended with the specified permittedValue
   * @param urn is the permittedValue identifier
   */
  private static void handlePermittedValue(
      DSLContext ctx, List<Member> exportMembers, String urn) {
    exportMembers.add(urnToMember(ctx, urn));
  }

  /**
   * Inside the function:
   *  1. Add the specified valueDomain as exportMember
   *  2. If the valueDomain is enumerated add all permittedValues as exportMembers
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param exportMembers is the list that has to be extended with the specified valueDomain
   * @param urn is the valueDomain identifier
   */
  private static void handleValueDomain(
      DSLContext ctx, List<Member> exportMembers, String urn) {
    exportMembers.add(urnToMember(ctx, urn));
    Identification valueDomainIdentification = IdentificationHandler.fromUrn(ctx, urn);
    exportMembers
        .addAll(PermittedValuesHandler.getUrns(ctx, valueDomainIdentification).stream()
            .map(memberUrn -> urnToMember(ctx, memberUrn)).collect(Collectors.toList()));
  }

  /**
   * Inside the function:
   *  1. Add the specified dataElement as exportMember
   *  2. Add the associated valueDomain as exportMember
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param userId represents the current user
   * @param exportMembers is the list that has to be extended with the specified dataElement
   * @param urn is the dataElement identifier
   */
  private static void handleDataElement(
      DSLContext ctx, int userId, List<Member> exportMembers, String urn) {
    String valueDomainUrn = IdentificationHandler.toUrn(ctx, ValueDomainHandler
        .getValueDomainScopedIdentifierByElementUrn(ctx, userId, urn));
    exportMembers.add(urnToMember(ctx, urn));
    exportMembers.add(urnToMember(ctx, valueDomainUrn));
  }

  /**
   * Inside the function:
   *  1. Add the specified group/record as exportMember
   *  2. Add all group/record members as exportMembers
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param exportMembers is the list that has to be extended with the specified group/record
   * @param urn is the group/record identifier
   */
  private static void handleGroupOrRecord(
      DSLContext ctx, List<Member> exportMembers, String urn) {
    exportMembers.add(urnToMember(ctx, urn));
    Identification identification = IdentificationHandler.fromUrn(ctx, urn);
    List<Member> elementMembers = MemberHandler.get(ctx, identification);
    exportMembers.addAll(elementMembers);
  }

  /**
   * Inside the function:
   *  1. Convert a urn to Member by adding the elementStatus
   *  2. Return the member
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param urn is the element identifier
   * @return the converted Members
   */
  private static Member urnToMember(DSLContext ctx, String urn) {
    Member member = new Member();
    member.setElementUrn(urn);
    member.setStatus(IdentificationHandler.fromUrn(ctx, urn).getStatus());
    return member;
  }

}
