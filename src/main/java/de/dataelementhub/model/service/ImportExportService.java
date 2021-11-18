package de.dataelementhub.model.service;

import de.dataelementhub.model.dto.importexport.ExportDescription;
import de.dataelementhub.model.dto.importexport.ExportDto;
import de.dataelementhub.model.dto.importexport.ImportDescription;
import de.dataelementhub.model.handler.importexport.ExportHandler;
import de.dataelementhub.model.handler.importexport.FileHandler;
import de.dataelementhub.model.handler.importexport.ImportHandler;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Future;

@Service
public class ImportExportService {

  /** Generates an Export file for defined elements. */
  @Async
  public void exportService(ExportDto exportDto, int userId, String format, Boolean fullExport,
      String timestamp, String exportDirectory) throws Exception {
    Future<String> response =
        ExportHandler.export(exportDto, userId, format, fullExport, timestamp, exportDirectory);
    File processedFile = new File(String.valueOf(response.get()));
    File newFile =
        new File(String.valueOf(response.get()).replace("-processing", "-done"));
    processedFile.renameTo(newFile);
  }

  /** Accept zip file and import its content (xml/json). */
  @Async
  public void importService(
        List<MultipartFile> file, String namespaceUrn, String importDirectory,
      int userId, String timestamp) {
    ImportHandler.importFiles(file, namespaceUrn, importDirectory,
        userId, timestamp);
  }

  /** returns the import/Export status PROCESSING/DONE/INTERRUPTED/NOT DEFINED. */
  public String checkStatus(String identifier, int userId, String type) {
    File[] exports = Objects.requireNonNull(new File(System.getProperty("user.dir")
        + "/uploads/" + type + "/" + userId).listFiles(File::isDirectory));
    for (File export : exports) {
      if (export.toString().contains(identifier)) {
        if (export.toString().contains("processing")) {
          return "PROCESSING";
        } else {
          return "DONE";
        }
      }
    }
    return "NOT DEFINED";
  }

  /** Returns all Imports. */
  public List<ImportDescription> allImports(int userId, String importDirectory) {
    File inputFolder = new File(importDirectory + "/" + userId);
    List<ImportDescription> importDescriptions = new ArrayList<>();
    List<String> listOfFiles = Arrays.stream(inputFolder.listFiles()).map(File::getName).collect(
        Collectors.toList());
    for (String item: listOfFiles) {
      List<String> itemParts = List.of(item.split("-"));
      List<String> tsp = List.of(itemParts.get(0).split("_"));
      Timestamp timestamp = Timestamp.valueOf(tsp.get(0) + "-" + tsp.get(1) + "-" + tsp.get(2)
          + " " + tsp.get(3) + ":" + tsp.get(4) + ":" + tsp.get(5) + "." + tsp.get(6));
      ImportDescription importDescription = new ImportDescription();
      importDescription.setId(itemParts.get(0));
      importDescription.setStatus(itemParts.get(2).toUpperCase());
      importDescription.setTimestamp(timestamp);
      importDescription.setNamespaceUrn(itemParts.get(1).toUpperCase()
          .replace("_", ":"));
      importDescriptions.add(importDescription);
    }
    return importDescriptions;
  }

  /** Returns all Exports. */
  public List<ExportDescription> allExports(int userId, String exportDirectory) {
    File inputFolder = new File(exportDirectory + "/" + userId);
    List<ExportDescription> exportDescriptions = new ArrayList<>();
    List<String> listOfFiles = Arrays.stream(inputFolder.listFiles()).map(File::getName).collect(
        Collectors.toList());
    for (String item: listOfFiles) {
      List<String> itemParts = List.of(item.split("-"));
      List<String> tsp = List.of(itemParts.get(0).split("_"));
      Timestamp timestamp = Timestamp.valueOf(tsp.get(0) + "-" + tsp.get(1) + "-" + tsp.get(2)
          + " " + tsp.get(3) + ":" + tsp.get(4) + ":" + tsp.get(5) + "." + tsp.get(6));
      ExportDescription exportDescription = new ExportDescription();
      exportDescription.setId(itemParts.get(0));
      exportDescription.setStatus(itemParts.get(2).toUpperCase());
      exportDescription.setTimestamp(timestamp);
      exportDescription.setFormat(itemParts.get(1).toUpperCase());
      exportDescriptions.add(exportDescription);
    }
    return exportDescriptions;
  }
}
