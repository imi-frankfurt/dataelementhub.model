package de.dataelementhub.model.handler.importexport;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.DataElementGroup;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Record;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.dto.importexport.StagedElement;
import de.dataelementhub.model.handler.element.DataElementGroupHandler;
import de.dataelementhub.model.handler.element.DataElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.RecordHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValueHandler;
import de.dataelementhub.model.service.ElementService;
import org.jooq.CloseableDSLContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StagedElementHandler {


  /** Converts StagedElement to dehub elements and saves them. */
  public static String stagedElementToElement(
          CloseableDSLContext ctx, StagedElement stagedElement, String namespaceUrn, int userId)
      throws Exception {
    Identification identification = new Identification();
    identification.setNamespaceUrn(namespaceUrn);
    identification.setStatus(Status.STAGED);
    identification.setElementType(stagedElement.getIdentification().getElementType());
    switch (stagedElement.getIdentification().getElementType()) {
      case DATAELEMENT:
        DataElement dataElement = new DataElement();
        dataElement.setDefinitions(stagedElement.getDefinitions());
        dataElement.setSlots(stagedElement.getSlots());
        dataElement.setConceptAssociations(stagedElement.getConceptAssociations());
        dataElement.setIdentification(identification);
        String valueDomainUrn = ImportHandler.urnDict.get(stagedElement.getValueDomainUrn());
        dataElement.setValueDomainUrn(valueDomainUrn);
        ScopedIdentifier scopedIdentifier = DataElementHandler.create(ctx, userId, dataElement);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case DATAELEMENTGROUP:
        DataElementGroup dataElementGroup = new DataElementGroup();
        dataElementGroup.setDefinitions(stagedElement.getDefinitions());
        dataElementGroup.setSlots(stagedElement.getSlots());
        dataElementGroup.setIdentification(identification);
        stagedElement.getMembers().forEach(
                (member -> {
                  String newUrn = ImportHandler.urnDict.get(member.getElementUrn());
                  member.setElementUrn(newUrn);
                })
        );
        dataElementGroup.setMembers(stagedElement.getMembers());
        scopedIdentifier = DataElementGroupHandler.create(ctx, userId, dataElementGroup);
        return IdentificationHandler.toUrn(scopedIdentifier);
      case RECORD:
        Record record = new Record();
        record.setDefinitions(stagedElement.getDefinitions());
        record.setSlots(stagedElement.getSlots());
        record.setIdentification(identification);
        stagedElement.getMembers().forEach(
                (member -> {
                  String newUrn = ImportHandler.urnDict.get(member.getElementUrn());
                  member.setElementUrn(newUrn);
                })
        );
        record.setMembers(stagedElement.getMembers());
        scopedIdentifier = RecordHandler.create(ctx, userId, record);
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
        return IdentificationHandler.toUrn(scopedIdentifier);
      case PERMISSIBLE_VALUE:
        PermittedValue permittedValue = new PermittedValue();
        permittedValue.setIdentification(identification);
        permittedValue.setDefinitions(stagedElement.getDefinitions());
        permittedValue.setSlots(stagedElement.getSlots());
        permittedValue.setUrn(stagedElement.getUrn());
        permittedValue.setConceptAssociations(stagedElement.getConceptAssociations());
        scopedIdentifier = PermittedValueHandler.create(ctx, userId, permittedValue);
        return IdentificationHandler.toUrn(scopedIdentifier);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }


  /** Converts dehub elements to StagedElements. **/
  public static List<StagedElement> elementsToStagedElements(
      List<String> elementUrns, int userId, Boolean fullExport) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<StagedElement> stagedElements = new ArrayList<>();
      List<Member> exportMembers = new ArrayList<>();
      for (String elementUrn : elementUrns) {
        if (elementUrn.toLowerCase().contains("namespace")) {
          String[] parts = elementUrn.split(":");
          List<Member> namespaceMembers =
                  NamespaceHandler.getNamespaceMembers(ctx, userId, Integer.valueOf(parts[1]), null);
          exportMembers.addAll(namespaceMembers);
        } else  {
          Member member = new Member();
          member.setElementUrn(elementUrn);
          member.setStatus(IdentificationHandler.fromUrn(elementUrn).getStatus());
          exportMembers.add(member);
        }
      }
      if (!fullExport) {
        exportMembers = exportMembers.stream()
                .filter(member -> member.getStatus().equals(Status.RELEASED)).collect(Collectors.toList());
        ExportHandler.nonExportable.addAll(exportMembers.stream()
                .filter(member -> !member.getStatus().equals(Status.RELEASED))
                .map(Member::getElementUrn).collect(Collectors.toList()));
      }
      for (Member member : exportMembers) {
        Element element = ElementService.read(userId, member.getElementUrn());
        StagedElement stagedElement = new StagedElement();
        stagedElement.setIdentification(element.getIdentification());
        stagedElement.setDefinitions(element.getDefinitions());
        stagedElement.setSlots(element.getSlots());
          switch (element.getIdentification().getElementType()) {
            case DATAELEMENT:
              stagedElement.setConceptAssociations(((DataElement) element).getConceptAssociations());
              stagedElement.setValueDomainUrn(((DataElement) element).getValueDomainUrn());
              stagedElements.addAll(elementsToStagedElements(
                      Collections.singletonList(stagedElement.getValueDomainUrn()), userId, fullExport));
              break;
            case DATAELEMENTGROUP:
            case RECORD:
              List<Member> members = ElementService.readMembers(userId, element.getIdentification().getUrn());
              List<String> membersUrns = members.stream().map(Member::getElementUrn).collect(Collectors.toList());
              stagedElements.addAll(elementsToStagedElements(membersUrns, userId, fullExport));
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
              stagedElement.setConceptAssociations(((PermittedValue) element).getConceptAssociations());
            default:
              throw new IllegalArgumentException("Element Type is not supported");
          }
        stagedElements.add(stagedElement);
      }
      return stagedElements.stream().distinct().collect(Collectors.toList());
    }
  }
}
