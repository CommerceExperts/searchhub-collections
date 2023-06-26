package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.searchhub.mph.MPHStringMap;

import java.io.IOException;

public class MPHStringMapDeserializer extends StdDeserializer<MPHStringMap> {

	protected MPHStringMapDeserializer() {
		super(MPHStringMap.class);
	}

	@Override
	public MPHStringMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
		ObjectCodec oc = jsonParser.getCodec();
		JsonNode node = oc.readTree(jsonParser);
		try (JsonParser dataParser = node.findValue("data").traverse()) {
			dataParser.setCodec(jsonParser.getCodec());
			MPHStringMap.SerializableData<?> deserializedData = dataParser.readValueAs(MPHStringMap.SerializableData.class);
			return MPHStringMap.fromData(deserializedData);
		}
	}

}
