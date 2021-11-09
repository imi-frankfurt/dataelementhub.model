package de.dataelementhub.model.handler.importexport;

import de.dataelementhub.model.dto.importexport.ExportDto;
import de.dataelementhub.model.dto.importexport.ImportDto;
import de.dataelementhub.model.dto.importexport.StagedElement;
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
          ExportDto exportDto, int userId, String format, Boolean fullExport, String timestamp) throws Exception {
    System.setProperty(
        "javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
    new File(System.getProperty("user.dir") + "/uploads/export").mkdir();
    new File(System.getProperty("user.dir") + "/uploads/export/" + userId).mkdir();
    nonExportable.clear();
    ImportDto importDto = new ImportDto();
    importDto.setLabel(exportDto.getExport());
    List<StagedElement> stagedElements = elementsToStagedElements(exportDto.getElements(), userId, fullExport);
    importDto.setStagedElements(stagedElements);
    Future<String> response;
    switch (format.toUpperCase()) {
      case "XML":
        response = exportXml(userId, importDto, timestamp);
        return response;
      case "JSON":
        response = exportJson(userId, importDto, timestamp);
        return response;
      default:
        throw new IllegalArgumentException("Format " + format + " is not defined!");
    }
  }

  /** Process Xml Exports. */
  public static Future<String> exportXml(int userId, ImportDto importDto, String timestamp) throws Exception {
    try {
      File file = new File(System.getProperty("user.dir") + "/uploads/export/" + userId + "/file.xml");
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
      zip(file.getParent(), file.getParent() + "/" + timestamp + "_export.zip", true);
      return new AsyncResult<>("DONE");
    } catch (JAXBException | IOException e) {
      throw new Exception(e);
    }
  }

  /** Process Json Exports. */
  public static Future<String> exportJson(
      int userId, ImportDto importDtoBody, String timestamp) throws Exception {
    try {
      File file = new File(System.getProperty("user.dir") + "/uploads/export/" + userId + "/file.json");
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
      zip(file.getParent(), file.getParent() + "/" + timestamp + "_export.zip", false);
      return new AsyncResult<>("DONE");
    } catch (JAXBException | IOException e) {
      throw new Exception(e);
    }
  }

  /** returns a list of Json/Xml files in a Directory. */
  public static List<String> allFilesInFolder(String folder, Boolean xmlExport) {
    File inputFolder = new File(folder);
    File[] listOfFiles = inputFolder.listFiles();
    List<String> filePaths = new ArrayList<>();
    assert listOfFiles != null;
    if (xmlExport) {
      for (File file : listOfFiles) {
        if (file.isFile() & file.getName().contains(".xml")) {
          filePaths.add(file.getAbsolutePath());
        }
      }
    } else {
      for (File file : listOfFiles) {
        if (file.isFile() & file.getName().contains(".json")) {
          filePaths.add(file.getAbsolutePath());
        }
      }
    }
    return filePaths;
  }

  /** Zip Json/Xml files. */
  public static void zip(String source, String destination, Boolean xmlExport) throws IOException {
    List<String> filePaths = allFilesInFolder(source, xmlExport);
    ZipFile zipFile = new ZipFile(destination);
    ZipParameters zipParameters = new ZipParameters();
    for (String path : filePaths) {
      try {
        zipFile.addFile(new File(path), zipParameters);
        FileUtils.forceDelete(new File(path));
      } catch (IOException e) {
        throw new IOException(e);
      }
    }
  }
}
