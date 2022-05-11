package de.dataelementhub.model.handler;

import static de.dataelementhub.dal.jooq.Tables.SOURCE;

import de.dataelementhub.dal.jooq.enums.SourceType;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import de.dataelementhub.dal.jooq.tables.records.SourceRecord;
import java.util.List;
import org.jooq.CloseableDSLContext;

/**
 * Source Handler.
 */
public class SourceHandler {

  /**
   * Get the source object for the local dataelementhub.
   */
  public static Source getLocalDeHubSource(CloseableDSLContext ctx, int userId) {
    return ctx.selectFrom(SOURCE)
        .where(SOURCE.NAME.eq("local"))
        .and(SOURCE.PREFIX.eq("local"))
        .and(SOURCE.BASE_URL.eq("local"))
        .and(SOURCE.TYPE.eq(SourceType.DATAELEMENTHUB))
        .fetchOneInto(Source.class);
  }

  /**
   * Returns all available sources.
   */
  public static List<Source> getSources(CloseableDSLContext ctx) {
    return
        ctx.selectFrom(SOURCE).fetchInto(Source.class);
  }

  /**
   * Returns source with provided id.
   */
  public static Source getSourceById(CloseableDSLContext ctx, int sourceId) {
    return
        ctx.selectFrom(SOURCE)
            .where(SOURCE.ID.eq(sourceId))
            .fetchOneInto(Source.class);
  }

  /**
   * Returns all available sources of the provided type.
   */
  public static List<Source> getSourcesByType(CloseableDSLContext ctx, SourceType sourceType) {
    return
        ctx.selectFrom(SOURCE)
            .where(SOURCE.TYPE.eq(sourceType))
            .fetchInto(Source.class);
  }

  /**
   * Insert a new source and return the id.
   */
  public static int create(CloseableDSLContext ctx, Source source) {
    SourceRecord sourceRecord = ctx.newRecord(SOURCE, source);
    sourceRecord.store();
    sourceRecord.refresh();
    return sourceRecord.getId();
  }
}
