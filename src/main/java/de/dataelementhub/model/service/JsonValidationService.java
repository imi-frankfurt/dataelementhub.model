package de.dataelementhub.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * JSON Validation Service.
 */
@Service
public class JsonValidationService {

  public static final String DEHUB_ELEMENT_VALIDATION_JSON = "schema/dehubElementValidation.json";
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * converts jsonString to jsonNode.
   */
  protected JsonNode getJsonNodeFromStringContent(String content) throws IOException {
    return mapper.readTree(content);
  }

  /**
   * reads DRAFT_7 jsonSchema from Classpath.
   */
  protected JsonSchema getJsonSchemaFromClasspath(String name) {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    InputStream is = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(name);
    return factory.getSchema(is);
  }

  /**
   * validates a given jsonString against dehubElementValidation.json.
   */
  public void validate(String jsonNodeToValidate) throws IOException {
    JsonSchema schema = getJsonSchemaFromClasspath(DEHUB_ELEMENT_VALIDATION_JSON);
    JsonNode node = getJsonNodeFromStringContent(jsonNodeToValidate);
    Set<ValidationMessage> errors = schema.validate(node);
    StringBuilder errorsCombined = new StringBuilder();
    for (ValidationMessage error : errors) {
      errorsCombined.append(error.toString()).append("\n");
    }
    if (!errorsCombined.toString().equals("")) {
      throw new IOException(
          "The JSON object you submitted did not pass validation.\n" + errorsCombined);
    }
  }

}
