package de.dataelementhub.model.service;

import de.dataelementhub.model.dto.importexport.ExportInfo;
import de.dataelementhub.model.dto.importexport.ExportRequest;
import de.dataelementhub.model.handler.export.ExportHandler;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Export Service.
 */
@Service
public class ExportService {

  @Value("${dehub.export.expirationPeriodInDays}")
  public int expirationPeriodInDays;

  @Value("${dehub.export.exportDirectory}")
  private String defaultExportDirectory;

  /**
   * Get predefined export expiration period.
   */
  public int getExpirationPeriodInDays() {
    return expirationPeriodInDays;
  }

  /** Generates an Export file for defined elements. */
  @Async
  public void exportService(DSLContext ctx, ExportRequest exportRequest,
      int userId, MediaType mediaType,
      Boolean fullExport, String timestamp, String exportDirectory) {
    ExportHandler.export(
        ctx, exportRequest, userId, mediaType, fullExport, timestamp, exportDirectory);
  }

  /** returns the import/Export status PROCESSING/DONE/INTERRUPTED/NOT DEFINED.
   **/
  public ExportInfo exportInfo(String identifier, int userId, String exportDirectory) {
    new File(exportDirectory + File.separator + userId).mkdir();
    File[] exports = Objects.requireNonNull(new File(exportDirectory + File.separator + userId)
        .listFiles(File::isDirectory));
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
        exportInfo.setMediaType(MediaType.parseMediaType("application/" + itemParts[1]));
        exportInfo.setProgress(itemParts[2].equalsIgnoreCase("DONE")
            ? 1 : ExportHandler.exportProgress);
      }
    }
    return exportInfo;
  }

  /** Returns all Exports. */
  public List<ExportInfo> allExports(int userId, String exportDirectory) {
    new File(exportDirectory + File.separator + userId).mkdir();
    File inputFolder = new File(exportDirectory + File.separator + userId);
    List<ExportInfo> exportDescriptions = new ArrayList<>();
    List<String> listOfFiles = Arrays.stream(inputFolder.listFiles()).map(File::getName).collect(
        Collectors.toList());
    for (String item : listOfFiles) {
      String[] itemParts = item.split("-");
      String[] tsp = itemParts[0].split("_");
      Timestamp timestamp = Timestamp.valueOf(tsp[0] + "-" + tsp[1] + "-" + tsp[2]
          + " " + tsp[3] + ":" + tsp[4] + ":" + tsp[5] + "." + tsp[6]);
      ExportInfo exportInfo = new ExportInfo();
      exportInfo.setId(itemParts[0]);
      exportInfo.setStatus(itemParts[2].toUpperCase());
      exportInfo.setTimestamp(timestamp);
      exportInfo.setMediaType(MediaType.parseMediaType("application/" + itemParts[1]));
      exportDescriptions.add(exportInfo);
      exportInfo.setProgress(itemParts[2].equalsIgnoreCase("DONE")
          ? 1 : ExportHandler.exportProgress);
    }
    return exportDescriptions;
  }

  /**
   * Get export directory.
   */
  public String getExportDirectory() {
    if (defaultExportDirectory == null) {
      return System.getProperty("java.io.tmpdir")
          + "/exports".replace('/', File.separatorChar);
    }
    return defaultExportDirectory;
  }
}
