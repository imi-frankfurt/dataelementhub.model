package de.dataelementhub.model.handler.export;

import static de.dataelementhub.model.handler.export.StagedElementHandler.elementsToStagedElements;

import de.dataelementhub.model.dto.export.Export;
import de.dataelementhub.model.dto.export.ExportRequest;
import de.dataelementhub.model.dto.export.StagedElement;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.eclipse.persistence.internal.oxm.NamespacePrefixMapper;
import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.springframework.scheduling.annotation.AsyncResult;

public class ExportHandler {

  public static List<String> nonExportable = new ArrayList<>();
  public static float exportProgress = 0;

  /** Export defined Elements as Xml or Json. */
  public static void export(
          ExportRequest exportRequest, int userId, String format, Boolean fullExport,
      String timestamp, String exportDirectory) {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    String destination = exportDirectory + "/" + userId + "/" + timestamp
        + "-" + format + "-processing";
    new File(exportDirectory + "/" + userId).mkdir();
    new File(destination).mkdir();
    nonExportable.clear();
    try {
      Export export = new Export();
      export.setLabel(exportRequest.getLabel());
      List<StagedElement> stagedElements = elementsToStagedElements(exportRequest.getElementUrns(),
          userId, fullExport);
      List<String> urns = stagedElements.stream().map(se -> se.getIdentification().getUrn())
          .collect(
              Collectors.toList());
      File exportedElements = new File(destination + "/exportedElements.txt");
      export.setStagedElements(stagedElements);
      Future<String> response;
      switch (format.toUpperCase()) {
        case "XML":
          response = exportXml(export, timestamp, destination);
          Files.write(exportedElements.toPath(), urns, Charset.defaultCharset());
          break;
        case "JSON":
          response = exportJson(export, timestamp, destination);
          Files.write(exportedElements.toPath(), urns, Charset.defaultCharset());
          break;
        default:
          throw new IllegalArgumentException("Format " + format + " is not defined!");
      }
      File processedFile = new File(String.valueOf(response.get()));
      File newFile =
          new File(String.valueOf(response.get()).replace("-processing", "-done"));
      processedFile.renameTo(newFile);
    } catch (Exception e) {
      File processedFile = new File(destination);
      File newFile =
          new File(destination.replace("-processing", "-interrupted"));
      processedFile.renameTo(newFile);
    }
  }

  /** Process Xml Exports. */
  public static Future<String> exportXml(Export export, String timestamp, String destination)
      throws Exception {
    File file = new File(destination + "/file.xml");
    JAXBContext jaxbContext = JAXBContext.newInstance(Export.class);
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
    jaxbMarshaller.marshal(export, file);
    FileHandler.zip(file.getParent(), file.getParent() + "/" + timestamp
        + ".zip", true);
    return new AsyncResult<>(file.getParent());
  }

  /** Process Json Exports. */
  public static Future<String> exportJson(Export export, String timestamp, String destination)
      throws Exception {
    File file = new File(destination + "/file.json");
    JAXBContext jaxbContext = JAXBContext.newInstance(Export.class);
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
    jaxbMarshaller.marshal(export, file);
    FileHandler.zip(file.getParent(), file.getParent() + "/" + timestamp
        + ".zip", false);
    return new AsyncResult<>(file.getParent());
  }
}
