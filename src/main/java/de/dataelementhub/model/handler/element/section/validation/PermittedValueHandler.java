package de.dataelementhub.model.handler.element.section.validation;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.Element;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.ConceptAssociationHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import java.util.UUID;
import org.jooq.CloseableDSLContext;

/**
 * Permitted Value Handler.
 */
public class PermittedValueHandler {

  /**
   * Get the permitted value for an identifier.
   */
  public static PermittedValue get(
      CloseableDSLContext ctx, int userId, Identification identification) {
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);
    element.getIdentification().setNamespaceUrn(
        NamespaceHandler.getNamespaceUrnById(ctx, element.getIdentification().getNamespaceId()));

    PermittedValue permittedValue = new PermittedValue();
    permittedValue.setValue(identifiedElementRecord.getPermittedValue());
    permittedValue.setIdentification(element.getIdentification());
    permittedValue.setDefinitions(element.getDefinitions());
    permittedValue.setSlots(element.getSlots());
    permittedValue
        .setConceptAssociations(ConceptAssociationHandler.get(ctx, element.getIdentification()));

    return permittedValue;
  }

  /**
   * Create a Permitted Value.
   */
  public static ScopedIdentifier create(CloseableDSLContext ctx, int userId,
      PermittedValue permittedValue)
      throws IllegalAccessException {

    // Check if the user has the right to write to the namespace
    AccessLevelType accessLevel = AccessLevelHandler
        .getAccessLevelByUserAndNamespaceUrn(ctx, userId,
            permittedValue.getIdentification().getNamespaceUrn());
    if (!DaoUtil.WRITE_ACCESS_TYPES.contains(accessLevel)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.dataelementhub.dal.jooq.tables.pojos.Element element = convert(permittedValue);
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(ElementHandler.saveElement(ctx, element));

    ScopedIdentifier scopedIdentifier =
        IdentificationHandler
            .create(ctx, userId, permittedValue.getIdentification(), element.getId());
    // TODO: do value domains NEED definitions?
    if (permittedValue.getDefinitions() != null && !permittedValue.getDefinitions().isEmpty()) {
      DefinitionHandler.create(ctx, permittedValue.getDefinitions(), element.getId(),
          scopedIdentifier.getId());
    }
    if (permittedValue.getSlots() != null) {
      SlotHandler.create(ctx, permittedValue.getSlots(), scopedIdentifier.getId());
    }
    if (permittedValue.getConceptAssociations() != null) {
      ConceptAssociationHandler
          .save(ctx, permittedValue.getConceptAssociations(), userId, scopedIdentifier.getId());
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * Convert a permitted value from DataElementHub model to a permissible value element from
   * DataElementHub DAL.
   */
  public static de.dataelementhub.dal.jooq.tables.pojos.Element convert(
      PermittedValue permittedValue) {
    de.dataelementhub.dal.jooq.tables.pojos.Element domain
        = new de.dataelementhub.dal.jooq.tables.pojos.Element();

    domain.setPermittedValue(permittedValue.getValue());
    domain.setElementType(ElementType.PERMISSIBLE_VALUE);
    return domain;
  }


  /**
   * Convert a permitted value from DataElementHub DAL to a permissible value element from
   * DataElementHub model.
   */
  public static PermittedValue convert(
      de.dataelementhub.dal.jooq.tables.pojos.Element permittedValueElement) {
    PermittedValue permittedValue = new PermittedValue();

    // TODO
    return permittedValue;
  }

  /**
   * Update a permitted value in the db.
   */
  public static Identification update(CloseableDSLContext ctx, int userId,
      PermittedValue permittedValue, PermittedValue previousPermittedValue)
      throws IllegalAccessException {

    // If the validation differs, an update is not allowed.
    if (!previousPermittedValue.getValue().equals(permittedValue.getValue())) {
      throw new UnsupportedOperationException("The value itself can not be changed during update.");
    }

    //update scopedIdentifier if status != DRAFT
    if (previousPermittedValue.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, permittedValue.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, permittedValue.getIdentification())
                  .getId());
      permittedValue.setIdentification(IdentificationHandler.convert(ctx, scopedIdentifier));
    }

    ElementHandler.delete(ctx, userId, previousPermittedValue.getIdentification().getUrn());
    create(ctx, userId, permittedValue);

    return permittedValue.getIdentification();
  }
}
