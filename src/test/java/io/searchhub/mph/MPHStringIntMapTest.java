package io.searchhub.mph;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;

class MPHStringIntMapTest extends AbstractMPHStringIntegerMapTest<MPHStringIntMap> {

	@Override
	protected MPHStringIntMap createUnderTest(Map<String, Integer> testData) {
		return MPHStringIntMap.build(testData);
	}

	@Test
	void serializationRoundTrip() throws IOException, ClassNotFoundException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objectOut = new ObjectOutputStream(out);
		objectOut.writeObject(underTest.getSerializableMphMapData());
		objectOut.close();

		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
		MPHStringIntMap.SerializableData deserializedData = (MPHStringIntMap.SerializableData) objectInputStream.readObject();
		underTest = MPHStringIntMap.fromData(deserializedData);
		containsKey();
		values();
	}

}
