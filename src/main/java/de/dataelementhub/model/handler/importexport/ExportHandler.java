package de.dataelementhub.model.handler.importexport;

import de.dataelementhub.model.dto.importexport.ExportDto;
import de.dataelementhub.model.dto.importexport.ImportDto;
import de.dataelementhub.model.dto.importexport.StagedElement;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.persistence.internal.oxm.NamespacePrefixMapper;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static de.dataelementhub.model.handler.importexport.StagedElementHandler.elementsToStagedElements;

public class ExportHandler {

  public static List<String> nonExportable = new ArrayList<>();

  /** Export defined Elements as Xml or Json. */
  public static Future<String> export(
          ExportDto exportDto, int userId, String format, Boolean fullExport, String timestamp, String exportDirectory) throws Exception {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    String destination = exportDirectory + userId + "/" + timestamp + "-" + format + "-processing";
    new File(System.getProperty("user.dir") + "/uploads/export").mkdir();
    new File(exportDirectory + userId).mkdir();
    new File(destination).mkdir();
    nonExportable.clear();
    ImportDto importDto = new ImportDto();
    importDto.setLabel(exportDto.getExport());
    List<StagedElement> stagedElements = elementsToStagedElements(exportDto.getElements(), userId, fullExport);
    List<String> Urns = stagedElements.stream().map(se -> se.getIdentification().getUrn()).collect(
        Collectors.toList());
    File exportedElements = new File(destination + "/exportedElements.txt");
    importDto.setStagedElements(stagedElements);
    Future<String> response;
    switch (format.toUpperCase()) {
      case "XML":
        response = exportXml(userId, importDto, timestamp, destination);
        Files.write(exportedElements.toPath(), Urns, Charset.defaultCharset());
        return response;
      case "JSON":
        response = exportJson(userId, importDto, timestamp, destination);
        Files.write(exportedElements.toPath(), Urns, Charset.defaultCharset());
        return response;
      default:
        throw new IllegalArgumentException("Format " + format + " is not defined!");
    }
  }

  /** Process Xml Exports. */
  public static Future<String> exportXml(int userId, ImportDto importDto, String timestamp, String destination) throws Exception {
    try {
      File file = new File(destination + "/file.xml");
      JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      NamespacePrefixMapper mapper =
          new NamespacePrefixMapper() {
            public String getPreferredPrefix(
                String namespaceUri, String suggestion, boolean requirePrefix) {
              return "ns2";
            }
          };
      jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
      jaxbMarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, "application/xml");
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.marshal(importDto, file);
      FileHandler.zip(file.getParent(), file.getParent() + "/" + timestamp + ".zip", true);
      return new AsyncResult<>(file.getParent());
    } catch (JAXBException | IOException e) {
      throw new Exception(e);
    }
  }

  /** Process Json Exports. */
  public static Future<String> exportJson(
      int userId, ImportDto importDtoBody, String timestamp, String destination) throws Exception {
    try {
      File file = new File(destination + "/file.json");
      JAXBContext jaxbContext = JAXBContext.newInstance(ImportDto.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      NamespacePrefixMapper mapper =
          new NamespacePrefixMapper() {
            public String getPreferredPrefix(
                String namespaceUri, String suggestion, boolean requirePrefix) {
              return "ns2";
            }
          };
      jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
      jaxbMarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, "application/json");
      jaxbMarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.marshal(importDtoBody, file);
      FileHandler.zip(file.getParent(), file.getParent() + "/" + timestamp + ".zip", false);
      return new AsyncResult<>(file.getParent());
    } catch (JAXBException | IOException e) {
      throw new Exception(e);
    }
  }
}
