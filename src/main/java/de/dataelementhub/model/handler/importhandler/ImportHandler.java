package de.dataelementhub.model.handler.importhandler;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.dataelementhub.dal.jooq.Tables.STAGING;
import static org.jooq.impl.DSL.count;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import de.dataelementhub.dal.jooq.tables.pojos.ScopedIdentifier;
import de.dataelementhub.dal.jooq.tables.records.ImportRecord;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.importdto.ImportDto;
import de.dataelementhub.model.dto.importdto.ImportInfo;
import de.dataelementhub.model.handler.element.NamespaceHandler;
import de.dataelementhub.model.handler.element.section.IdentificationHandler;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.jooq.CloseableDSLContext;
import org.jooq.Record2;
import org.jooq.impl.SQLDataType;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public class ImportHandler {

  /** Unzip received file and handle importing its content. */
  public static void importFiles(
      CloseableDSLContext ctx, List<MultipartFile> files,
      String importDirectory, int userId, int importId) {
    String destination = importDirectory + File.separator + userId + File.separator + importId;
    new File(importDirectory + File.separator + userId).mkdir();
    new File(destination).mkdir();
    for (MultipartFile file : files) {
      Path fileNameAndPath = Paths.get(destination, file.getOriginalFilename());
      try {
        unzip(fileNameAndPath.toString(), destination);
        allFilesInFolder(ctx, destination, importId);
      } catch (Exception e) {
        ctx.update(IMPORT)
            .set(IMPORT.STATUS, ProcessStatus.ABORTED)
            .set(IMPORT.LABEL, e.getMessage())
            .where(IMPORT.ID.eq(importId))
            .execute();
      }
    }
  }

  /** Unzip to defined directory. */
  public static void unzip(String source, String destination) throws ZipException {
    ZipFile zipFile = new ZipFile(source);
    zipFile.extractAll(destination);
  }

  /** list all files inside a defined directory. */
  public static void allFilesInFolder(CloseableDSLContext ctx, String folder, int importId)
      throws Exception {
    File inputFolder = new File(folder);
    File[] listOfFiles = inputFolder.listFiles();
    for (File file : Objects.requireNonNull(listOfFiles)) {
      if (file.isFile() && !file.getName().contains(".zip")) {
        importType(ctx, file.getAbsolutePath(), importId);
      }
    }
  }

  /** detect file type (xml/json) then handle importing it. */
  public static void importType(CloseableDSLContext ctx, String fileToImport,
      int importId) throws Exception {
    if (fileToImport.contains(".xml")) {
      importXml(ctx, fileToImport, importId);
    } else if (fileToImport.contains(".json")) {
      importJson(ctx, fileToImport, importId);
    }
  }

  /** handles importing xml file. */
  public static void importXml(
      CloseableDSLContext ctx, String fileToImport, int importId)
      throws Exception {
    File file = new File(fileToImport);
    validateAgainstSchema(fileToImport);
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    ImportDto importDtoBody = (ImportDto) jaxbUnmarshaller.unmarshal(file);
    saveElements(ctx, importDtoBody.getStagedElements(), importId);
  }

  /** handles importing json file. */
  public static void importJson(
      CloseableDSLContext ctx, String fileToImport, int importId)
      throws Exception {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    validateAgainstSchema(fileToImport);
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE,
        MediaType.APPLICATION_JSON_VALUE);
    jaxbUnmarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    StreamSource json = new StreamSource(new StringReader(readFileAsString(fileToImport)));
    ImportDto importDtoBody = jaxbUnmarshaller.unmarshal(json, ImportDto.class).getValue();
    saveElements(ctx, importDtoBody.getStagedElements(), importId);
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
  public static void validateAgainstSchema(String fileToValidate) throws Exception {
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
      URI jsonSchemaUri =
          new File(
              System.getProperty("user.dir")
                  + "/src/main/resources/schema/StagingImport.json"
                  .replace('/', File.separatorChar)).toURI();
      ObjectMapper mapper = new ObjectMapper();
      JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
      JsonSchema schema = factory.getSchema(jsonSchemaUri);
      JsonNode jsonNode = mapper.valueToTree(new StreamSource(new File(fileToValidate)));
      schema.validate(jsonNode);
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
    String namespaceUrn = IdentificationHandler.toUrn(scopedIdentifier);
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
        .getNamespaceUrnById(importRecord.getNamespaceId()));
    importInfo.setConverted(conversionProcess);
    importInfo.setStaged(stagingProcess);
    importInfo.setTimestamp(Timestamp.valueOf(importRecord.getCreatedAt()));
    return importInfo;
  }
}
