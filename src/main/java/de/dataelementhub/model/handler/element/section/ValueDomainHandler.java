package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.Routines.getValueDomainScopedIdentifierByDataelementUrn;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.enums.ValidationType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.validation.DatetimeHandler;
import de.dataelementhub.model.handler.element.section.validation.NumericHandler;
import de.dataelementhub.model.handler.element.section.validation.PermittedValuesHandler;
import de.dataelementhub.model.handler.element.section.validation.TextHandler;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * ValueDomain Handler.
 */
public class ValueDomainHandler extends ElementHandler {


  /**
   * Get the Value Domain for an identified element record.
   */
  public static ValueDomain get(
      DSLContext ctx, int userId, Identification identification) {
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);

    de.dataelementhub.model.dto.element.Element element = ElementHandler
        .convertToElement(ctx, identification, identifiedElementRecord);

    Element valueDomainElement = identifiedElementRecord.into(Element.class);

    ValueDomain valueDomain = new ValueDomain();
    valueDomain.setType(valueDomainElement.getDatatype());
    valueDomain.setIdentification(element.getIdentification());
    valueDomain.setDefinitions(element.getDefinitions());
    valueDomain.setSlots(element.getSlots());
    valueDomain
        .setConceptAssociations(ConceptAssociationHandler.get(ctx, element.getIdentification()));

    switch (valueDomain.getType()) {
      case ValueDomain.TYPE_DATE:
      case ValueDomain.TYPE_DATETIME:
      case ValueDomain.TYPE_TIME:
        valueDomain.setDatetime(DatetimeHandler.convert(valueDomainElement));
        break;
      case ValueDomain.TYPE_NUMERIC:
        valueDomain.setNumeric(NumericHandler.convert(valueDomainElement));
        break;
      case ValueDomain.TYPE_STRING:
        valueDomain.setText(TextHandler.convert(valueDomainElement));
        break;
      case ValueDomain.TYPE_ENUMERATED:
        valueDomain.setPermittedValues(PermittedValuesHandler.get(ctx, userId, identification));
        break;
      default:
        break;
    }
    Integer namespaceIdentifier = NamespaceHandler.getNamespaceIdByUrn(ctx,
        identification.getNamespaceUrn());
    valueDomain.getIdentification().setNamespaceUrn(identification.getNamespaceUrn());
    valueDomain.getIdentification().setNamespaceId(namespaceIdentifier);
    return valueDomain;
  }

  /**
   * Convert a Value Domain of DataElementHub Model to a ValueDomain of DataElementHub DAL.
   */
  public static Element convert(ValueDomain valueDomain) {
    switch (valueDomain.getType()) {
      case ValueDomain.TYPE_DATE:
      case ValueDomain.TYPE_DATETIME:
      case ValueDomain.TYPE_TIME:
        return DatetimeHandler.convert(valueDomain);
      case ValueDomain.TYPE_NUMERIC:
        return NumericHandler.convert(valueDomain);
      case ValueDomain.TYPE_STRING:
        return TextHandler.convert(valueDomain);
      case ValueDomain.TYPE_BOOLEAN: {
        Element domain = new Element();
        domain.setDatatype(valueDomain.getType());
        domain.setDescription("(true|false|yes|no|f|t)");
        domain.setFormat(domain.getDescription());
        domain.setValidationData(domain.getDescription());
        domain.setValidationType(ValidationType.BOOLEAN);
        domain.setMaximumCharacters(5);
        domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
        return domain;
      }
      case ValueDomain.TYPE_TBD: {
        Element domain = new Element();
        domain.setDatatype(valueDomain.getType());
        domain.setDescription(ValidationType.TBD.getName());
        domain.setFormat(ValidationType.TBD.getName());
        domain.setValidationType(ValidationType.TBD);
        domain.setMaximumCharacters(0);
        domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
        return domain;
      }
      case ValueDomain.TYPE_ENUMERATED: {
        return PermittedValuesHandler.convert(valueDomain);
      }
      default:
        throw new IllegalArgumentException("Unknown value domain type: " + valueDomain.getType());
    }
  }

  /**
   * Returns the scoped identifier of the value domain for the given Dataelement.
   */
  public static ScopedIdentifier getValueDomainScopedIdentifierByElementUrn(DSLContext ctx,
      int userId,
      String dataElementUrn) {
    return ctx.selectQuery(getValueDomainScopedIdentifierByDataelementUrn(dataElementUrn))
        .fetchOneInto(ScopedIdentifier.class);
  }

  /**
   * Create a new ValueDomain of DataElementHub DAL with a Value Domain of DataElementHub Model.
   */
  public static ScopedIdentifier create(DSLContext ctx, int userId,
      ValueDomain valueDomain)
      throws IllegalAccessException {

    // Check if the user has the right to write to the namespace
    AccessLevelType accessLevel = AccessLevelHandler
        .getAccessLevelByUserAndNamespaceUrn(ctx, userId,
            valueDomain.getIdentification().getNamespaceUrn());
    if (!DaoUtil.WRITE_ACCESS_TYPES.contains(accessLevel)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }

    if (valueDomain.getType().equals(ValueDomain.TYPE_ENUMERATED)
        && valueDomain.getIdentification().getStatus() == Status.RELEASED && (
        valueDomain.getPermittedValues() == null || valueDomain.getPermittedValues().isEmpty())) {
      throw new IllegalArgumentException(
          "Can't create released enumerated value domain without permitted values.");
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.dataelementhub.dal.jooq.tables.pojos.Element element = convert(valueDomain);

    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(ElementHandler.saveElement(ctx, element));

    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(ctx, userId, valueDomain.getIdentification(), element.getId());

    if (valueDomain.getDefinitions() != null && !valueDomain.getDefinitions().isEmpty()) {
      DefinitionHandler.create(ctx, valueDomain.getDefinitions(), element.getId(),
          scopedIdentifier.getId());
    }
    if (valueDomain.getSlots() != null) {
      SlotHandler.create(ctx, valueDomain.getSlots(), scopedIdentifier.getId());
    }
    if (valueDomain.getConceptAssociations() != null) {
      ConceptAssociationHandler
          .save(ctx, valueDomain.getConceptAssociations(), userId, scopedIdentifier.getId());
    }

    if (valueDomain.getPermittedValues() != null) {
      valueDomain.getPermittedValues().forEach(pv -> pv.setIdentification(null));
      PermittedValuesHandler
          .create(ctx, userId, valueDomain.getPermittedValues(), scopedIdentifier);
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * Update a value domain. Currently only working on drafts.
   */
  public static Identification update(DSLContext ctx, int userId, ValueDomain valueDomain,
      ValueDomain oldValueDomain) throws NoSuchMethodException, IllegalAccessException {

    if (oldValueDomain.getIdentification().getStatus() == Status.DRAFT) {

      if (valueDomain.getIdentification().getStatus() == Status.RELEASED && valueDomain.getType()
          .equals(ValueDomain.TYPE_ENUMERATED) && (valueDomain.getPermittedValues() == null
          || valueDomain.getPermittedValues().isEmpty())) {
        throw new IllegalArgumentException("Can't release value domain without permitted values");
      }

      delete(ctx, userId, valueDomain.getIdentification().getUrn());
      create(ctx, userId, valueDomain);
      return valueDomain.getIdentification();
    } else {
      throw new NoSuchMethodException(
          "Updating released value domains is currently not supported.");
    }
  }
}
