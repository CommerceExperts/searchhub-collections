package io.searchhub.mph.jackson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.searchhub.mph.MPHStringMap;
import io.searchhub.mph.PackageVersion;

public class MPHJacksonModule extends SimpleModule {

	public MPHJacksonModule() {
		super(PackageVersion.VERSION);
		super.addSerializer(MPHStringMap.class, new MPHStringMapSerializer());
		super.addDeserializer(MPHStringMap.class, new MPHStringMapDeserializer());

		super.setMixInAnnotation(Map.class, MapAnnotations.class);
		super.setMixInAnnotation(MPHStringMap.SerializableData.class, SerializableDataAnnotations.class);
	}

	@JsonTypeInfo(
			use = JsonTypeInfo.Id.CLASS,
			property = "type",
			defaultImpl = LinkedHashMap.class)
	public interface MapAnnotations {

	}


	public static abstract class SerializableDataAnnotations<V> {
		@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
		protected List<V> values;
	}
}
