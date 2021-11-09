package de.dataelementhub.model.service;

import de.dataelementhub.model.dto.importexport.ExportDto;
import de.dataelementhub.model.handler.importexport.ExportHandler;
import de.dataelementhub.model.handler.importexport.ImportHandler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Future;

@Service
public class ImportExportService {

  Hashtable<String, String> importExportStatus = new Hashtable<>();

  /** Generates an Export file for defined elements. */
  @Async
  public void exportService(
          ExportDto exportDto, int userId, String format, Boolean fullExport, String timestamp) throws Exception {
    importExportStatus.put(userId + ":" + timestamp, "PROCESSING");
    Future<String> response = ExportHandler.export(exportDto, userId, format, fullExport, timestamp);
    importExportStatus.put(userId + ":" + timestamp, response.get());
  }

  /** Accept zip file and import its content (xml/json). */
  @Async
  public void importService(
        List<MultipartFile> file, String namespaceUrn, String importDirectory, int userId, String timestamp)
          throws Exception {
    importExportStatus.put(userId + ":" + timestamp, "PROCESSING");
    Future<String> response = ImportHandler.importFiles(file, namespaceUrn, importDirectory, userId, timestamp);
    importExportStatus.put(userId + ":" + timestamp, response.get());
  }

  /** returns the import/Export status PROCESSING/DONE/NOT DEFINED. */
  public String checkStatus(String identifier, int userId) {
    System.out.println(importExportStatus);
    System.out.println(userId + ":" + identifier);
    if (!importExportStatus.containsKey(userId + ":" + identifier)) {
      return "NOT DEFINED";
    }
    return importExportStatus.get(userId + ":" + identifier);
  }
}
