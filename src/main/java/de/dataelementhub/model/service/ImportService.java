package de.dataelementhub.model.service;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.STAGING;
import static de.dataelementhub.model.DaoUtil.WRITE_ACCESS_TYPES;

import de.dataelementhub.dal.jooq.enums.AccessLevelType;
import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import de.dataelementhub.dal.jooq.tables.records.ImportRecord;
import de.dataelementhub.model.DaoUtil;
import de.dataelementhub.model.dto.importdto.ImportInfo;
import de.dataelementhub.model.dto.listviews.StagedElement;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.importhandler.ImportHandler;
import de.dataelementhub.model.handler.importhandler.StagedElementHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jooq.CloseableDSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportService {

  /**
   * Execute an import.
   **/
  public void execute(
      CloseableDSLContext ctx, List<MultipartFile> files,
      String importDirectory, int userId, int importId)
      throws IOException {
    String destination = ImportHandler.createImportDirectory(importDirectory, userId, importId);
    for (MultipartFile file : files) {
      Path fileNameAndPath = Paths.get(destination, file.getOriginalFilename());
      ImportHandler.unzip(fileNameAndPath.toString(), destination);
      File[] allFilesInFolder = ImportHandler.getAllFilesInFolder(destination);
      ImportHandler.validateAllFilesInFolder(allFilesInFolder);
      importToStagingArea(ctx, allFilesInFolder, importId);
    }
  }

  /**
   * Save stagedElements to the staging area.
   **/
  @Async
  public void importToStagingArea(CloseableDSLContext ctx, File[] allFilesInFolder, int importId) {
    try {
      ImportHandler.startImportAccordingToFileType(ctx, importId, allFilesInFolder);
    } catch (Exception e) {
      ctx.update(IMPORT)
          .set(IMPORT.STATUS, ProcessStatus.ABORTED)
          .set(IMPORT.LABEL, e.getMessage())
          .where(IMPORT.ID.eq(importId))
          .execute();
    }
  }

  /**
   * Convert stagedElements to Drafts.
   **/
  @Async
  public void convertToDraft(
      CloseableDSLContext ctx, List<String> stagedElementsIds, int userId, int importId) throws
      IllegalAccessException {
    if (!importExists(ctx, importId)) {
      throw new NoSuchElementException();
    }
    if (!importAccessGranted(ctx, importId, userId)) {
      throw new IllegalAccessException();
    }
    ImportHandler.convertToDrafts(ctx, importId, userId, stagedElementsIds);
  }

  /** Check user grants then delete staged import. */
  public void deleteStagedImport(
      CloseableDSLContext ctx, int userId, int importId) throws IllegalAccessException,
      NoSuchElementException {
    if (!importExists(ctx, importId)) {
      throw new NoSuchElementException();
    }
    Integer namespaceIdentifier = ctx.select().from(IMPORT).leftJoin(SCOPED_IDENTIFIER)
        .on(IMPORT.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.ELEMENT_ID))
        .where(IMPORT.ID.eq(importId)).fetchOne().getValue(SCOPED_IDENTIFIER.IDENTIFIER);
    if (importAccessGranted(ctx, importId, userId)) {
      ctx.deleteFrom(IMPORT).where(IMPORT.ID.eq(importId)).execute();
      ctx.deleteFrom(STAGING).where(STAGING.IMPORT_ID.eq(importId)).execute();
    } else {
      throw new IllegalAccessException();
    }
  }

  /** Generate importId. */
  public int generateImportId(CloseableDSLContext ctx, String namespaceUrn, int userId,
      List<MultipartFile> files, String importDirectory) throws IOException {
    int namespaceId = IdentificationHandler
        .getScopedIdentifier(ctx, namespaceUrn).getNamespaceId();
    ImportRecord importIdRecord = ctx.insertInto(IMPORT)
        .set(IMPORT.NAMESPACE_ID, namespaceId)
        .set(IMPORT.STATUS, ProcessStatus.PROCESSING)
        .set(IMPORT.CREATED_BY, userId)
        .returning(IMPORT.ID)
        .fetchOne();
    int importId = importIdRecord != null ? importIdRecord.getId() : -1;
    String destination = importDirectory + File.separator + userId + File.separator + importId;
    new File(importDirectory + File.separator + userId).mkdir();
    new File(destination).mkdir();
    for (MultipartFile file : files) {
      Path fileNameAndPath = Paths.get(destination, file.getOriginalFilename());
      Files.write(fileNameAndPath, file.getBytes());
    }
    return importId;
  }

  /** Returns all Imports. */
  public List<ImportInfo> listAllImports(CloseableDSLContext ctx, int userId) {
    List<ImportInfo> importInfoList = new ArrayList<>();
    List<ImportRecord> imports = ctx.selectFrom(IMPORT).where(IMPORT.CREATED_BY.eq(userId)
        .or(IMPORT.NAMESPACE_ID.in(DaoUtil.getUserNamespaceAccessQuery(ctx, userId,
            allowedAccessLevelTypes())))).fetch();
    imports.forEach(
        importRecord -> {
          ImportInfo importInfo = ImportHandler.importRecordToImportInfo(ctx, importRecord);
          importInfoList.add(importInfo);
        }
    );
    return importInfoList;
  }

  /** Get import info by ID. */
  public ImportInfo getImportInfo(CloseableDSLContext ctx, int importId, int userId)
      throws IllegalAccessException, NoSuchElementException {
    if (!importExists(ctx, importId)) {
      throw new NoSuchElementException();
    }
    if (!importAccessGranted(ctx, importId, userId)) {
      throw new IllegalAccessException();
    }
    ImportRecord importRecord = Objects.requireNonNull(
        ctx.selectFrom(IMPORT).where(IMPORT.ID.eq(importId))
            .and(IMPORT.CREATED_BY.eq(userId)
                .or(IMPORT.NAMESPACE_ID.in(DaoUtil.getUserNamespaceAccessQuery(ctx, userId,
                    allowedAccessLevelTypes())))).fetchOne());
    return ImportHandler.importRecordToImportInfo(ctx, importRecord);
  }

  /** Get Import members. */
  public List<StagedElement> getImportMembersListView(
      CloseableDSLContext ctx, int importId, int userId,
      Boolean hideSubElements, Boolean onlyConverted)
      throws IllegalAccessException, NoSuchElementException {
    Result<Record> stagingRecords;
    if (!importExists(ctx, importId)) {
      throw new NoSuchElementException();
    }
    if (!importAccessGranted(ctx, importId, userId)) {
      throw new IllegalAccessException();
    }
    if (hideSubElements) {
      List<String> subElements = new ArrayList<>();
      for (String s : ctx.select(STAGING.MEMBERS)
          .from(STAGING)
          .where(STAGING.IMPORT_ID.eq(importId)).fetch().into(String.class)) {
        String[] split = s.split(";");
        subElements.addAll(List.of(split));
      }
      stagingRecords = ctx
          .select()
          .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
          .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
              .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
          .and(STAGING.STAGED_ELEMENT_ID.notIn(subElements))
          .fetch();
    } else {
      stagingRecords = ctx
          .select()
          .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
          .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
              .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
          .fetch();
    }
    List<StagedElement> stagedElements =
        StagedElementHandler.stagingRecordsToStagedElements(ctx, stagingRecords);
    if (onlyConverted) {
      stagedElements = stagedElements.stream().filter(se -> se.getElementUrn() != null).collect(
          Collectors.toList());
    }
    return stagedElements;
  }

  /** Get StagedElement Members. */
  public List<StagedElement> getStagedElementMembers(
      CloseableDSLContext ctx, int importId, int userId,
      String stagedElementId, Boolean onlyConverted) throws IllegalAccessException {
    List<StagedElement> stagedElementMembers;
    if (!importAccessGranted(ctx, importId, userId)) {
      throw new IllegalAccessException();
    }
    List<StagedElement> stagedElements = StagedElementHandler
        .getStagedElementMembers(ctx, importId, userId, stagedElementId);
    if (onlyConverted) {
      stagedElements = stagedElements.stream().filter(se -> se.getElementUrn() != null).collect(
          Collectors.toList());
    }
    return stagedElements;
  }

  /** Get StagedElement by ID from specified Import. */
  public de.dataelementhub.model.dto.element.StagedElement getStagedElement(
      CloseableDSLContext ctx, int importId, int userId, String stagedElementId)
      throws IllegalAccessException {
    if (!importAccessGranted(ctx, importId, userId)) {
      throw new IllegalAccessException();
    }
    return StagedElementHandler.getStagedElement(ctx, importId, userId, stagedElementId);
  }

  /** Check if an import exists by ID. */
  private boolean importExists(CloseableDSLContext ctx, int importId) {
    return ctx.fetchExists(ctx.selectFrom(IMPORT).where(IMPORT.ID.eq(importId)));
  }

  /** Check if user is allowed to access an import. */
  private boolean importAccessGranted(CloseableDSLContext ctx, int importId, int userId) {
    return ctx.fetchExists(ctx.selectFrom(IMPORT).where(IMPORT.CREATED_BY.eq(userId)
        .or(IMPORT.NAMESPACE_ID.in(DaoUtil.getUserNamespaceAccessQuery(ctx, userId,
            allowedAccessLevelTypes())))).and(IMPORT.ID.eq(importId)));
  }

  /** Return grantTypes that allowed to access an import. */
  private List<AccessLevelType> allowedAccessLevelTypes() {
    return WRITE_ACCESS_TYPES;
  }
}
