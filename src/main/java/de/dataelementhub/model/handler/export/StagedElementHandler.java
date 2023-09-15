package de.dataelementhub.model.handler.export;

import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValueHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;

/**
 * StagedElement Handler.
 */
public class StagedElementHandler {


  /**
   * Inside the function:
   *  1. Get all relevant sub members from the given urns
   *  (e.g. namespace members, the valueDomain of a given dataElement, etc.)
   *  2. Filter the members according to the fullExport parameter
   *  3. Convert All exportMembers from Element into StagedElement
   *  4. Return all exportMembers as a list of stagedElements
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param elementUrns is a list of urns that has to be exported with their sub urns
   * @param userId represents the current user
   * @param fullExport represents the export type
   * @return a list of the converted stagedElements
   */
  public static List<StagedElement> elementsToStagedElements(
      DSLContext ctx, List<String> elementUrns, int userId, Boolean fullExport) {
    List<StagedElement> stagedElements = new ArrayList<>();
    List<Member> exportMembers =
        ExportMembersHandler.getAllExportMembers(ctx, userId, elementUrns);
    if (!fullExport) {
      exportMembers = excludeOnlyFullExportMembers(exportMembers);
      extendTheNonExportableElementsArray(exportMembers);
    }
    float index = 1;
    for (Member member : exportMembers) {
      Element element = ElementHandler.readSubElement(ctx, userId, member.getElementUrn());
      StagedElement stagedElement = new StagedElement();
      handleElementBasics(element, stagedElement);
      switch (element.getIdentification().getElementType()) {
        case DATAELEMENT:
          handleDataElement((DataElement) element, stagedElement);
          break;
        case DATAELEMENTGROUP:
        case RECORD:
          handleGroupAndRecord(
              ctx, (DataElementGroup) element, stagedElement, userId);
          break;
        case ENUMERATED_VALUE_DOMAIN:
        case DESCRIBED_VALUE_DOMAIN:
          handleValueDomain((ValueDomain) element, stagedElement);
          break;
        case PERMISSIBLE_VALUE:
          handlePermittedValue((PermittedValue) element, stagedElement);
          break;
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
      stagedElements.add(stagedElement);
      ExportHandler.exportProgress = index / exportMembers.size();
      index++;
    }
    return stagedElements;
  }

  /**
   * Inside the function:
   *  1. Expand the predefined stagedElement with the permittedValue specific parts
   *
   * @param permittedValue is the provided element
   * @param stagedElement is the predefined element that has to be extended
   */
  private static void handlePermittedValue(
      PermittedValue permittedValue,
      StagedElement stagedElement) {
    stagedElement.setValue(permittedValue.getValue());
    stagedElement.setUrn(permittedValue.getUrn());
    stagedElement.setConceptAssociations(permittedValue.getConceptAssociations());
  }

  /**
   * Inside the function:
   *  1. Expand the predefined stagedElement with the valueDomain specific parts
   *
   * @param valueDomain is the provided element
   * @param stagedElement is the predefined element that has to be extended
   */
  private static void handleValueDomain(
      ValueDomain valueDomain,
      StagedElement stagedElement) {
    stagedElement.setType(valueDomain.getType());
    stagedElement.setText(valueDomain.getText());
    stagedElement.setNumeric(valueDomain.getNumeric());
    stagedElement.setDatetime(valueDomain.getDatetime());
    List<PermittedValue> permittedValuesOnlyUrnVersion = valueDomain.getPermittedValues();
    if (permittedValuesOnlyUrnVersion != null) {
      stagedElement.setPermittedValues(permittedValuesOnlyUrnVersion.stream()
          .map(PermittedValueHandler::convertToOnlyUrnVersion).collect(Collectors.toList()));
    }
    stagedElement.setConceptAssociations(valueDomain.getConceptAssociations());
  }

  /**
   * Inside the function:
   *   1. Expand the predefined stagedElement with the group/record specific parts
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param dataElementGroup is the provided element
   * @param stagedElement is the predefined element that has to be extended
   * @param userId represents the current user
   */
  private static void handleGroupAndRecord(
      DSLContext ctx,
      DataElementGroup dataElementGroup,
      StagedElement stagedElement,
      int userId) {
    List<Member> members = ElementHandler.readMembers(ctx, userId, dataElementGroup
        .getIdentification().getUrn());
    stagedElement.setMembers(members);
  }

  /**
   * Inside the function:
   *  1. Expand the predefined stagedElement with the dataElement specific parts
   *
   * @param dataElement is the provided element
   * @param stagedElement is the predefined element that has to be extended
   */
  private static void handleDataElement(DataElement dataElement, StagedElement stagedElement) {
    stagedElement.setConceptAssociations(dataElement.getConceptAssociations());
    stagedElement.setValueDomainUrn(dataElement.getValueDomainUrn());
  }

  /**
   * Inside the function:
   *   1. Expand the predefined stagedElement with the Element specific parts
   *
   * @param element is the provided element
   * @param stagedElement is the predefined element that has to be extended
   */
  private static void handleElementBasics(Element element, StagedElement stagedElement) {
    stagedElement.setIdentification(element.getIdentification());
    stagedElement.setDefinitions(element.getDefinitions());
    stagedElement.setSlots(element.getSlots());
  }

  /**
   * Inside the function:
   *  1. Filter export members to include only RELEASED exportMembers
   *  2. Return the filtered list
   *
   * @param exportMembers is the list that has to be filtered
   * @return the filtered list
   */

  private static List<Member> excludeOnlyFullExportMembers(List<Member> exportMembers) {
    return exportMembers.stream()
        .filter(member -> member.getStatus().equals(Status.RELEASED))
        .collect(Collectors.toList());
  }

  /**
   * Inside the function:
   *   1. Add all NON RELEASED exportMembers to the blacklist(members that should not be exported)
   *
   * @param exportMembers is a list of all export members
   */
  private static void extendTheNonExportableElementsArray(List<Member> exportMembers) {
    de.dataelementhub.model.handler.export.ExportHandler.nonExportable
        .addAll(exportMembers.stream()
            .filter(member -> !member.getStatus().equals(Status.RELEASED))
            .map(Member::getElementUrn).collect(Collectors.toList()));
  }
}
