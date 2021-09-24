package de.dataelementhub.model.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.dataelementhub.model.dto.element.section.ValueDomain;
import de.dataelementhub.model.dto.element.section.validation.Numeric;
import de.dataelementhub.model.dto.element.section.validation.NumericFloat;
import de.dataelementhub.model.dto.element.section.validation.NumericInteger;
import java.io.IOException;

/**
 * TypeAdapter Class to handle Numeric DTO and its extensions.
 * When reading a numeric object, check the type attribute and parse the JSON Object
 * with the proper numeric class (float or int).
 */
public class NumericValidationAdapter extends TypeAdapter<Numeric> {

  private static Gson GSON;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.disableHtmlEscaping();
    GSON = gsonBuilder.create();
  }

  public NumericValidationAdapter() {}

  /**
   * Writes one JSON value (an array, object, string, number, boolean or null) for validation.
   *
   * @param jsonWriter the JsonWriter to use
   * @param numeric the Numeric object to write
   */
  @Override
  public void write(JsonWriter jsonWriter, Numeric numeric) throws IOException {
    if (numeric == null) {
      jsonWriter.nullValue();
      return;
    }
    jsonWriter.jsonValue(GSON.toJson(numeric));
  }

  /**
   * Reads one JSON value (an array, object, string, number, boolean or null) and converts it to a
   * Numeric object. Returns the converted object.
   *
   * @param jsonReader the JsonReader to use
   * @return the converted Numeric object. May be null.
   */
  @Override
  public Numeric read(JsonReader jsonReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.nextNull();
      return null;
    } else {
      JsonElement jsonElement = readJsonElement(jsonReader);
      JsonObject jsonObject = jsonElement.getAsJsonObject();
      String type = jsonObject.getAsJsonPrimitive("type").getAsString();

      if (type == null || type.isEmpty()) {
        return null;
      } else {
        if (type.equalsIgnoreCase(Numeric.TYPE_FLOAT)) {
          return GSON.fromJson(jsonObject, NumericFloat.class);
        } else if (type.equalsIgnoreCase(Numeric.TYPE_INTEGER)) {
          return GSON.fromJson(jsonObject, NumericInteger.class);
        } else {
          throw new IllegalArgumentException("Illegal type: " + type);
        }
      }
    }
  }

  /**
   * Read the next element from a json reader.
   *
   * @param in the jsonreader to read from
   * @return The next element - either a jsonprimitive, jsonobject or jsonarray
   */
  private JsonElement readJsonElement(JsonReader in) throws IOException {
    switch (in.peek()) {
      case NUMBER:
        return new JsonPrimitive(new LazilyParsedNumber(in.nextString()));
      case BOOLEAN:
        return new JsonPrimitive(in.nextBoolean());
      case STRING:
        return new JsonPrimitive(in.nextString());
      case NULL:
        in.nextNull();
        return JsonNull.INSTANCE;
      case BEGIN_ARRAY:
        JsonArray array = new JsonArray();
        in.beginArray();
        while (in.hasNext()) {
          array.add(readJsonElement(in));
        }
        in.endArray();
        return array;
      case BEGIN_OBJECT:
        JsonObject object = new JsonObject();
        in.beginObject();
        while (in.hasNext()) {
          object.add(in.nextName(), readJsonElement(in));
        }
        in.endObject();
        return object;
      default:
        throw new IllegalArgumentException();
    }
  }
}
