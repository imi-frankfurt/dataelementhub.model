package de.dataelementhub.model.handler.element;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.DataElement;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.element.section.ConceptAssociationHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import de.dataelementhub.model.handler.element.section.ValueDomainHandler;
import java.util.UUID;
import org.jooq.CloseableDSLContext;

public class DataElementHandler extends ElementHandler {

  /**
   * Create a new DataElement and return its new ID.
   */
  public static ScopedIdentifier create(CloseableDSLContext ctx, int userId,
      DataElement dataElement)
      throws IllegalAccessException {

    // Check if the user has the right to write to the namespace
    AccessLevelType accessLevel = AccessLevelHandler
        .getAccessLevelByUserAndNamespaceUrn(ctx, userId,
            dataElement.getIdentification().getNamespaceUrn());
    if (!DaoUtil.WRITE_ACCESS_TYPES.contains(accessLevel)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }

    // If value domain AND value domain urn are set - throw an error (for now?)
    if (dataElement.getValueDomain() != null && dataElement.getValueDomainUrn() != null
        && !dataElement.getValueDomainUrn().isEmpty()) {
      throw new IllegalArgumentException("Only value domain OR value domain urn is allowed");
    } else if (dataElement.getValueDomainUrn() != null && !dataElement.getValueDomainUrn()
        .isEmpty()) {
      // If value domain urn is used, check if it is of an allowed type (enumerated or described vd)
      ElementType elementType = IdentificationHandler.fromUrn(ctx, dataElement.getValueDomainUrn())
          .getElementType();
      if (elementType != ElementType.ENUMERATED_VALUE_DOMAIN
          && elementType != ElementType.DESCRIBED_VALUE_DOMAIN) {
        throw new IllegalArgumentException(
            "Value Domain urn must belong to an actual value domain.");
      }
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);

    ScopedIdentifier valueDomainIdentifier;
    if (dataElement.getValueDomain() != null) {
      // An identification object is needed but not supplied when created this way
      Identification valueDomainIdentification = new Identification();
      valueDomainIdentification.setNamespaceUrn(dataElement.getIdentification().getNamespaceUrn());
      if (dataElement.getValueDomain().getType()
          .equalsIgnoreCase(ValueDomain.TYPE_ENUMERATED)) {
        valueDomainIdentification.setElementType(ElementType.ENUMERATED_VALUE_DOMAIN);
      } else {
        valueDomainIdentification.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
      }
      valueDomainIdentification.setStatus(dataElement.getIdentification().getStatus());
      dataElement.getValueDomain().setIdentification(valueDomainIdentification);
      valueDomainIdentifier = ValueDomainHandler.create(ctx, userId, dataElement.getValueDomain());
    } else {
      valueDomainIdentifier = IdentificationHandler
          .getScopedIdentifier(ctx, dataElement.getValueDomainUrn());
      if (valueDomainIdentifier == null) {
        CtxUtil.rollbackAndSetAutoCommit(ctx, autoCommit);
        throw new IllegalArgumentException("ValueDomain urn not found");
      }
      // If the scoped identifier of the value domain is in another namespace than the dataelement,
      // import the value domain into the dataelements namespace

      Integer deNsIdentifier = IdentificationHandler.getNamespaceIdentifierFromUrn(
          dataElement.getIdentification().getNamespaceUrn());
      Integer vdNsIdentifier = IdentificationHandler.getNamespaceIdentifierFromUrn(
          dataElement.getValueDomainUrn());

      if (!deNsIdentifier.equals(vdNsIdentifier)) {
        Namespace targetNamespace = NamespaceHandler
            .getByIdentifier(ctx, userId, deNsIdentifier);
        valueDomainIdentifier = ElementHandler
            .importIntoParentNamespace(ctx, userId, targetNamespace.getIdentification()
                .getNamespaceId(), dataElement.getValueDomainUrn());
      }
    }

    de.dataelementhub.dal.jooq.tables.pojos.Element element
        = new de.dataelementhub.dal.jooq.tables.pojos.Element();
    element.setElementId(valueDomainIdentifier.getElementId());
    element.setElementType(ElementType.DATAELEMENT);
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }

    element.setId(saveElement(ctx, element));

    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(ctx, userId, dataElement.getIdentification(), element.getId());
    DefinitionHandler.create(ctx, dataElement.getDefinitions(), element.getId(),
        scopedIdentifier.getId());
    if (dataElement.getSlots() != null) {
      SlotHandler.create(ctx, dataElement.getSlots(), scopedIdentifier.getId());
    }
    if (dataElement.getConceptAssociations() != null) {
      ConceptAssociationHandler
          .save(ctx, dataElement.getConceptAssociations(), userId, scopedIdentifier.getId());
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * Get a dataelement by its urn.
   */
  public static DataElement get(
      CloseableDSLContext ctx, int userId, Identification identification) {
    String urn = identification.getUrn();
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);

    DataElement dataElement = new DataElement();
    dataElement.setIdentification(element.getIdentification());
    dataElement.setDefinitions(element.getDefinitions());
    ScopedIdentifier valueDomainScopedIdentifier = ValueDomainHandler
        .getValueDomainScopedIdentifierByElementUrn(ctx, userId, urn);
    dataElement.setValueDomainUrn(IdentificationHandler.toUrn(ctx, valueDomainScopedIdentifier));
    dataElement.setSlots(element.getSlots());
    dataElement
        .setConceptAssociations(ConceptAssociationHandler.get(ctx, element.getIdentification()));
    return dataElement;
  }

  /**
   * Save an element.
   */
  public static int saveElement(CloseableDSLContext ctx,
      de.dataelementhub.dal.jooq.tables.pojos.Element dataElement) {

    return ctx.insertInto(ELEMENT)
        .set(ctx.newRecord(ELEMENT, dataElement))
        .returning(ELEMENT.ID)
        .fetchOne().getId();
  }

  /**
   * Update a dataelement.
   */
  public static Identification update(CloseableDSLContext ctx, int userId, DataElement dataElement)
      throws IllegalAccessException {
    DataElement previousDataElement = get(ctx, userId, dataElement.getIdentification());

    if (dataElement.getValueDomain() != null) {
      throw new IllegalArgumentException("value domain field has to be empty.");
    }
    if (dataElement.getValueDomainUrn() != null && !dataElement.getValueDomainUrn()
        .equalsIgnoreCase(previousDataElement.getValueDomainUrn())) {
      throw new UnsupportedOperationException("Validation changes are not allowed during update.");
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    //update scopedIdentifier if status != DRAFT
    if (previousDataElement.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, dataElement.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, dataElement.getIdentification())
                  .getId());
      dataElement.setIdentification(IdentificationHandler.convert(ctx, scopedIdentifier));
    }

    delete(ctx, userId, previousDataElement.getIdentification().getUrn());
    create(ctx, userId, dataElement);

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return dataElement.getIdentification();
  }

}
