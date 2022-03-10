package de.dataelementhub.model.handler.element;

import static de.dataelementhub.dal.jooq.Tables.DEFINITION;
import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.IDENTIFIED_ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.LISTVIEW_ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER_HIERARCHY;
import static de.dataelementhub.dal.jooq.Tables.SLOT;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.Status;
import de.dataelementhub.dal.jooq.tables.pojos.DehubUser;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.pojos.UserNamespaceAccess;
import de.dataelementhub.dal.jooq.tables.records.DefinitionRecord;
import de.dataelementhub.dal.jooq.tables.records.IdentifiedElementRecord;
import de.dataelementhub.dal.jooq.tables.records.ListviewElementRecord;
import de.dataelementhub.model.CtxUtil;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.DeHubUserPermission;
import de.dataelementhub.model.dto.element.Namespace;
import de.dataelementhub.model.dto.element.section.Definition;
import de.dataelementhub.model.dto.element.section.Identification;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.listviews.NamespaceMember;
import de.dataelementhub.model.handler.AccessLevelHandler;
import de.dataelementhub.model.handler.UserHandler;
import de.dataelementhub.model.handler.element.section.DefinitionHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.element.section.SlotHandler;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.jooq.CloseableDSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jooq.lambda.tuple.Tuple2;
import org.simpleflatmapper.jdbc.JdbcMapper;
import org.simpleflatmapper.jdbc.JdbcMapperFactory;
import org.simpleflatmapper.util.TypeReference;

public class NamespaceHandler extends ElementHandler {

  /**
   * Create a new Namespace and return its new ID.
   */
  public static ScopedIdentifier create(CloseableDSLContext ctx, int userId, Namespace namespace) {
    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.dataelementhub.dal.jooq.tables.pojos.Element element
        = new de.dataelementhub.dal.jooq.tables.pojos.Element();
    element.setElementType(ElementType.NAMESPACE);
    element.setHidden(namespace.getIdentification().getHideNamespace());
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(saveElement(ctx, element));
    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(
            ctx, userId, namespace.getIdentification(), element.getId());
    DefinitionHandler
        .create(ctx, namespace.getDefinitions(), element.getId(), scopedIdentifier.getId());
    if (namespace.getSlots() != null) {
      SlotHandler.create(ctx, namespace.getSlots(), scopedIdentifier.getId());
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);

    // Creator of the namespace gets admin rights by default
    UserHandler
        .setUserAccessToNamespace(userId, scopedIdentifier.getIdentifier(), AccessLevelType.ADMIN);

    return scopedIdentifier;
  }

