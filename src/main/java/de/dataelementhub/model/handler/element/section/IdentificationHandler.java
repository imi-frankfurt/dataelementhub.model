package de.dataelementhub.model.handler.element.section;

import static de.dataelementhub.dal.jooq.Routines.getScopedIdentifierByUrn;
import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.Routines;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.RelationType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.routines.GetNamespaceScopedIdentifierByUrn;
import de.dataelementhub.dal.jooq.tables.Element;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.dal.jooq.tables.records.ScopedIdentifierRecord;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.handler.ElementRelationHandler;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.jooq.CloseableDSLContext;
import org.jooq.impl.DSL;

public class IdentificationHandler {

  /**
   * Convert a ScopedIdentifier object of DataElementHub DAL to a Identification object of
   * DataElementHub Model.
   */
  public static Identification convert(ScopedIdentifier scopedIdentifier) {
    if (scopedIdentifier == null || scopedIdentifier.getIdentifier() == null) {
      return null;
    }
    Identification identification = new Identification();
    identification.setElementType(scopedIdentifier.getElementType());
    identification.setNamespaceId(scopedIdentifier.getNamespaceId());
    identification.setStatus(scopedIdentifier.getStatus());
    identification.setIdentifier(scopedIdentifier.getIdentifier());
    identification.setRevision(scopedIdentifier.getVersion());
    identification.setUrn(toUrn(scopedIdentifier));
    if (identification.getElementType() == ElementType.NAMESPACE) {
      identification.setNamespaceUrn(identification.getUrn());
    } else {
      identification.setNamespaceUrn(
          NamespaceHandler.getNamespaceUrnById(scopedIdentifier.getNamespaceId()));
    }
    return identification;
  }

  /**
   * Convert an Identification object of DataElementHub Model to a ScopedIdentifier object of
   * DataElementHub DAL.
   */
  public static ScopedIdentifier convert(int userId, Identification identification, int elementId,
      int namespaceId) {
    ScopedIdentifier scopedIdentifier = new ScopedIdentifier();
    scopedIdentifier.setElementType(identification.getElementType());
    scopedIdentifier.setNamespaceId(namespaceId);
    scopedIdentifier.setStatus(identification.getStatus());
    scopedIdentifier.setIdentifier(identification.getIdentifier());
    scopedIdentifier.setElementId(elementId);
    scopedIdentifier.setCreatedBy(userId);
    scopedIdentifier.setUrl("");
    scopedIdentifier.setVersion(identification.getRevision());
    return scopedIdentifier;
  }


  /**
   * Convert an Identification object of DataElementHub Model to a ScopedIdentifier object of
   * DataElementHub DAL.
   */
  public static ScopedIdentifier convert(int userId, Identification identification, int elementId) {
    ScopedIdentifier scopedIdentifier = new ScopedIdentifier();
    scopedIdentifier.setElementType(identification.getElementType());
    scopedIdentifier.setNamespaceId(
        getNamespaceIdentifierFromUrn(identification.getNamespaceUrn()));
    scopedIdentifier.setStatus(identification.getStatus());
    scopedIdentifier.setIdentifier(identification.getIdentifier());
    scopedIdentifier.setElementId(elementId);
    scopedIdentifier.setCreatedBy(userId);
    scopedIdentifier.setUrl("");
    scopedIdentifier.setVersion(identification.getRevision());
    return scopedIdentifier;
  }

  /**
   * Returns the specified scoped identifier.
   */
  public static ScopedIdentifier getScopedIdentifier(CloseableDSLContext ctx, String urn) {
    return ctx.selectQuery(getScopedIdentifierByUrn(urn)).fetchOneInto(ScopedIdentifier.class);
  }

  /**
   * Returns the specified scoped identifier.
   */
  public static ScopedIdentifier getScopedIdentifier(CloseableDSLContext ctx,
      Identification identification) {
    return getScopedIdentifier(ctx, identification.getUrn());
  }

  /**
   * Create a new ScopedIdentifier of DataElementHub DAL with a given Identification of
   * DataElementHub Model.
   */
  public static ScopedIdentifier create(CloseableDSLContext ctx, int userId,
      Identification identification, int elementId) {

    ScopedIdentifier scopedIdentifier;
    if (identification.getElementType().equals(ElementType.NAMESPACE)) {
      //elementId is saved in scoped_Identifier.namespace_id cause column has Not-Null-Constraint
      // and identifier is created later
      scopedIdentifier =
          IdentificationHandler.convert(userId, identification, elementId, elementId);
    } else {
      IdentifiedElementRecord namespaceRecord = NamespaceHandler.getLatestNamespaceRecord(ctx,
          userId, getNamespaceIdentifierFromUrn(identification.getNamespaceUrn()));
      scopedIdentifier =
          IdentificationHandler.convert(userId, identification, elementId, namespaceRecord.getId());
    }

    // Set proper values for creating new elements
    scopedIdentifier.setStatus(identification.getStatus());
    if (identification.getRevision() == null) {
      scopedIdentifier.setVersion(
          0);  //version = 0, when creating new elements so that the trigger in the db is called
    }
    if (scopedIdentifier.getUuid() == null) {
      scopedIdentifier.setUuid(UUID.randomUUID());
    }

    ScopedIdentifierRecord scopedIdentifierRecord = ctx
        .newRecord(SCOPED_IDENTIFIER, scopedIdentifier);
    scopedIdentifierRecord.store();
    scopedIdentifierRecord.refresh();
    return scopedIdentifierRecord.into(ScopedIdentifier.class);
  }


