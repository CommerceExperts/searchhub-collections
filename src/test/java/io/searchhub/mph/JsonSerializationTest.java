package io.searchhub.mph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchhub.mph.jackson.MPHJacksonModule;
import lombok.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

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
	public void testMPHMapWithPrimitiveValue() throws JsonProcessingException {
		Map<String, int[]> testData = Map.of("unit", new int[] { 1 }, "test", new int[] { 2 });
		MPHStringMap<int[]> underTest = MPHStringMap.build(testData);
		PredictDataWrapper dto = new PredictDataWrapper(new PredictData(underTest));

		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
		mapper.enable(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY);
		String serializedDto = mapper.writeValueAsString(dto);
		System.out.println(serializedDto);

		PredictDataWrapper deserializedDto = mapper.readValue(serializedDto, new TypeReference<>() {});

		for (Map.Entry<String, int[]> entry : testData.entrySet()) {
			assertArrayEquals(underTest.get(entry.getKey()), deserializedDto.getData().map.get(entry.getKey()));
		}
	}

	@Test
	public void testAsTransparentMapImpl() throws JsonProcessingException {
		AnyDTO dto = new AnyDTO(underTest);

		ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
		// normally not necessary, since it's declared as a java-service-loader impl via META-INF/services
		//		mapper.registerModule(new MPHJacksonModule());

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
	@ToString
	public static class AnyDTO {

		// works
		// @JsonDeserialize(using = MPHStringMapDeserializer.class)
		public Map<String, String> map;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PredictDataWrapper {

		private PredictData data;
	}

	@Builder(toBuilder = true)
	@Getter
	@ToString
	@EqualsAndHashCode
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
	public static class PredictData {

		private final Map<String, int[]> map;
	}
}