  /**
   * Get the namespace id (database id) for a given urn.
   */
  public static Integer getNamespaceIdByUrn(String urn) {
    Integer identifier = IdentificationHandler.getIdentifierFromUrn(urn);
    Integer revision = IdentificationHandler.getRevisionFromUrn(urn);
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return ctx.select(SCOPED_IDENTIFIER.NAMESPACE_ID).from(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(identifier))
          .and(SCOPED_IDENTIFIER.VERSION.equal(revision))
          .fetchOneInto(Integer.class);
    }
  }

  /**
   * Get the namespace urn for a given namespace id (database id).
   */
  public static String getNamespaceUrnById(int namespaceId) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ScopedIdentifier scopedIdentifier = ctx.select(SCOPED_IDENTIFIER.fields())
          .from(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(namespaceId))
          .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.VERSION.equal(
              ctx.select(DSL.max(SCOPED_IDENTIFIER.VERSION)).from(SCOPED_IDENTIFIER)
                  .where(SCOPED_IDENTIFIER.NAMESPACE_ID.equal(namespaceId))
                  .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))))
          .fetchOneInto(ScopedIdentifier.class);
      return IdentificationHandler.toUrn(scopedIdentifier);
    }
  }

  /**
   * Get the namespace urn for a given namespace id (database id).
   */
  public static String getNamespaceUrnById(CloseableDSLContext ctx, int namespaceId) {
    ScopedIdentifier scopedIdentifier = ctx.selectFrom(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(namespaceId))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
        .orderBy(SCOPED_IDENTIFIER.VERSION.desc()).limit(1)
        .fetchOneInto(ScopedIdentifier.class);
    return "urn:" + scopedIdentifier.getNamespaceId() + ":namespace:"
        + scopedIdentifier.getIdentifier() + ":" + scopedIdentifier.getVersion();
  }

  /**
   * Get a Namespace.
   */
  public static Namespace getByIdentifier(CloseableDSLContext ctx, int userId,
      Integer namespaceIdentifier) {

    try {
      Integer namespaceId = ctx.select(SCOPED_IDENTIFIER.NAMESPACE_ID).from(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))
          .and(SCOPED_IDENTIFIER.VERSION.equal(
              ctx.select(DSL.max(SCOPED_IDENTIFIER.VERSION)).from(SCOPED_IDENTIFIER)
                  .where(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))
                  .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))))
          .fetchOneInto(Integer.class);

      Namespace namespace = getByDatabaseId(ctx, userId, namespaceId);
      namespace.setSlots(SlotHandler.get(ctx, namespace.getIdentification()));
      return namespace;
    } catch (NullPointerException e) {
      throw new NoSuchElementException();
    }
  }


  /**
   * Get a Namespace by its URN.
   */
  public static Namespace getByUrn(CloseableDSLContext ctx, int userId, String urn) {
    try {
      Namespace namespace = getByDatabaseId(ctx, userId, getNamespaceIdByUrn(urn));
      namespace.setSlots(SlotHandler.get(ctx, namespace.getIdentification()));
      return namespace;
    } catch (NullPointerException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get all Namespace Members.
   */
  public static List<Member> getNamespaceMembers(CloseableDSLContext ctx, int userId,
      Integer namespaceIdentifier, List<ElementType> elementTypes, Boolean hideSubElements) {

    if (elementTypes == null || elementTypes.isEmpty()) {
      elementTypes = Arrays
          .asList(ElementType.DATAELEMENT, ElementType.DATAELEMENTGROUP, ElementType.RECORD);
    }
    try {
      Integer namespaceId = ctx.select(SCOPED_IDENTIFIER.NAMESPACE_ID).from(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))
          .and(SCOPED_IDENTIFIER.VERSION.equal(
              ctx.select(DSL.max(SCOPED_IDENTIFIER.VERSION)).from(SCOPED_IDENTIFIER)
                  .where(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))
                  .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))))
          .fetchOneInto(Integer.class);
      // to check if Namespace is accessible by current user
      getByDatabaseId(ctx, userId, namespaceId);
      List<ScopedIdentifier> scopedIdentifiers = new ArrayList<>();
      if (hideSubElements) {
        scopedIdentifiers =
            ctx.selectFrom(SCOPED_IDENTIFIER).where(
                    SCOPED_IDENTIFIER.NAMESPACE_ID.eq(namespaceId))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(elementTypes))
                .andNotExists(ctx.select(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID)
                    .from(SCOPED_IDENTIFIER_HIERARCHY)
                    .where(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID.eq(SCOPED_IDENTIFIER.ID))
                    .andNotExists(ctx.select(SCOPED_IDENTIFIER.ID).from(SCOPED_IDENTIFIER)
                        .where(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID))
                        .and(SCOPED_IDENTIFIER.STATUS.eq(Status.OUTDATED))))
                .fetchInto(ScopedIdentifier.class);
      } else {
        scopedIdentifiers =
            ctx.selectFrom(SCOPED_IDENTIFIER).where(
                    SCOPED_IDENTIFIER.NAMESPACE_ID.eq(namespaceId))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.in(elementTypes))
                .fetchInto(ScopedIdentifier.class);
      }

      List<Member> namespaceMembers = new ArrayList<>();
      scopedIdentifiers.forEach(
          (scopedIdentifier) -> {
            Member member = new Member();
            member.setElementUrn(IdentificationHandler.toUrn(scopedIdentifier));
            member.setStatus(scopedIdentifier.getStatus());
            namespaceMembers.add(member);
          });
      return namespaceMembers;
    } catch (NullPointerException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get all Namespace Records by its Identifier.
   */
  public static List<IdentifiedElementRecord> getNamespaceRecords(CloseableDSLContext ctx,
      int userId, Integer namespaceIdentifier) {
    return ctx
        .selectFrom(IDENTIFIED_ELEMENT)
        .where(IDENTIFIED_ELEMENT.ELEMENT_TYPE.equal(ElementType.NAMESPACE)
            .and(IDENTIFIED_ELEMENT.SI_IDENTIFIER.equal(namespaceIdentifier))
        )
        .fetch();
  }

  /**
   * Get the latest Namespace Record by its Identifier. If there is a released namespace, return
   * that one, otherwise the latest version (be it outdated or a draft)
   */
  public static IdentifiedElementRecord getLatestNamespaceRecord(CloseableDSLContext ctx,
      int userId, Integer namespaceIdentifier) {
    List<IdentifiedElementRecord> namespaceRecords = getNamespaceRecords(ctx, userId,
        namespaceIdentifier);

    Optional<IdentifiedElementRecord> releasedNamespaceOptional =
        namespaceRecords.stream().filter(r -> r.getSiStatus().equals(Status.RELEASED)).findFirst();

    return releasedNamespaceOptional.orElseGet(() -> namespaceRecords.stream().max(
            Comparator.comparing(IdentifiedElementRecord::getSiVersion))
        .orElseThrow(NoSuchElementException::new));
  }

  /**
   * Get a namespace by its id.
   */
  public static Namespace getByDatabaseId(CloseableDSLContext ctx, int userId,
      Integer namespaceId) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(DaoUtil.accessibleByUserId(ctx, userId))
        .and(ELEMENT.ID.eq(namespaceId));
    return fetchNamespaceQuery(query).stream().findFirst().orElse(null);
  }

  private static SelectConditionStep<Record> getNamespacesQuery(CloseableDSLContext ctx) {
    return ctx.select(ELEMENT.fields())
        .select(DEFINITION.fields())
        .select(SCOPED_IDENTIFIER.fields())
        .select(SLOT.fields())
        .from(ELEMENT)
        .leftJoin(DEFINITION)
        .on(DEFINITION.ELEMENT_ID.eq(ELEMENT.ID))
        .leftJoin(SCOPED_IDENTIFIER)
        .on(SCOPED_IDENTIFIER.ELEMENT_ID.eq(ELEMENT.ID))
        .leftJoin(SLOT)
        .on(SLOT.SCOPED_IDENTIFIER_ID.eq(SCOPED_IDENTIFIER.ID))
        .where(ELEMENT.ELEMENT_TYPE.eq(ElementType.NAMESPACE));
  }

  /**
   * Fetch and convert DescribedElements from a given query.
   */
  private static List<Namespace> fetchNamespaceQuery(SelectConditionStep<Record> query) {
    Result<?> result = query.fetch();
    Map<Integer, Namespace> namespaces = new HashMap<>();
    List<Integer> processedDefinitions = new ArrayList<>();
    List<Integer> processedSlots = new ArrayList<>();

    for (Record r : result) {
      de.dataelementhub.dal.jooq.tables.pojos.Element element =
          r.into(ELEMENT.fields()).into(de.dataelementhub.dal.jooq.tables.pojos.Element.class);
      de.dataelementhub.dal.jooq.tables.pojos.Definition definition =
          r.into(DEFINITION.fields())
              .into(de.dataelementhub.dal.jooq.tables.pojos.Definition.class);
      de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier scopedIdentifier =
          r.into(SCOPED_IDENTIFIER.fields())
              .into(de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier.class);
      de.dataelementhub.dal.jooq.tables.pojos.Slot slot =
          r.into(SLOT.fields()).into(de.dataelementhub.dal.jooq.tables.pojos.Slot.class);

      if (namespaces.containsKey(element.getId())) {
        if (!processedDefinitions.contains(definition.getId())) {
          namespaces.get(element.getId()).getDefinitions()
              .add(DefinitionHandler.convert(definition));
          processedDefinitions.add(definition.getId());
        }
        if (slot != null && slot.getId() != null && !processedSlots.contains(slot.getId())) {
          namespaces.get(element.getId()).getSlots().add(SlotHandler.convert(slot));
          processedSlots.add(slot.getId());
        }
      } else {
        Namespace namespace = new Namespace();
        namespace.setDefinitions(new ArrayList<>());
        namespace.setSlots(new ArrayList<>());
        namespace.getDefinitions().add(DefinitionHandler.convert(definition));
        processedDefinitions.add(definition.getId());
        namespace.setIdentification(IdentificationHandler.convert(scopedIdentifier));
        if (slot != null && slot.getId() != null) {
          namespace.getSlots().add(SlotHandler.convert(slot));
          processedSlots.add(slot.getId());
        }
        if (element.getHidden() != null) {
          namespace.getIdentification().setHideNamespace(element.getHidden());
        } else {
          namespace.getIdentification().setHideNamespace(false);
        }
        namespaces.put(element.getId(), namespace);
      }
    }

    return new ArrayList<>(namespaces.values());
  }


  /**
   * Returns a list of all non-hidden namespaces.
   */
  public static List<Namespace> getPublicNamespaces(CloseableDSLContext ctx) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(ELEMENT.HIDDEN.isNull().or(ELEMENT.HIDDEN.eq(false)));
    return fetchNamespaceQuery(query);
  }

  /**
   * Returns all readable namespaces, including the implicitly readable namespaces.
   */
  public static List<Namespace> getReadableNamespaces(CloseableDSLContext ctx, int userId) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(DaoUtil.accessibleByUserId(ctx, userId));
    return fetchNamespaceQuery(query);
  }

  /**
   * Returns a list of namespaces which the user can explicitly read (as defined in the
   * "user_namespace_access" table).
   */
  public static List<Namespace> getExplicitlyReadableNamespaces(CloseableDSLContext ctx,
      int userId) {
    return getNamespacesByAccessLevel(ctx, userId, AccessLevelType.READ);
  }

  /**
   * Returns all writable namespaces (which are not hidden or which are writable for the user as
   * defined in the "user_namespace_access" table.
   */
  public static List<Namespace> getWritableNamespaces(CloseableDSLContext ctx, int userId) {
    return getNamespacesByAccessLevel(ctx, userId, AccessLevelType.WRITE);
  }

  /**
   * Returns a list of namespaces which the user has admin access to (as defined in the
   * "user_namespace_access" table).
   */
  public static List<Namespace> getAdministrableNamespaces(CloseableDSLContext ctx, int userId) {
    return getNamespacesByAccessLevel(ctx, userId, AccessLevelType.ADMIN);
  }

  /**
   * Returns a list of namespaces the user has the given access type to.
   *
   * @param accessLevel "READ", "WRITE" or "ADMIN"
   */
  public static List<Namespace> getNamespacesByAccessLevel(
      CloseableDSLContext ctx, int userId, AccessLevelType accessLevel) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(ELEMENT.ID.in(DaoUtil
        .getUserNamespaceAccessQuery(ctx, userId, Collections.singletonList(accessLevel))));
    return fetchNamespaceQuery(query);
  }

  /**
   * Updates definition of a namespace.
   */
  public static Identification update(CloseableDSLContext ctx, int userId, Namespace namespace) {
    Namespace previousNamespace = getByUrn(ctx, userId, namespace.getIdentification().getUrn());

    //update scopedIdentifier if status != DRAFT
    if (previousNamespace.getIdentification().getStatus() != Status.DRAFT) {
      final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);

      ScopedIdentifier scopedIdentifier = IdentificationHandler.updateNamespaceIdentifier(ctx,
          namespace.getIdentification());
      Identification newIdentification = IdentificationHandler.convert(scopedIdentifier);
      newIdentification.setNamespaceId(NamespaceHandler.getNamespaceIdByUrn(
          previousNamespace.getIdentification().getNamespaceUrn()));
      Boolean newHideNamespace = namespace.getIdentification().getHideNamespace();
      newIdentification.setHideNamespace(newHideNamespace != null ? newHideNamespace :
          previousNamespace.getIdentification().getHideNamespace());
      List<UserNamespaceAccess> namespaceAccess = AccessLevelHandler
          .getAccessForNamespaceById(ctx, previousNamespace.getIdentification().getNamespaceId());

      delete(ctx, userId, previousNamespace.getIdentification().getUrn());
      namespace.setIdentification(newIdentification);
      ScopedIdentifier newScopedIdentifier = create(ctx, userId, namespace);
      IdentificationHandler.convert(newScopedIdentifier);

      namespaceAccess.forEach(g -> g.setNamespaceId(newScopedIdentifier.getNamespaceId()));

      AccessLevelHandler.setAccessForNamespace(ctx, namespaceAccess);
      updateNamespaceIds(ctx, userId, previousNamespace.getIdentification().getNamespaceId(),
          newScopedIdentifier.getNamespaceId());

      CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
      return IdentificationHandler.convert(newScopedIdentifier);
    } else {
      DefinitionHandler.updateDefinitions(ctx, userId,
          previousNamespace.getIdentification().getUrn(), namespace.getDefinitions());
      SlotHandler.updateSlots(ctx, userId,
          previousNamespace.getIdentification().getUrn(), namespace.getSlots());
      // update identification
      if (previousNamespace.getIdentification().getStatus() != namespace.getIdentification()
          .getStatus()) {
        IdentificationHandler.updateStatus(ctx, userId, previousNamespace.getIdentification(),
            namespace.getIdentification().getStatus());
      }
      if (previousNamespace.getIdentification().getHideNamespace() != namespace.getIdentification()
          .getHideNamespace()) {
        setHideNamespace(ctx, userId, namespace.getIdentification().getUrn(),
            namespace.getIdentification().getHideNamespace());
      }
      return namespace.getIdentification();
    }
  }

  /**
   * Update the hidden attribute of a namespace element.
   */
  private static void setHideNamespace(CloseableDSLContext ctx, int userId, String urn,
      Boolean hideNamespace) {
    ScopedIdentifier scopedIdentifier = IdentificationHandler.getScopedIdentifier(ctx, urn);
    if (scopedIdentifier.getElementType() != ElementType.NAMESPACE) {
      throw new IllegalArgumentException();
    }
    ctx.update(ELEMENT)
        .set(ELEMENT.HIDDEN, hideNamespace)
        .where(ELEMENT.ID.eq(scopedIdentifier.getElementId()))
        .execute();
  }

  /**
   * Get all Members for the list view representation by a namespace id.
   */
  public static List<NamespaceMember> getNamespaceMembersListview(CloseableDSLContext ctx,
      int userId,
      Integer namespaceIdentifier, List<ElementType> elementTypes, Boolean hideSubElements) {

    List<NamespaceMember> namespaceMembers = new ArrayList<>();
    if (elementTypes == null || elementTypes.isEmpty()) {
      elementTypes = Arrays
          .asList(ElementType.DATAELEMENT, ElementType.DATAELEMENTGROUP, ElementType.RECORD);
    }

    JdbcMapper<Tuple2<ListviewElementRecord, List<DefinitionRecord>>> mapper =
        JdbcMapperFactory.newInstance()
            .addKeys("si_id", "id")
            .newMapper(new TypeReference<Tuple2<ListviewElementRecord, List<DefinitionRecord>>>() {
            });
    try {
      ResultSet rs = null;
      if (hideSubElements) {
        rs =
          ctx
              .select(
                  LISTVIEW_ELEMENT.SI_ID,
                  LISTVIEW_ELEMENT.SI_IDENTIFIER,
                  LISTVIEW_ELEMENT.SI_VERSION,
                  LISTVIEW_ELEMENT.SI_STATUS,
                  LISTVIEW_ELEMENT.ID,
                  LISTVIEW_ELEMENT.ELEMENT_TYPE,
                  LISTVIEW_ELEMENT.VD_DATATYPE,
                  DEFINITION.ID,
                  DEFINITION.LANGUAGE,
                  DEFINITION.DESIGNATION,
                  DEFINITION.DEFINITION_)
              .from(LISTVIEW_ELEMENT)
              .leftJoin(DEFINITION).on(DEFINITION.SCOPED_IDENTIFIER_ID.eq(LISTVIEW_ELEMENT.SI_ID))
              .where(LISTVIEW_ELEMENT.ELEMENT_TYPE.in(elementTypes))
              .andNotExists(ctx.select(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID)
                  .from(SCOPED_IDENTIFIER_HIERARCHY)
                  .where(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID.eq(LISTVIEW_ELEMENT.SI_ID))
                  .andNotExists(ctx.select(SCOPED_IDENTIFIER.ID).from(SCOPED_IDENTIFIER)
                      .where(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID))
                      .and(SCOPED_IDENTIFIER.STATUS.eq(Status.OUTDATED))))
              .and(LISTVIEW_ELEMENT.SI_NAMESPACE_ID.eq(
                  ctx.select(SCOPED_IDENTIFIER.ID)
                      .from(SCOPED_IDENTIFIER)
                      .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
                      .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceIdentifier))
                      .orderBy(SCOPED_IDENTIFIER.VERSION.desc())
                      .limit(1)
              ))
              .orderBy(LISTVIEW_ELEMENT.SI_ID).fetchResultSet();
      } else {
        rs =
          ctx
              .select(
                  LISTVIEW_ELEMENT.SI_ID,
                  LISTVIEW_ELEMENT.SI_IDENTIFIER,
                  LISTVIEW_ELEMENT.SI_VERSION,
                  LISTVIEW_ELEMENT.SI_STATUS,
                  LISTVIEW_ELEMENT.ID,
                  LISTVIEW_ELEMENT.ELEMENT_TYPE,
                  LISTVIEW_ELEMENT.VD_DATATYPE,
                  DEFINITION.ID,
                  DEFINITION.LANGUAGE,
                  DEFINITION.DESIGNATION,
                  DEFINITION.DEFINITION_)
              .from(LISTVIEW_ELEMENT)
              .leftJoin(DEFINITION).on(DEFINITION.SCOPED_IDENTIFIER_ID.eq(LISTVIEW_ELEMENT.SI_ID))
              .where(LISTVIEW_ELEMENT.ELEMENT_TYPE.in(elementTypes))
              .and(LISTVIEW_ELEMENT.SI_NAMESPACE_ID.eq(
                  ctx.select(SCOPED_IDENTIFIER.ID)
                      .from(SCOPED_IDENTIFIER)
                      .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
                      .and(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceIdentifier))
                      .orderBy(SCOPED_IDENTIFIER.VERSION.desc())
                      .limit(1)
              ))
              .orderBy(LISTVIEW_ELEMENT.SI_ID).fetchResultSet();
      }
      mapper.stream(rs).forEach(t2 -> {
        NamespaceMember nsm = new NamespaceMember();
        nsm.setRevision(t2.v1().getSiVersion());
        nsm.setIdentifier(t2.v1().getSiIdentifier());
        nsm.setElementType(t2.v1().getElementType());
        nsm.setStatus(t2.v1().getSiStatus());
        nsm.setValidationType(t2.v1().getVdDatatype());
        List<Definition> definitions = new ArrayList<>();
        t2.v2().forEach(def -> {
          Definition definition = new Definition();
          definition.setDefinition(def.getDefinition());
          definition.setDesignation(def.getDesignation());
          definition.setLanguage(def.getLanguage());
          definitions.add(definition);
        });
        nsm.setDefinitions(definitions);
        namespaceMembers.add(nsm);
      });
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return namespaceMembers;
  }

  /**
   * Read the users permissions for a given namespace (by namespace identifier).
   */
  public static List<DeHubUserPermission> getUserAccessForNamespace(CloseableDSLContext ctx,
      int userId, int namespaceIdentifier) throws IllegalAccessException {
    if (AccessLevelHandler.getAccessLevelByUserAndNamespaceIdentifier(ctx, userId,
        namespaceIdentifier) != AccessLevelType.ADMIN) {
      throw new IllegalAccessException("Insufficient rights to manage namespace grants.");
    }
    List<UserNamespaceAccess> userNamespaceAccess = AccessLevelHandler
        .getAccessForNamespaceByIdentifier(ctx, namespaceIdentifier);
    List<DeHubUserPermission> permissions = new ArrayList<>();
    userNamespaceAccess.forEach(una -> {
      DehubUser user = UserHandler.getUserById(ctx, una.getUserId());
      permissions.add(new DeHubUserPermission(user.getAuthId(), una.getAccessLevel().getLiteral()));
    });
    return permissions;
  }

  /**
   * Update the namespace ids in the scoped identifier table when a namespace is updated.
   */
  private static int updateNamespaceIds(CloseableDSLContext ctx, int userId, int oldId,
      int newId) {
    return ctx.update(SCOPED_IDENTIFIER)
        .set(SCOPED_IDENTIFIER.NAMESPACE_ID, newId)
        .where(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(oldId))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.notEqual(ElementType.NAMESPACE))
        .execute();
  }
}
