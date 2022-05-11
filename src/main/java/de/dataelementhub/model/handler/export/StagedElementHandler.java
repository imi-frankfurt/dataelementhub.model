package de.dataelementhub.model.handler.export;

import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.CloseableDSLContext;

/**
 * StagedElement Handler.
 */
public class StagedElementHandler {


  /**
   * Converts dehub elements to StagedElements.
   **/
  public static List<StagedElement> elementsToStagedElements(
      CloseableDSLContext ctx, List<String> elementUrns, int userId, Boolean fullExport) {
    List<StagedElement> stagedElements = new ArrayList<>();
    List<Member> exportMembers = new ArrayList<>();
    for (String elementUrn : elementUrns) {
      if (elementUrn.toLowerCase().contains("namespace")) {
        String[] parts = elementUrn.split(":");
        List<Member> namespaceMembers =
            NamespaceHandler.getNamespaceMembers(ctx, userId, Integer.valueOf(parts[1]),
                null, true);
        exportMembers.addAll(namespaceMembers);
      } else {
        Member member = new Member();
        member.setElementUrn(elementUrn);
        member.setStatus(IdentificationHandler.fromUrn(ctx, elementUrn).getStatus());
        exportMembers.add(member);
      }
    }
    if (!fullExport) {
      exportMembers = exportMembers.stream()
          .filter(member -> member.getStatus().equals(Status.RELEASED))
          .collect(Collectors.toList());
      de.dataelementhub.model.handler.export.ExportHandler.nonExportable
          .addAll(exportMembers.stream()
              .filter(member -> !member.getStatus().equals(Status.RELEASED))
              .map(Member::getElementUrn).collect(Collectors.toList()));
    }
    float index = 1;
    for (Member member : exportMembers) {
      Element element = ElementHandler.readSubElement(ctx, userId, member.getElementUrn());
      StagedElement stagedElement = new StagedElement();
      stagedElement.setIdentification(element.getIdentification());
      stagedElement.setDefinitions(element.getDefinitions());
      stagedElement.setSlots(element.getSlots());
      switch (element.getIdentification().getElementType()) {
        case DATAELEMENT:
          stagedElement.setConceptAssociations(((DataElement) element).getConceptAssociations());
          stagedElement.setValueDomainUrn(((DataElement) element).getValueDomainUrn());
          stagedElements.addAll(elementsToStagedElements(
              ctx, Collections.singletonList(stagedElement.getValueDomainUrn()),
              userId, fullExport));
          break;
        case DATAELEMENTGROUP:
        case RECORD:
          List<Member> members = ElementHandler.readMembers(ctx, userId, element
              .getIdentification().getUrn());
          List<String> membersUrns = members.stream().map(Member::getElementUrn)
              .collect(Collectors.toList());
          stagedElements.addAll(elementsToStagedElements(ctx, membersUrns, userId, fullExport));
          stagedElement.setMembers(members);
          break;
        case ENUMERATED_VALUE_DOMAIN:
        case DESCRIBED_VALUE_DOMAIN:
          stagedElement.setType(((ValueDomain) element).getType());
          stagedElement.setText(((ValueDomain) element).getText());
          stagedElement.setNumeric(((ValueDomain) element).getNumeric());
          stagedElement.setDatetime(((ValueDomain) element).getDatetime());
          stagedElement.setPermittedValues(((ValueDomain) element).getPermittedValues());
          stagedElement.setConceptAssociations(((ValueDomain) element).getConceptAssociations());
          break;
        case PERMISSIBLE_VALUE:
          stagedElement.setValue(((PermittedValue) element).getValue());
          stagedElement.setUrn(((PermittedValue) element).getUrn());
          stagedElement.setConceptAssociations(((PermittedValue) element)
              .getConceptAssociations());
          break;
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
      stagedElements.add(stagedElement);
      ExportHandler.exportProgress = index / exportMembers.size();
      index = index + 1;
    }
    return stagedElements.stream().distinct().collect(Collectors.toList());
  }
}
