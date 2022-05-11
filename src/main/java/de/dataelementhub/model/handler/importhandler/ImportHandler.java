package de.dataelementhub.model.handler.importhandler;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.STAGING;
import static org.jooq.impl.DSL.count;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.ImportRecord;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.importexport.ImportExport;
import de.dataelementhub.model.dto.importexport.ImportInfo;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.everit.json.schema.loader.SchemaLoader;
import org.jooq.CloseableDSLContext;
import org.jooq.Record2;
import org.jooq.impl.SQLDataType;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.MediaType;

/**
 * Import Handler.
 */
public class ImportHandler {

  /** Create import directory and return it. */
  public static String createImportDirectory(String importDirectory, int userId, int importId) {
    String destination = importDirectory + File.separator + userId + File.separator + importId;
    new File(importDirectory + File.separator + userId).mkdir();
    new File(destination).mkdir();
    return destination;
  }

  /** Unzip to defined directory. */
  public static void unzip(String source, String destination) throws ZipException {
    ZipFile zipFile = new ZipFile(source);
    zipFile.extractAll(destination);
  }

  /** list all files inside a defined directory.*/
  public static File[] getAllFilesInFolder(String folder) {
    File inputFolder = new File(folder);
    File[] listOfFiles = inputFolder.listFiles();
    return listOfFiles;
  }

  /** Validate all files in a folder against schema and throw an exception
   * in case the files are not valid.*/
  public static void validateAllFilesInFolder(File[] allFilesInFolder) throws IOException {
    for (File file : Objects.requireNonNull(allFilesInFolder)) {
      if (file.isFile() && file.getName().contains(".json") || file.getName().contains(".xml")) {
        validateAgainstSchema(file.getAbsolutePath());
      }
    }
  }

  /** Detect file type (xml/json) then handle importing it.*/
  public static void startImportAccordingToFileType(
      CloseableDSLContext ctx, int importId, File[] allFilesInFolder) throws Exception {
    for (File file : Objects.requireNonNull(allFilesInFolder)) {
      if (file.isFile() && !file.getName().contains(".zip")) {
        String fileAbsolutePath = file.getAbsolutePath();
        if (fileAbsolutePath.contains(".xml")) {
          importXml(ctx, fileAbsolutePath, importId);
        } else if (fileAbsolutePath.contains(".json")) {
          importJson(ctx, fileAbsolutePath, importId);
        }
      }
    }
  }

  /** handles importing xml file. */
  public static void importXml(
      CloseableDSLContext ctx, String fileToImport, int importId)
      throws Exception {
    File file = new File(fileToImport);
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportExport.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    ImportExport importExport = (ImportExport) jaxbUnmarshaller.unmarshal(file);
    saveElements(ctx, importExport.getStagedElements(), importId);
  }

