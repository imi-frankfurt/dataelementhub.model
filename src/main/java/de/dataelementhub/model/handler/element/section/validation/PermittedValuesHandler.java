package de.dataelementhub.model.handler.element.section.validation;

import static de.dataelementhub.dal.jooq.Routines.getScopedIdentifierByUrn;
import static de.dataelementhub.dal.jooq.tables.ValueDomainPermissibleValue.VALUE_DOMAIN_PERMISSIBLE_VALUE;

import de.dataelementhub.dal.jooq.Tables;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.tables.pojos.Element;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.ValueDomainPermissibleValueRecord;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.PermittedValue;
import de.dataelementhub.model.handler.element.ElementHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.MemberHandler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;

/**
 * Permitted Values Handler.
 */
public class PermittedValuesHandler {

  /**
   * Convert a ValueDomain object of DataElementHub DAL to a List of Permitted Values object of
   * DataElementHub Model.
   */
  public static List<PermittedValue> convert(Element valueDomain) {
    List<PermittedValue> permittedValues = new ArrayList<>();
    return permittedValues;
  }

  /**
   * Convert a Value Domain of DataElementHub Model to an Element of DataElementHub DAL.
   */
  public static Element convert(ValueDomain valueDomain) {
    Element domain = new Element();
    domain.setDatatype(valueDomain.getType());
    domain.setFormat(valueDomain.getType());
    try {
      domain.setMaximumCharacters(valueDomain.getPermittedValues().stream()
          .map(pv -> pv.getValue().length())
          .max(Comparator.naturalOrder()).orElse(0));
    } catch (NullPointerException e) {
      // If the value domain contained URNs, a better way to calculate the max char value is needed.
      // TODO: Find that better way. For now...set it to 0
      domain.setMaximumCharacters(0);
    }
    domain.setElementType(ElementType.ENUMERATED_VALUE_DOMAIN);
    return domain;

  }

  /**
   * Create new permitted values in the db.
   */
  public static void create(DSLContext ctx, int userId,
      List<PermittedValue> permittedValues, ScopedIdentifier parentScopedIdentifier)
      throws IllegalAccessException {

    List<ScopedIdentifier> permittedValueIdentifierList = new ArrayList<>();
    Identification parentIdentification =
        IdentificationHandler.convert(ctx, parentScopedIdentifier);
    Identification fallbackIdentification = new Identification();
    fallbackIdentification.setStatus(parentIdentification.getStatus());
    fallbackIdentification.setNamespaceId(parentIdentification.getNamespaceId());
    fallbackIdentification.setNamespaceUrn(parentIdentification.getNamespaceUrn());
    fallbackIdentification.setElementType(ElementType.PERMISSIBLE_VALUE);

    for (PermittedValue permittedValue : permittedValues) {
      ScopedIdentifier scopedIdentifier;
      if (permittedValue.getUrn() != null && !permittedValue.getUrn().isEmpty()) {
        scopedIdentifier = IdentificationHandler.getScopedIdentifier(ctx, permittedValue.getUrn());
        // If the scoped identifier is in another namespace than the value domain, import the
        // permitted value to this namespace
        if (!scopedIdentifier.getNamespaceId().equals(parentIdentification.getNamespaceId())) {
          scopedIdentifier = IdentificationHandler.getScopedIdentifierFromAnotherNamespace(ctx,
              userId, parentScopedIdentifier.getNamespaceId(), scopedIdentifier);
          if (scopedIdentifier == null) {
            scopedIdentifier = ElementHandler
                .importIntoParentNamespace(ctx, userId, parentScopedIdentifier.getNamespaceId(),
                    permittedValue.getUrn());
          }
        }
      } else {
        // If the permitted value itself has no identification supplied, use the one from its parent
        if (permittedValue.getIdentification() == null) {
          permittedValue.setIdentification(fallbackIdentification);
        }
        scopedIdentifier = PermittedValueHandler.create(ctx, userId, permittedValue);
      }
      permittedValueIdentifierList.add(scopedIdentifier);
    }
    createRelations(ctx, parentScopedIdentifier.getId(), permittedValueIdentifierList);
  }


  /**
   * Inside the function:
   *  1. Get valueDomainScopedIdentifier from its identification
   *  2. Identify the associated permittedValues
   *  3. Return their urns
   *
   * @param ctx configures jOOQ's behaviour when executing queries
   * @param valueDomainIdentification to identify the valueDomain
   * @return a list of all associated permittedValues (only urns)
   */
  public static List<String> getUrns(DSLContext ctx, Identification valueDomainIdentification) {

    ScopedIdentifier valueDomainIdentifier = ctx
        .selectQuery(getScopedIdentifierByUrn(valueDomainIdentification.getUrn()))
        .fetchOneInto(ScopedIdentifier.class);

    List<Integer> permittedValueIds = ctx
        .select(VALUE_DOMAIN_PERMISSIBLE_VALUE.PERMISSIBLE_VALUE_SCOPED_IDENTIFIER_ID)
        .from(VALUE_DOMAIN_PERMISSIBLE_VALUE)
        .where(VALUE_DOMAIN_PERMISSIBLE_VALUE.VALUE_DOMAIN_SCOPED_IDENTIFIER_ID
            .eq(valueDomainIdentifier.getId()))
        .fetchInto(Integer.class);

    List<de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        MemberHandler.getScopedIdentifiers(ctx, permittedValueIds);

    return scopedIdentifiers.stream().map(si -> IdentificationHandler.toUrn(ctx, si)).collect(
        Collectors.toList());
  }

  /**
   * Get a list of permitted values.
   */
  public static List<PermittedValue> get(DSLContext ctx, int userId,
      Identification valueDomainIdentification) {

    List<PermittedValue> permittedValues = new ArrayList<>();
    ScopedIdentifier valueDomainIdentifier = ctx
        .selectQuery(getScopedIdentifierByUrn(valueDomainIdentification.getUrn()))
        .fetchOneInto(ScopedIdentifier.class);

    List<Integer> permittedValueIds = ctx
        .select(VALUE_DOMAIN_PERMISSIBLE_VALUE.PERMISSIBLE_VALUE_SCOPED_IDENTIFIER_ID)
        .from(VALUE_DOMAIN_PERMISSIBLE_VALUE)
        .where(VALUE_DOMAIN_PERMISSIBLE_VALUE.VALUE_DOMAIN_SCOPED_IDENTIFIER_ID
            .eq(valueDomainIdentifier.getId()))
        .fetchInto(Integer.class);

    List<de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        MemberHandler.getScopedIdentifiers(ctx, permittedValueIds);

    scopedIdentifiers.forEach(si -> permittedValues.add(PermittedValueHandler
        .get(ctx, userId, IdentificationHandler.convert(ctx, si))));

    return permittedValues;
  }

  /**
   * Create relations between scoped identifiers.
   */
  public static void createRelations(DSLContext ctx, int valueDomainScopedIdentifierId,
      List<ScopedIdentifier> permittedValuesScopedIdentifiers) {

    List<ValueDomainPermissibleValueRecord> recordList = permittedValuesScopedIdentifiers.stream()
        .map(p -> {
          ValueDomainPermissibleValueRecord record = ctx
              .newRecord(Tables.VALUE_DOMAIN_PERMISSIBLE_VALUE);
          record.setPermissibleValueScopedIdentifierId(p.getId());
          record.setValueDomainScopedIdentifierId(valueDomainScopedIdentifierId);
          return record;
        }).collect(Collectors.toList());

    ctx.batchInsert(recordList).execute();
  }
}
