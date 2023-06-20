package io.searchhub.mph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.searchhub.mph.jackson.MPHJacksonModule;
import io.searchhub.mph.jackson.MPHStringMapDeserializer;
import io.searchhub.mph.jackson.MPHStringMapSerializer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonSerializationTest {

	Map<String, String> testData = new HashMap<>();
	private MPHStringMap<String> underTest;

	@BeforeEach
	public void setup() {
		Random random = new Random();
		for (int i = 1; i < 'Z'; i++) {
			String key = Character.toString(i) + " " + (i + random.nextInt());
			String value = "Value:" + (i + random.nextInt()) + Character.toString(i);
			testData.put(key, value);
		}
		underTest = MPHStringMap.build(testData);
	}

	@Test
	public void testMPHStringMap() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String serializedMPHData = mapper.writeValueAsString(underTest.getSerializableMphMapData());

		MPHStringMap.SerializableData<String> deserializedData = mapper.readValue(serializedMPHData, new TypeReference<>() {});
		MPHStringMap<String> deserializedMPH = MPHStringMap.fromData(deserializedData);

		for (Map.Entry<String, String> entry : testData.entrySet()) {
			assertEquals(underTest.get(entry.getKey()), deserializedMPH.get(entry.getKey()));

			assertNull(underTest.get(entry.getKey() + "troll"));
			assertNull(deserializedMPH.get(entry.getKey() + "troll"));
		}
	}

	@Test
	public void testAsTransparentMapImpl() throws JsonProcessingException {
		AnyDTO dto = new AnyDTO(underTest);

		ObjectMapper mapper = new ObjectMapper();
		// normally not necessary, since it's declared as a java-service-loader impl via META-INF/services
		mapper.registerModule(new MPHJacksonModule());

		String serializedMap = mapper.writeValueAsString(dto);
		System.out.println(serializedMap);
		AnyDTO dtoCopy = mapper.readValue(serializedMap, AnyDTO.class);

		for (Map.Entry<String, String> entry : testData.entrySet()) {
			assertEquals(dto.map.get(entry.getKey()), dtoCopy.map.get(entry.getKey()));

			assertNull(dto.map.get("any " + entry.getKey()));
			assertNull(dtoCopy.map.get("any " + entry.getKey()));
		}
	}

	@Test
	public void testDeserializationOfStandardMapImpl() throws JsonProcessingException {
		AnyDTO dto = new AnyDTO(testData);

		ObjectMapper mapper = new ObjectMapper();
		// make sure, module is used, which adds annotation to Map interface
		mapper.registerModule(new MPHJacksonModule());

		String serializedMap = mapper.writeValueAsString(dto);
		// remove type information
		serializedMap = serializedMap.replace("\"type\":\"java.util.HashMap\",", "");
		System.out.println(serializedMap);
		AnyDTO dtoCopy = mapper.readValue(serializedMap, AnyDTO.class);

		for (Map.Entry<String, String> entry : testData.entrySet()) {
			assertEquals(dto.map.get(entry.getKey()), dtoCopy.map.get(entry.getKey()));

			assertNull(dto.map.get("any " + entry.getKey()));
			assertNull(dtoCopy.map.get("any " + entry.getKey()));
		}
	}

	@Test
	public void testMapType() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = mapper.readValue("{\"foo\":\"bar\"}", new TypeReference<Map<String, String>>() {});
		System.out.println(map.getClass().getCanonicalName());
	}

	@NoArgsConstructor
	@AllArgsConstructor
	public static class AnyDTO {
		// works
		// @JsonDeserialize(using = MPHStringMapDeserializer.class)

		public Map<String, String> map;
	}
}