  /**
   * Returns a free identifier for the given namespace and element type.
   */
  public static String getFreeIdentifier(CloseableDSLContext ctx, Integer namespaceId,
      ElementType type) {
    Element ns = ELEMENT.as("ns");
    Integer max = ctx.select(DSL.max(
            DSL.when(SCOPED_IDENTIFIER.IDENTIFIER.cast(String.class).likeRegex("^\\d+$"),
                    SCOPED_IDENTIFIER.IDENTIFIER.cast(Integer.class))
                .else_(0)))
        .from(SCOPED_IDENTIFIER)
        .leftJoin(ns)
        .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
        .where(ns.ID.eq(namespaceId))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(type))
        .fetchOne().value1();
    if (max != null) {
      return String.valueOf(max + 1);
    } else {
      return "1";
    }
  }

  /**
   * Updates a ScopedIdentifier of DataElementHub DAL with a given Identification of DataElementHub
   * Model.
   */
  public static ScopedIdentifier update(CloseableDSLContext ctx, int userId,
      Identification identification, int elementId) {
    ScopedIdentifier scopedIdentifier = getScopedIdentifier(ctx, identification);
    scopedIdentifier.setElementId(elementId);
    scopedIdentifier.setCreatedBy(userId);
    scopedIdentifier.setUrl("none");
    scopedIdentifier.setVersion(identification.getRevision() + 1);
    return scopedIdentifier;
  }

  /**
   * Updates a ScopedIdentifier of DataElementHub DAL with a given Identification of DataElementHub
   * Model.
   */
  public static void updateStatus(CloseableDSLContext ctx, int userId,
      Identification identification, Status status) {
    Element ns = ELEMENT.as("ns");
    ctx.update(SCOPED_IDENTIFIER)
        .set(SCOPED_IDENTIFIER.STATUS, status)
        .where(SCOPED_IDENTIFIER.ID.in(
            ctx.select(SCOPED_IDENTIFIER.ID)
                .from(SCOPED_IDENTIFIER)
                .leftJoin(ns)
                .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
                .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
                .and(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(identification.getNamespaceId()))
                .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))))
        .execute();
  }

  /**
   * Updates a ScopedIdentifier of DataElementHub DAL with a given Identification of DataElementHub
   * Model.
   */
  public static ScopedIdentifier updateNamespaceIdentifier(CloseableDSLContext ctx,
      Identification identification) {
    ScopedIdentifier scopedIdentifier = ctx.select(SCOPED_IDENTIFIER.fields())
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
        .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(identification.getIdentifier()))
        .and(SCOPED_IDENTIFIER.VERSION.equal(identification.getRevision()))
        .fetchOneInto(ScopedIdentifier.class);
    scopedIdentifier.setVersion(identification.getRevision() + 1);
    return scopedIdentifier;
  }

  /**
   * Convert a urn to a Identification object.
   */
  public static Identification fromUrn(String urn) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ScopedIdentifier scopedIdentifier;
      if (urn.toLowerCase().contains("namespace")) {
        GetNamespaceScopedIdentifierByUrn procedure = new GetNamespaceScopedIdentifierByUrn();
        procedure.setUrn(urn);
        procedure.execute(ctx.configuration());
        ScopedIdentifierRecord scopedIdentifierRecord = procedure.getReturnValue();
        scopedIdentifier = scopedIdentifierRecord.into(ScopedIdentifier.class);
      } else {
        scopedIdentifier = getScopedIdentifier(ctx, urn);
      }
      return convert(scopedIdentifier);
    }
  }

  /**
   * Check if a String could be an urn. This only checks if the String has the correct "layout"
   */
  public static boolean isUrn(String urn) {
    if (urn == null) {
      return false;
    }
    String[] parts = urn.split(":");
    return parts.length == 5 && parts[0].equals("urn");
  }

  /**
   * Accept scopedIdentifier and return Urn.
   */
  public static String toUrn(ScopedIdentifier scopedIdentifier) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ScopedIdentifierRecord scopedIdentifierRecord = ctx
          .newRecord(SCOPED_IDENTIFIER, scopedIdentifier);
      return ctx.select(Routines.urn(scopedIdentifierRecord)).fetchOneInto(String.class);
    }
  }

  /**
   * Extract the namespace identifier (not id) from the urn.
   */
  public static Integer getNamespaceIdentifierFromUrn(String urn) {
    if (!isUrn(urn)) {
      return null;
    }
    try {
      return Integer.parseInt(urn.split(":")[1]);
    } catch (NumberFormatException e) {
      return null;
    }
  }


  /**
   * Extract the identifier from the urn.
   */
  public static Integer getIdentifierFromUrn(String urn) {
    if (!isUrn(urn)) {
      return null;
    }
    try {
      return Integer.parseInt(urn.split(":")[3]);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Extract the revision from the urn.
   */
  public static Integer getRevisionFromUrn(String urn) {
    if (!isUrn(urn)) {
      return null;
    }
    try {
      return Integer.parseInt(urn.split(":")[4]);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Outdates the scoped identifier with the given URN.
   */
  public static void outdateIdentifier(CloseableDSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    Element ns = ELEMENT.as("ns");
    ctx.update(SCOPED_IDENTIFIER)
        .set(SCOPED_IDENTIFIER.STATUS, Status.OUTDATED)
        .where(SCOPED_IDENTIFIER.ID.in(
            ctx.select(SCOPED_IDENTIFIER.ID)
                .from(SCOPED_IDENTIFIER)
                .leftJoin(ns)
                .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
                .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
                .and(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(identification.getNamespaceId()))
                .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))))
        .execute();
  }

  /**
   * Deletes the scoped identifier with the given URN.
   */
  public static void deleteDraftIdentifier(CloseableDSLContext ctx, int userId, String urn)
      throws IllegalArgumentException {
    ScopedIdentifier scopedIdentifier = getScopedIdentifier(ctx, urn);

    if (scopedIdentifier.getStatus() == Status.DRAFT
        || scopedIdentifier.getStatus() == Status.STAGED) {
      ctx.deleteFrom(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ID.eq(scopedIdentifier.getId()))
          .execute();
    } else {
      throw new IllegalArgumentException(
          "Identifier is not a draft. Call outdateIdentifier instead.");
    }
  }

  /**
   * Import an element to another Namespace. Create a new ScopedIdentifier, linking to the element.
   */
  public static ScopedIdentifier importToNamespace(CloseableDSLContext ctx, int userId,
      ScopedIdentifier sourceIdentifier,
      int targetNamespaceId) {
    ScopedIdentifier targetIdentifier = new ScopedIdentifier();
    String freeIdentifier = getFreeIdentifier(ctx, targetNamespaceId,
        sourceIdentifier.getElementType());

    // Importing will always lead to a new identifier.
    targetIdentifier.setIdentifier(Integer.parseInt(freeIdentifier));
    targetIdentifier.setVersion(1);
    targetIdentifier.setElementType(sourceIdentifier.getElementType());
    targetIdentifier.setCreatedBy(userId);
    targetIdentifier.setStatus(sourceIdentifier.getStatus());
    targetIdentifier.setElementId(sourceIdentifier.getElementId());
    targetIdentifier.setNamespaceId(targetNamespaceId);
    targetIdentifier.setUuid(UUID.randomUUID());
    targetIdentifier.setUrl(sourceIdentifier.getUrl());

    ScopedIdentifierRecord targetIdentifierRecord = ctx
        .newRecord(SCOPED_IDENTIFIER, targetIdentifier);
    targetIdentifierRecord.store();
    targetIdentifier.setId(targetIdentifierRecord.getId());

    ElementRelationHandler
        .insertLocalRelation(ctx, userId, toUrn(targetIdentifier), toUrn(sourceIdentifier),
            RelationType.equal);

    return targetIdentifier;
  }


  /**
   * Import an element to another Namespace. Create a new ScopedIdentifier, linking to the element.
   */
  public static ScopedIdentifier importToNamespace(CloseableDSLContext ctx, int userId, String urn,
      int targetNamespaceId) {
    ScopedIdentifier sourceIdentifier = IdentificationHandler.getScopedIdentifier(ctx, urn);
    return importToNamespace(ctx, userId, sourceIdentifier, targetNamespaceId);
  }

  /**
   * Check if the element belonging to an identification can be released.
   */
  public static boolean canBeReleased(CloseableDSLContext ctx, int userId,
      Identification identification) {
    if (identification == null) {
      throw new NoSuchElementException();
    }

    if (!identification.getStatus().equals(Status.STAGED) && !identification.getStatus()
        .equals(Status.DRAFT)) {
      throw new IllegalStateException("Neither a draft nor staged");
    }

    // No need to try to check a namespaces namespace, because that would not make sense.
    if (identification.getElementType() != ElementType.NAMESPACE) {
      Namespace namespace = NamespaceHandler.getByUrn(ctx, userId,
          identification.getNamespaceUrn());

      if (namespace.getIdentification().getStatus().equals(Status.STAGED)
          || namespace.getIdentification().getStatus().equals(Status.DRAFT)) {
        throw new IllegalStateException("Namespace is not released. Element can not be released.");
      }
    }

    return true;
  }

  /**
   * When creating an element, users might submit identifiers/revisions which are not to be used.
   */
  public static Identification removeUserSubmittedIdentifierAndRevision(
      Identification identification) {
    identification.setIdentifier(null);
    identification.setRevision(null);
    return identification;
  }
}
