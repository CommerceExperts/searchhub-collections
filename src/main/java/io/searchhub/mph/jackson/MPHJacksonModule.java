package io.searchhub.mph.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.type.MapType;
import io.searchhub.mph.MPHStringMap;
import io.searchhub.mph.PackageVersion;

import java.util.List;

public class MPHJacksonModule extends SimpleModule {

	public MPHJacksonModule() {
		super(PackageVersion.VERSION);
		super.addSerializer(MPHStringMap.class, new MPHStringMapSerializer());
		super.addDeserializer(MPHStringMap.class, new MPHStringMapDeserializer());

		super.setMixInAnnotation(MPHStringMap.SerializableData.class, SerializableDataAnnotations.class);
		super.setSerializerModifier(new BeanSerializerModifier() {

			@Override
			public JsonSerializer<?> modifyMapSerializer(SerializationConfig config, MapType valueType, BeanDescription beanDesc, JsonSerializer<?> mapSerializer) {
				// Return a delegating serializer for all but the MPHStringMapSerializer
				return mapSerializer instanceof MPHStringMapSerializer ? mapSerializer : new DelegatingMapSerializer(mapSerializer);
			}
		});
		super.setDeserializerModifier(new BeanDeserializerModifier() {
			@Override
			public JsonDeserializer<?> modifyMapDeserializer(DeserializationConfig config, MapType type, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
				return new DelegatingMapDeserializer(deserializer);
			}
		});
	}

	public static abstract class SerializableDataAnnotations<V> {

		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
		protected List<V> values;
	}
}
