package de.dataelementhub.model.handler.importexport;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CheckExpiredImportExport {

  /** Delete all Imports/Exports older than 7 Days. */
  @Scheduled(fixedRate = 1000 * 60 * 60)
  @PostConstruct
  public static void deleteFiles() throws IOException {
    Path destination = Paths.get(System.getProperty("user.dir") + "/uploads");
    int daysToKeep = 7;
    final Instant retentionFilePeriod = ZonedDateTime.now().minusDays(daysToKeep).toInstant();
    final AtomicInteger countDeletedFiles = new AtomicInteger();
    Files.find(
            destination,
            5,
        (path, basicFileAttrs) ->
                basicFileAttrs.creationTime().toInstant().isBefore(retentionFilePeriod))
        .forEach(
            fileToDelete -> {
              try {
                if (!Files.isDirectory(fileToDelete)) {
                  Files.delete(fileToDelete);
                  countDeletedFiles.incrementAndGet();
                }
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
    countDeletedFiles.get();
  }
}
