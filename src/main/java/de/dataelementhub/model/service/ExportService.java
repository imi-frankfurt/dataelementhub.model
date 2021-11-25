package de.dataelementhub.model.service;

import de.dataelementhub.model.dto.export.ExportInfo;
import de.dataelementhub.model.dto.export.ExportRequest;
import de.dataelementhub.model.handler.export.ExportHandler;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

  /** Generates an Export file for defined elements. */
  @Async
  public void exportService(ExportRequest exportRequest, int userId, String format,
      Boolean fullExport, String timestamp, String exportDirectory) {
    ExportHandler.export(exportRequest, userId, format, fullExport, timestamp, exportDirectory);
  }

  /** returns the import/Export status PROCESSING/DONE/INTERRUPTED/NOT DEFINED.
   **/
  public ExportInfo exportInfo(String identifier, int userId, String type) {
    File[] exports = Objects.requireNonNull(new File(System.getProperty("user.dir")
        + "/uploads/" + type + "/" + userId).listFiles(File::isDirectory));
    ExportInfo exportInfo = new ExportInfo();
    exportInfo.setStatus("NOT DEFINED");
    for (File export : exports) {
      if (export.getName().split("-")[0].equals(identifier)) {
        String[] itemParts = export.getName().split("-");
        String[] tsp = itemParts[0].split("_");
        Timestamp timestamp = Timestamp.valueOf(tsp[0] + "-" + tsp[1] + "-" + tsp[2]
            + " " + tsp[3] + ":" + tsp[4] + ":" + tsp[5] + "." + tsp[6]);
        exportInfo.setId(itemParts[0]);
        exportInfo.setStatus(itemParts[2].toUpperCase());
        exportInfo.setTimestamp(timestamp);
        exportInfo.setFormat(itemParts[1].toUpperCase());
        exportInfo.setProgress(itemParts[2].equalsIgnoreCase("DONE")
            ? 1 : ExportHandler.exportProgress);
      }
    }
    return exportInfo;
  }

  /** Returns all Exports. */
  public List<ExportInfo> allExports(int userId, String exportDirectory) {
    File inputFolder = new File(exportDirectory + "/" + userId);
    List<ExportInfo> exportDescriptions = new ArrayList<>();
    List<String> listOfFiles = Arrays.stream(inputFolder.listFiles()).map(File::getName).collect(
        Collectors.toList());
    for (String item: listOfFiles) {
      String[] itemParts = item.split("-");
      String[] tsp = itemParts[0].split("_");
      Timestamp timestamp = Timestamp.valueOf(tsp[0] + "-" + tsp[1] + "-" + tsp[2]
          + " " + tsp[3] + ":" + tsp[4] + ":" + tsp[5] + "." + tsp[6]);
      ExportInfo exportInfo = new ExportInfo();
      exportInfo.setId(itemParts[0]);
      exportInfo.setStatus(itemParts[2].toUpperCase());
      exportInfo.setTimestamp(timestamp);
      exportInfo.setFormat(itemParts[1].toUpperCase());
      exportDescriptions.add(exportInfo);
      exportInfo.setProgress(itemParts[2].equalsIgnoreCase("DONE")
          ? 1 : ExportHandler.exportProgress);
    }
    return exportDescriptions;
  }
}