package de.dataelementhub.model.handler.export;

import static de.dataelementhub.model.handler.export.StagedElementHandler.elementsToStagedElements;

import de.dataelementhub.model.dto.element.StagedElement;
import de.dataelementhub.model.dto.export.Export;
import de.dataelementhub.model.dto.export.ExportRequest;
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
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.AsyncResult;

public class ExportHandler {

  public static final String JAVAX_XML_BIND_CONTEXT_FACTORY = "javax.xml.bind.context.factory";
  public static final String JAVAX_XML_BIND_CONTEXT_FACTORY_VALUE =
      "org.eclipse.persistence.jaxb.JAXBContextFactory";
  public static final String EXPORTED_ELEMENTS_FILENAME = "exportedElements.txt";
  public static final String SUFFIX_PROCESSING = "-processing";
  public static final String SUFFIX_DONE = "-done";
  public static final String SUFFIX_INTERRUPTED = "-interrupted";
  public static final String NAMESPACE_PREFIX_MAPPER = "com.sun.xml.bind.namespacePrefixMapper";
  public static List<String> nonExportable = new ArrayList<>();
  public static float exportProgress = 0;

  /** Export defined Elements as Xml or Json. */
  public static void export(
          ExportRequest exportRequest, int userId, MediaType mediaType, Boolean fullExport,
      String timestamp, String exportDirectory) {
    System.setProperty(JAVAX_XML_BIND_CONTEXT_FACTORY, JAVAX_XML_BIND_CONTEXT_FACTORY_VALUE);
    String destination = exportDirectory + File.separator + userId + File.separator + timestamp
        + "-" + mediaType.getSubtype() + SUFFIX_PROCESSING;
    new File(exportDirectory + File.separator + userId).mkdir();
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
      File exportedElements = new File(destination + File.separator + EXPORTED_ELEMENTS_FILENAME);
      export.setStagedElements(stagedElements);
      Future<String> response = export(export, timestamp, destination, mediaType);
      Files.write(exportedElements.toPath(), urns, Charset.defaultCharset());
      File processedFile = new File(String.valueOf(response.get()));
      File newFile =
          new File(String.valueOf(response.get()).replace(SUFFIX_PROCESSING, SUFFIX_DONE));
      processedFile.renameTo(newFile);
    } catch (Exception e) {
      File processedFile = new File(destination);
      File newFile =
          new File(destination.replace(SUFFIX_PROCESSING, SUFFIX_INTERRUPTED));
      processedFile.renameTo(newFile);
    }
  }

  /**
   * Process Exports.
   */
  public static Future<String> export(Export export, String timestamp, String destination,
      MediaType mediaType)
      throws Exception {
    // Only support xml and json at the moment
    if (!(mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_XML)
        || mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON))) {
      throw new IllegalArgumentException("Unsupported media type: " + mediaType);
    }
    File file = new File(destination + File.separator + "file." + mediaType.getSubtype());
    JAXBContext jaxbContext = JAXBContext.newInstance(Export.class);
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    NamespacePrefixMapper mapper =
        new NamespacePrefixMapper() {
          public String getPreferredPrefix(
              String namespaceUri, String suggestion, boolean requirePrefix) {
            return "ns2";
          }
        };
    jaxbMarshaller.setProperty(NAMESPACE_PREFIX_MAPPER, mapper);
    jaxbMarshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, mediaType.toString());
    jaxbMarshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    jaxbMarshaller.marshal(export, file);
    FileHandler.zip(file.getParent(), file.getParent() + "/" + timestamp
        + ".zip", mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_XML));
    return new AsyncResult<>(file.getParent());
  }
}