  /** handles importing json file. */
  public static void importJson(
      CloseableDSLContext ctx, String fileToImport, int importId)
      throws Exception {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportExport.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE,
        MediaType.APPLICATION_JSON_VALUE);
    jaxbUnmarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    StreamSource json = new StreamSource(new StringReader(readFileAsString(fileToImport)));
    ImportExport importExport = jaxbUnmarshaller.unmarshal(json, ImportExport.class).getValue();
    saveElements(ctx, importExport.getStagedElements(), importId);
  }

  /** Convert stagedElements to elements and save them. */
  public static void saveElements(
      CloseableDSLContext ctx, List<StagedElement> stagedElements, int importId) {
    ctx.update(IMPORT)
        .set(IMPORT.NUMBER_OF_ELEMENTS, stagedElements.size())
        .where(IMPORT.ID.eq(importId))
        .execute();
    stagedElements
        .forEach(
            (stagedElement -> {
              String membersAsString = stagedElement.getMembers() != null ? stagedElement
                  .getMembers().stream().map(Member::getElementUrn)
                  .collect(Collectors.joining(";")) : "";
              try {
                ctx.insertInto(STAGING)
                    .set(STAGING.DATA, new ObjectMapper().writeValueAsString(stagedElement))
                    .set(STAGING.ELEMENT_TYPE, stagedElement.getIdentification().getElementType())
                    .set(STAGING.DESIGNATION, stagedElement.getDefinitions()
                        .size() > 0 ? stagedElement.getDefinitions().get(0).getDesignation() : "")
                    .set(STAGING.IMPORT_ID, importId)
                    .set(STAGING.STAGED_ELEMENT_ID, stagedElement.getIdentification().getUrn())
                    .set(STAGING.MEMBERS, membersAsString)
                    .execute();
                ctx.update(IMPORT)
                    .set(IMPORT.STATUS, ProcessStatus.COMPLETED)
                    .where(IMPORT.ID.eq(importId))
                    .execute();
              } catch (JsonProcessingException e) {
                ctx.update(IMPORT)
                    .set(IMPORT.STATUS, ProcessStatus.ABORTED)
                    .set(IMPORT.LABEL, e.getMessage())
                    .where(IMPORT.ID.eq(importId))
                    .execute();
              }
            }));
  }

  /** returns JSON file as String. */
  public static String readFileAsString(String file) throws Exception {
    return new String(Files.readAllBytes(Paths.get(file)));
  }

  /** File validation against XSD/JSON Schema. */
  public static void validateAgainstSchema(String fileToValidate) throws IOException {
    try {
      if (fileToValidate.contains(".xsd")) {
        File schemaFile =
            new File(
                System.getProperty("user.dir")
                    + "/src/main/resources/schema/StagingImport.xsd"
                    .replace('/', File.separatorChar));
        Source xmlFile = new StreamSource(new File(fileToValidate));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
      } else if (fileToValidate.contains(".json")) {
        JSONObject jsonSchema =
            new JSONObject(
                new JSONTokener(
                    new FileInputStream(
                        System.getProperty("user.dir")
                            + "/src/main/resources/schema/StagingImport.json"
                            .replace('/', File.separatorChar))));
        JSONObject jsonSubject =
            new JSONObject(new JSONTokener(new FileInputStream(fileToValidate)));
        org.everit.json.schema.Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
      }
    } catch (Exception e) {
      throw new IOException("The import file you submitted did not pass validation.\n"
          + e.getMessage());
    }
  }

  /** Convert StagedElements to drafts. */
  public static void convertToDrafts(CloseableDSLContext ctx, int importId, int userId,
      List<String> stagedElementsIds) {
    Integer namespaceId = Objects.requireNonNull(
            ctx.select().from(IMPORT).where(IMPORT.ID.eq(importId)).fetchOne())
        .getValue(IMPORT.NAMESPACE_ID);
    ScopedIdentifier scopedIdentifier = ctx.selectFrom(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_ID.eq(namespaceId))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
        .fetchOneInto(ScopedIdentifier.class);
    String namespaceUrn = IdentificationHandler.toUrn(ctx, scopedIdentifier);
    stagedElementsIds.forEach(stagedElementId -> {
      StagedElement stagedElement = null;
      stagedElement = StagedElementHandler.getStagedElement(ctx, importId,
          userId, stagedElementId);
      try {
        StagedElementHandler.stagedElementToElement(ctx, stagedElement, namespaceUrn,
            userId, importId);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException();
      }
    });
  }

  /** Convert an importRecord to importInfo. */
  public static ImportInfo importRecordToImportInfo(
      CloseableDSLContext ctx, ImportRecord importRecord) {
    ImportInfo importInfo = new ImportInfo();
    double conversionProcess;
    double stagingProcess;
    try {
      Record2<Double, Double> countNotNullEntriesAndAllEntries =
          ctx.select(count(STAGING.SCOPED_IDENTIFIER_ID).cast(
                  SQLDataType.DOUBLE).as("notNull"),
              count().cast(
                  SQLDataType.DOUBLE).as("all")).from(STAGING)
          .where(STAGING.IMPORT_ID.eq(importRecord.getId())).fetchOne();
      conversionProcess =
          countNotNullEntriesAndAllEntries.value1() / countNotNullEntriesAndAllEntries.value2();
      stagingProcess =
          countNotNullEntriesAndAllEntries.value2() / importRecord.getNumberOfElements();
    } catch (Exception e) {
      stagingProcess = (double) 0;
      conversionProcess = (double) 0;
    }
    importInfo.setId(importRecord.getId());
    importInfo.setStatus(importRecord.getStatus());
    importInfo.setNamespaceUrn(NamespaceHandler
        .getNamespaceUrnById(ctx, importRecord.getNamespaceId()));
    importInfo.setConverted(conversionProcess);
    importInfo.setStaged(stagingProcess);
    importInfo.setTimestamp(Timestamp.valueOf(importRecord.getCreatedAt()));
    return importInfo;
  }
}
