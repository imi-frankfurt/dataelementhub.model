package de.dataelementhub.model.handler.importhandler;

import static de.dataelementhub.dal.jooq.Tables.IMPORT;
import static de.dataelementhub.dal.jooq.Tables.STAGING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.dal.jooq.enums.ProcessStatus;
import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.element.section.Member;
import de.dataelementhub.model.dto.importdto.ImportDto;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
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
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.multipart.MultipartFile;

public class ImportHandler {

  /** Unzip received file and handle importing its content. */
  public static void importFiles(
      CloseableDSLContext ctx, List<MultipartFile> files,
      String importDirectory, int userId,
      int importId, String timestamp) {
    String destination = importDirectory + "/" + userId + "/" + timestamp;
    new File(importDirectory + "/" + userId).mkdir();
    new File(destination).mkdir();
    for (MultipartFile file : files) {
      Path fileNameAndPath = Paths.get(destination, file.getOriginalFilename());
      try {
        unzip(fileNameAndPath.toString(), destination);
        allFilesInFolder(ctx, destination, importId);
      } catch (Exception e) {
        e.printStackTrace();
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
    jaxbUnmarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, "application/json");
    jaxbUnmarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    StreamSource json = new StreamSource(new StringReader(readFileAsString(fileToImport)));
    ImportDto importDtoBody = jaxbUnmarshaller.unmarshal(json, ImportDto.class).getValue();
    saveElements(ctx, importDtoBody.getStagedElements(), importId);
  }

  /** Convert stagedElements to elements and save them. */
  public static void saveElements(CloseableDSLContext ctx, List<StagedElement> stagedElements, int importId) {
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
                        .size() > 0 ? stagedElement.getDefinitions().get(0).getDesignation() :
                        stagedElement.getIdentification().getElementType().toString())
                    .set(STAGING.IMPORT_ID, importId)
                    .set(STAGING.STAGED_ELEMENT_ID, stagedElement.getIdentification().getUrn())
                    .set(STAGING.MEMBERS, membersAsString)
                    .execute();
              } catch (JsonProcessingException e) {
                e.printStackTrace();
              }
            }));
    ctx.update(IMPORT)
        .set(IMPORT.STATUS, ProcessStatus.DONE)
        .where(IMPORT.ID.eq(importId))
        .execute();
  }

  /** returns JSON file as String. */
  public static String readFileAsString(String file) throws Exception {
    return new String(Files.readAllBytes(Paths.get(file)));
  }

  /** Defines which elements should be imported first. */
  public static Integer elementTypeOrder(ElementType elementType) {
    switch (elementType) {
      case PERMISSIBLE_VALUE:
        return 1;
      case ENUMERATED_VALUE_DOMAIN:
      case DESCRIBED_VALUE_DOMAIN:
        return 2;
      case DATAELEMENT:
        return 3;
      case DATAELEMENTGROUP:
      case RECORD:
        return 4;
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }

  /** File validation against XSD/JSON Schema. */
  public static void validateAgainstSchema(String fileToValidate) throws Exception {
    if (fileToValidate.contains(".xsd")) {
      File schemaFile =
          new File(
              System.getProperty("user.dir")
                  + "/src/main/resources/schema/StagingImport.xsd");
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
                          + "/src/main/resources/schema/StagingImport.json")));
      JSONObject jsonSubject = new JSONObject(new JSONTokener(new FileInputStream(fileToValidate)));
      org.everit.json.schema.Schema schema = SchemaLoader.load(jsonSchema);
      schema.validate(jsonSubject);
    }
  }
}
