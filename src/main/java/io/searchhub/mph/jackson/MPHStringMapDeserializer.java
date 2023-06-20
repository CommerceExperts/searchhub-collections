package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.type.TypeBase;
import com.fasterxml.jackson.databind.type.TypeBindings;
import io.searchhub.mph.MPHStringMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MPHStringMapDeserializer extends StdDeserializer<MPHStringMap> {

	protected MPHStringMapDeserializer() {
		super(MPHStringMap.class);
	}

	@Override
	public MPHStringMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
		MPHStringMap.SerializableData<?> deserializedData = jsonParser.readValueAs(MPHStringMap.SerializableData.class);
		return MPHStringMap.fromData(deserializedData);
	}

}
