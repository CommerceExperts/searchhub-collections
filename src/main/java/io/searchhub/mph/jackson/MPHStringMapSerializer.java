package io.searchhub.mph.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.searchhub.mph.MPHStringMap;

import java.io.IOException;

public class MPHStringMapSerializer extends StdSerializer<MPHStringMap> {

	protected MPHStringMapSerializer() {
		super(MPHStringMap.class);
	}

	@Override
	public Class<MPHStringMap> handledType() {
		return MPHStringMap.class;
	}

	@Override
	public void serialize(MPHStringMap mphStringMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
		jsonGenerator.writeObject(mphStringMap.getSerializableMphMapData());
	}

	@Override
	public void serializeWithType(MPHStringMap value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
		// [databind#631]: Assign current value, to be accessible by custom serializers
		gen.setCurrentValue(value);
		WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));

		MPHStringMap.SerializableData mphData = value.getSerializableMphMapData();
		gen.writeObjectField("mphFunctionData", mphData.getMphFunctionData());
		gen.writeObjectField("avgBucketSize", mphData.getAvgBucketSize());
		gen.writeObjectField("leafSize", mphData.getLeafSize());
		gen.writeObjectField("keyValueMap", mphData.getKeyValueMap());
		gen.writeObjectField("values", mphData.getValues());
		typeSer.writeTypeSuffix(gen, typeIdDef);

	}
}
