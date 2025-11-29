package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.validation.DefinedPermittedValue;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import org.jooq.DSLContext;

import java.util.UUID;

/**
 * Permitted Value Handler.
 */
public class DefinedPermittedValueHandler {

  /**
   * Get the permitted value for an identifier.
   */
  public static DefinedPermittedValue get(
      DSLContext ctx, int userId, Identification identification) {
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);
    element.getIdentification().setNamespaceUrn(
        NamespaceHandler.getNamespaceUrnById(ctx, element.getIdentification().getNamespaceId()));

    DefinedPermittedValue definedpermittedValue = new DefinedPermittedValue();
    definedpermittedValue.setValue(identifiedElementRecord.getDefinedPermittedValue());
    definedpermittedValue.setIdentification(element.getIdentification());
    definedpermittedValue.setDefinitions(element.getDefinitions());
    definedpermittedValue.setSlots(element.getSlots());
//    definedpermittedValue
//        .setConceptAssociations(ConceptAssociationHandler.get(ctx, element.getIdentification()));

    return definedpermittedValue;
  }

  /**
   * Create a Permitted Value.
   */
  public static ScopedIdentifier create(DSLContext ctx, int userId,
                                        DefinedPermittedValue definedPermittedValue)
      throws IllegalAccessException {

    // Check if the user has the right to write to the namespace
    AccessLevelType accessLevel = AccessLevelHandler
        .getAccessLevelByUserAndNamespaceUrn(ctx, userId,
                definedPermittedValue.getIdentification().getNamespaceUrn());
    if (!DaoUtil.WRITE_ACCESS_TYPES.contains(accessLevel)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }

    de.dataelementhub.dal.jooq.tables.pojos.Element element = convert(definedPermittedValue);
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(ElementHandler.saveElement(ctx, element));

    ScopedIdentifier scopedIdentifier =
        IdentificationHandler
            .create(ctx, userId, definedPermittedValue.getIdentification(), element.getId());
    // TODO: do value domains NEED definitions?
    if (definedPermittedValue.getDefinitions() != null && !definedPermittedValue.getDefinitions().isEmpty()) {
      DefinitionHandler.create(ctx, definedPermittedValue.getDefinitions(), element.getId(),
          scopedIdentifier.getId());
    }
    if (definedPermittedValue.getSlots() != null) {
      SlotHandler.create(ctx, definedPermittedValue.getSlots(), scopedIdentifier.getId());
    }
//    if (definedPermittedValue.getConceptAssociations() != null) {
//      ConceptAssociationHandler
//          .save(ctx, definedPermittedValue.getConceptAssociations(), userId, scopedIdentifier.getId());
//    }

    return scopedIdentifier;
  }

            /**
             * Convert a permitted value from DataElementHub model to a permissible value element from
             * DataElementHub DAL.
             */
    public static de.dataelementhub.dal.jooq.tables.pojos.Element convert(
            DefinedPermittedValue definedPermittedValue) {
      de.dataelementhub.dal.jooq.tables.pojos.Element domain
              = new de.dataelementhub.dal.jooq.tables.pojos.Element();

      domain.setDefinedPermittedValue(definedPermittedValue.getValue());
      domain.setElementType(ElementType.DEFINED_PERMISSIBLE_VALUE);
    return domain;
  }


  /**
   * Convert a permitted value from DataElementHub DAL to a permissible value element from
   * DataElementHub model.
   */
  public static DefinedPermittedValue convert(
      de.dataelementhub.dal.jooq.tables.pojos.Element definedPermittedValueElement) {
    DefinedPermittedValue definedPermittedValue = new DefinedPermittedValue();

    // TODO
    return definedPermittedValue;
  }

  /**
   * Update a permitted value in the db.
   */
  public static Identification update(DSLContext ctx, int userId,
                                      DefinedPermittedValue definedPermittedValue, DefinedPermittedValue previousDefinedPermittedValue)
      throws IllegalAccessException {

    // If the validation differs, an update is not allowed.
    if (!previousDefinedPermittedValue.getValue().equals(definedPermittedValue.getValue())) {
      throw new UnsupportedOperationException("The value itself can not be changed during update.");
    }

    //update scopedIdentifier if status != DRAFT
    if (previousDefinedPermittedValue.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, definedPermittedValue.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, definedPermittedValue.getIdentification())
                  .getId());
      definedPermittedValue.setIdentification(IdentificationHandler.convert(ctx, scopedIdentifier));
    }

    ElementHandler.delete(ctx, userId, previousDefinedPermittedValue.getIdentification().getUrn());
    create(ctx, userId, definedPermittedValue);

    return definedPermittedValue.getIdentification();
  }
}
