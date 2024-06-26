package io.searchhub.mph;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;

class MPHStringMapTest extends AbstractMPHStringIntegerMapTest<MPHStringMap<Integer>> {

	@Override
	protected MPHStringMap<Integer> createUnderTest(Map<String, Integer> testData) {
		return MPHStringMap.build(testData);
	}

	@Test
	void serializationRoundTrip() throws IOException, ClassNotFoundException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objectOut = new ObjectOutputStream(out);
		objectOut.writeObject(underTest.getSerializableMphMapData());
		objectOut.close();

		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		MPHStringMap.SerializableData<Integer> deserializedData = (MPHStringMap.SerializableData<Integer>) objectInputStream.readObject();
		underTest = MPHStringMap.fromData(deserializedData);
		containsKey();
		values();
	}

}
