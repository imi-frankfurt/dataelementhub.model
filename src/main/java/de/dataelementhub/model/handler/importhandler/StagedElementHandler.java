package de.dataelementhub.model.handler.importhandler;

import static de.dataelementhub.dal.jooq.Tables.ELEMENT;
import static de.dataelementhub.dal.jooq.Tables.STAGING;

import de.dataelementhub.model.dto.element.StagedElement;
import java.util.List;
import java.util.concurrent.Future;
import org.jooq.CloseableDSLContext;
import org.springframework.scheduling.annotation.AsyncResult;

public class StagedElementHandler {

//  /** . */
//  public static void stageElement(CloseableDSLContext ctx, String namespaceUrn,
//      int userId, List<StagedElement> stagedElements) {
//    return ctx.insertInto(STAGING)
//        .set(ctx.newRecord(ELEMENT, dataElement))
//        .returning(ELEMENT.ID)
//        .fetchOne().getId();
//  }
}
