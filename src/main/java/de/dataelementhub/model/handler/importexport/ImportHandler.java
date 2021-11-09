package de.dataelementhub.model.handler.importexport;


import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.ElementType;
import de.dataelementhub.model.dto.importexport.ImportDto;
import de.dataelementhub.model.dto.importexport.StagedElement;
import net.lingala.zip4j.ZipFile;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.everit.json.schema.loader.SchemaLoader;
import org.jooq.CloseableDSLContext;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;

public class ImportHandler {

  public static Map<String, String> urnDict = new HashMap<String, String>();
  public static List<String> unImportableUrns = new ArrayList<>();

  /** Unzip received file and handle importing its content. */
  public static Future<String> importFiles(
      List<MultipartFile> files, String namespaceUrn, String importDirectory, int userId, String timestamp)
          throws Exception {
    new File(System.getProperty("user.dir") + "/uploads/import/" + userId).mkdir();
    new File(System.getProperty("user.dir") + "/uploads/import/" + userId + "/" + timestamp).mkdir();
    String destination = importDirectory + "/" + userId + "/" + timestamp;
    for (MultipartFile file : files) {
      Path fileNameAndPath = Paths.get(destination, userId + "_" + file.getOriginalFilename());
      try {
        Files.write(fileNameAndPath, file.getBytes());
        unzip(fileNameAndPath.toString(), destination);
      } catch (IOException e) {
        throw new IOException(e);
      }
    }
    return allFilesInFolder(destination, namespaceUrn, userId);
  }

  /** Unzip to defined directory. */
  public static void unzip(String source, String destination) throws IOException {
    try {
      ZipFile zipFile = new ZipFile(source);
      zipFile.extractAll(destination);
    } catch (IOException e) {
      throw new IOException(e);
    }
  }

  /** list all files inside a defined directory. */
  public static Future<String> allFilesInFolder(String folder, String namespaceUrn, int userId) throws Exception {
    Future<String> response = null;
    File inputFolder = new File(folder);
    File[] listOfFiles = inputFolder.listFiles();
    for (File file : Objects.requireNonNull(listOfFiles)) {
      if (file.isFile() && !file.getName().contains(".zip")) {
        response = importType(file.getAbsolutePath(), namespaceUrn, userId);
      }
    }
    return response;
  }

  /** detect file type (xml/json) then handle importing it. */
  public static Future<String> importType(String fileToImport, String namespaceUrn, int userId)
      throws Exception {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      try {
        if (fileToImport.contains(".xml")) {
          return importXml(ctx, fileToImport, namespaceUrn, userId);
        } else if (fileToImport.contains(".json")) {
          return importJson(ctx, fileToImport, namespaceUrn, userId);
        }
      } catch (JAXBException | IOException e) {
        throw new Exception(e);
      }
    }
    System.out.println(fileToImport);
    throw new IllegalArgumentException("Only XML and JSON files can be imported!");
  }

  /** handles importing xml file. */
  public static Future<String> importXml(
          CloseableDSLContext ctx, String fileToImport, String namespaceUrn, int userId) throws Exception {
    File file = new File(fileToImport);
    validateAgainstSchema(fileToImport);
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    ImportDto importDtoBody = (ImportDto) jaxbUnmarshaller.unmarshal(file);
    return saveElements(ctx, namespaceUrn, userId, importDtoBody.getStagedElements());
  }

  /** handles importing json file. */
  public static Future<String> importJson(
          CloseableDSLContext ctx, String fileToImport, String namespaceUrn, int userId) throws Exception {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    validateAgainstSchema(fileToImport);
    JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, "application/json");
    jaxbUnmarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    StreamSource json = new StreamSource(new StringReader(readFileAsString(fileToImport)));
    ImportDto importDtoBody = jaxbUnmarshaller.unmarshal(json, ImportDto.class).getValue();
    return saveElements(ctx, namespaceUrn, userId, importDtoBody.getStagedElements());
  }

  /** Convert stagedElements to elements and save them. */
  public static Future<String> saveElements(
      CloseableDSLContext ctx, String namespaceUrn, int userId, List<StagedElement> stagedElements) {
      stagedElements
      .stream()
      .sorted(Comparator.comparingInt(se -> elementTypeOrder(se.getIdentification().getElementType())))
      .forEachOrdered(
              (stagedElement -> {
                try {
                  String urn = StagedElementHandler.stagedElementToElement(ctx, stagedElement, namespaceUrn, userId);
                  urnDict.put(stagedElement.getIdentification().getUrn(), urn);
                } catch (Exception e) {
                  unImportableUrns.add(stagedElement.getIdentification().getUrn());
                }
              }));
    return new AsyncResult<String>("DONE");
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
      try {
        File schemaFile =
            new File(
                System.getProperty("user.dir")
                    + "/src/main/resources/schema/StagingImport_updated.xsd");
        Source xmlFile = new StreamSource(new File(fileToValidate));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
      } catch (SAXException | IOException e) {
        e.printStackTrace();
      }
    } else if (fileToValidate.contains(".json")) {
      try {
        JSONObject jsonSchema =
            new JSONObject(
                new JSONTokener(
                    new FileInputStream(
                        System.getProperty("user.dir") + "/src/main/resources/schema/StagingImport.json")));
        JSONObject jsonSubject = new JSONObject(new JSONTokener(new FileInputStream(fileToValidate)));
        org.everit.json.schema.Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);
      } catch (Exception e) {
        throw new Exception(e);
      }
    }
  }
}
