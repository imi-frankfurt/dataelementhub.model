package de.dataelementhub.model.service;

import de.dataelementhub.dal.jooq.enums.SourceType;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import de.dataelementhub.model.handler.SourceHandler;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

/**
 * Source Service.
 */
@Service
public class SourceService {

  /**
   * Create a new Source and return its new ID.
   */
  public int create(DSLContext ctx, Source source) {
    return SourceHandler.create(ctx, source);
  }

  /**
   * Get a Source by its id.
   */
  public Source read(DSLContext ctx, int id) {
    return SourceHandler.getSourceById(ctx, id);
  }

  /**
   * Get a list of all Sources.
   */
  public List<Source> list(DSLContext ctx) {
    return SourceHandler.getSources(ctx);
  }

  /**
   * Get a list of all Sources of the provided type.
   */
  public List<Source> listByType(DSLContext ctx, SourceType sourceType) {
    return SourceHandler.getSourcesByType(ctx, sourceType);
  }

}
