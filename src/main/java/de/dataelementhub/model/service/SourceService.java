package de.dataelementhub.model.service;

import de.dataelementhub.dal.ResourceManager;
import de.dataelementhub.dal.jooq.enums.SourceType;
import de.dataelementhub.dal.jooq.tables.pojos.Source;
import de.dataelementhub.model.handler.SourceHandler;
import java.util.List;
import org.jooq.CloseableDSLContext;
import org.springframework.stereotype.Service;

@Service
public class SourceService {

  /**
   * Create a new Source and return its new ID.
   */
  public int create(Source source) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return SourceHandler.create(ctx, source);
    }
  }

  /**
   * Get a Source by its id.
   */
  public Source read(int id) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return SourceHandler.getSourceById(ctx, id);
    }
  }

  /**
   * Get a list of all Sources.
   */
  public List<Source> list() {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return SourceHandler.getSources(ctx);
    }
  }

  /**
   * Get a list of all Sources of the provided type.
   */
  public List<Source> listByType(SourceType sourceType) {
    try (CloseableDSLContext ctx = ResourceManager.getDslContext()) {
      return SourceHandler.getSourcesByType(ctx, sourceType);
    }
  }

}
