package de.dataelementhub.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import de.dataelementhub.dal.jooq.tables.records.ImportRecord;
import de.dataelementhub.model.dto.importdto.ImportInfo;
import de.dataelementhub.model.dto.listviews.StagedElement;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import de.dataelementhub.model.handler.importhandler.ImportHandler;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import org.jooq.CloseableDSLContext;
import org.jooq.Record1;
import org.jooq.RecordType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.STAGING;

@Service
public class ImportService {

  /**
   * Accept zip file and import its content (xml/json).
   **/
  @Async
  public void importService(
      List<MultipartFile> file, String importDirectory, int userId,
      int importId, String timestamp) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ImportHandler.importFiles(ctx, file, importDirectory,
          userId, importId, timestamp);
    }
  }

  /** Generate importId. */
  public int generateImportId(String namespaceUrn, int userId, List<MultipartFile> files,
      String importDirectory, String timestamp) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      String destination = importDirectory + "/" + userId + "/" + timestamp;
      new File(importDirectory + "/" + userId).mkdir();
      new File(destination).mkdir();
      for (MultipartFile file : files) {
        Path fileNameAndPath = Paths.get(destination, file.getOriginalFilename());
        try {
          Files.write(fileNameAndPath, file.getBytes());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      int namespaceId = IdentificationHandler
          .getScopedIdentifier(ctx, namespaceUrn).getNamespaceId();
      ImportRecord importIdRecord = ctx.insertInto(IMPORT)
          .set(IMPORT.NAMESPACE_ID, namespaceId)
          .set(IMPORT.STATUS, ProcessStatus.PROCESSING)
          .set(IMPORT.CREATED_BY, userId)
          .returning(IMPORT.ID)
          .fetchOne();
      return importIdRecord != null ? importIdRecord.getId() : -1;
    }
  }

  /** Returns all Imports. */
  public List<ImportInfo> allImports(int userId) {
    List<ImportInfo> importInfoList = new ArrayList<>();
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      ctx.selectFrom(IMPORT).where(IMPORT.CREATED_BY.eq(userId)).fetch().forEach(
          importRecord -> {
            ImportInfo importInfo = new ImportInfo();
            importInfo.setId(importRecord.getId());
            importInfo.setStatus(importRecord.getStatus());
            importInfo.setNamespaceUrn(NamespaceHandler
                .getNamespaceUrnById(importRecord.getNamespaceId()));
            importInfo.setTimestamp(Timestamp.valueOf(importRecord.getCreatedAt()));
            importInfoList.add(importInfo);
          }
      );
    }
    return importInfoList;
  }

  /** returns the import status PROCESSING/DONE/INTERRUPTED/NOT DEFINED. */
  public ProcessStatus checkStatus(String identifier, int userId) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return Objects.requireNonNull(
          ctx.selectFrom(IMPORT).where(IMPORT.ID.eq(Integer.valueOf(identifier)))
              .and(IMPORT.CREATED_BY.eq(userId)).fetchOne()).getStatus();
    }
  }

  /** . */
  public List<StagedElement> getImportMembersListView(int importId, int userId, Boolean hideSubElements) {
    List<StagedElement> stagedElements;
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      if (hideSubElements) {
        List<String> subElements = new ArrayList<>();
        for (String s : ctx.select(STAGING.MEMBERS)
            .from(STAGING)
            .where(STAGING.IMPORT_ID.eq(importId)).fetch().into(String.class)) {
          String[] split = s.split(";");
          subElements.addAll(List.of(split));
        }
        stagedElements = ctx
            .select(STAGING.STAGED_ELEMENT_ID, STAGING.DESIGNATION, STAGING.ELEMENT_TYPE)
            .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
            .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
                .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
            .and(STAGING.STAGED_ELEMENT_ID.notIn(subElements))
            .fetch().into(StagedElement.class);
      } else {
        stagedElements = ctx
            .select(STAGING.STAGED_ELEMENT_ID, STAGING.DESIGNATION, STAGING.ELEMENT_TYPE)
            .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
            .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
                .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
            .fetch().into(StagedElement.class);
      }
    }
    return stagedElements;
  }

  /** . */
  public List<StagedElement> getStagedElementMembers(int importId, int userId, String stagedElementId) {
    List<StagedElement> stagedElementMembers;
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      List<String> stagedElementMembersIds = List.of(ctx.select(STAGING.MEMBERS)
          .from(STAGING)
          .where(STAGING.IMPORT_ID.eq(importId))
          .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId)).fetchOne().into(String.class)
          .split(";"));
      stagedElementMembers = ctx
          .select(STAGING.STAGED_ELEMENT_ID, STAGING.DESIGNATION, STAGING.ELEMENT_TYPE)
          .from(STAGING).where(STAGING.IMPORT_ID.eq(importId))
          .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
              .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
          .and(STAGING.STAGED_ELEMENT_ID.in(stagedElementMembersIds))
          .fetch().into(StagedElement.class);
    }
    return stagedElementMembers;
  }

  /** .
   * @return*/
  public de.dataelementhub.model.dto.element.StagedElement getStagedElement(
      int importId, int userId, String stagedElementId) throws JsonProcessingException {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      String stagedElementAsString = String.valueOf(ctx.select(STAGING.DATA)
          .from(STAGING)
          .where(STAGING.IMPORT_ID.eq(importId))
          .and(STAGING.STAGED_ELEMENT_ID.eq(stagedElementId))
          .and(String.valueOf(ctx.fetchExists(ctx.selectFrom(IMPORT)
              .where(IMPORT.ID.eq(importId)).and(IMPORT.CREATED_BY.eq(userId)))))
          .fetchOneInto(String.class));
      return stagedElementAsString != null ?
          new ObjectMapper().readValue(stagedElementAsString,
          de.dataelementhub.model.dto.element.StagedElement.class) : null;
    }
  }

}
