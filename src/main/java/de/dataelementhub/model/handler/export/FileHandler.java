package de.dataelementhub.model.handler.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.tomcat.util.http.fileupload.FileUtils;

/**
 * File Handler.
 */
public class FileHandler {

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
