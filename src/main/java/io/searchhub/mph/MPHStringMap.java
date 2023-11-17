package io.searchhub.mph;

import static io.searchhub.mph.MPHUtil.buildEvaluator;
import static io.searchhub.mph.MPHUtil.getMphFunctionData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.minperf.RecSplitEvaluator;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Immutable map using minimal perfect hashing for the keys + stores additional hash value per key to reduce risk of wrong mapping.
 * <p>Since keys are not stored, it's not possible to use `keySet` and `entrySet`.</p>
 * <p>
 * Also since immutable, put, putAll, clear and remove will throw an UnsupportedOperationException.
 * </p>
 *
 * @param <V>
 */
public class MPHStringMap<V> implements Map<String, V> {

	private final static Function<String, Integer> EMPTY_MAP_FUNCTION = x -> -1;

	@RequiredArgsConstructor
	@AllArgsConstructor
	@Getter
	public final static class SerializableData<V> implements Serializable {

		static <X> SerializableData<X> getEmptyData() {
			return new SerializableData<>(8, 32, new byte[0], new long[0], Collections.emptyList());
		}

		static final long serialVersionUID = 1_000L;

		int    leafSize;
		int    avgBucketSize;
		byte[] mphFunctionData;
		long[] keyValueMap;
		List<V> values;

		public void setMphFunctionData(String base64Str) {
			this.mphFunctionData = Base64.getDecoder().decode(base64Str);
		}
	}

	public static <V> MPHStringMap<V> build(Map<String, V> inputData) {
		return build(inputData.keySet(), inputData::get, inputData.size());
	}

	/**
	 * Use this builder in case you have duplicate values that can be stored once.
	 * To use it, the exact amount of values has to be known.
	 * The values SHOULD implement equals and hashCode to allow a correct deduplication.
	 *
	 * @param keys        key-set
	 * @param valueLookup function to lookup a value for a key
	 * @param valueCount  the exact count of values. If the value count is similar to the amount of keys, no deduplication is done.
	 * @param <V>         value type
	 * @return
	 */
	public static <V> MPHStringMap<V> build(Set<String> keys, Function<String, V> valueLookup, int valueCount) {
		long[] keyValueMap = new long[keys.size()];
		List<V> values = new ArrayList<>(Collections.nCopies(valueCount, null));
		if (keys.isEmpty()) return new MPHStringMap<>(EMPTY_MAP_FUNCTION, SerializableData.getEmptyData());

		int leafSize = 8, avgBucketSize = 32;
		byte[] mphFunctionData = getMphFunctionData(leafSize, avgBucketSize, keys);
		SerializableData<V> mphMapData = new SerializableData<>(leafSize, avgBucketSize, mphFunctionData, keyValueMap, values);

		RecSplitEvaluator<String> recSplitEvaluator = buildEvaluator(leafSize, avgBucketSize, mphFunctionData);
		AtomicInteger valueIndex = new AtomicInteger(0);
		// if there are less values than keys, then use deduplication
		Map<V, Integer> valueDeduplication = valueCount == keys.size() ? null : new HashMap<>();
		for (String key : keys) {
			int keyIndex = recSplitEvaluator.evaluate(key);
			V value = valueLookup.apply(key);

			int _valueIndex;
			if (valueDeduplication != null) {
				_valueIndex = valueDeduplication.computeIfAbsent(value, v -> valueIndex.getAndIncrement());
			}
			else {
				_valueIndex = valueIndex.getAndIncrement();
			}

			if (_valueIndex > values.size()) {
				throw new IllegalArgumentException("Found more values than specified by valueCount " + valueCount);
			}
			values.set(_valueIndex, value);
			keyValueMap[keyIndex] = getVerifiableValueIndex(key, _valueIndex);
		}

		return new MPHStringMap<>(recSplitEvaluator::evaluate, mphMapData);
	}

	public static <V> MPHStringMap<V> build(Iterable<Entry<String, V>> keyValueIterable, int size) {
		AtomicReference<Entry<String, V>> currentEntry = new AtomicReference<>();
		Set<String> keySetEmulator = new AbstractSet() {

			@Override
			public Iterator<String> iterator() {
				Iterator<Entry<String, V>> kvIterator = keyValueIterable.iterator();
				return new Iterator() {

					@Override
					public boolean hasNext() {
						return kvIterator.hasNext();
					}

					@Override
					public String next() {
						Entry<String, V> next = kvIterator.next();
						currentEntry.set(next);
						return next.getKey();
					}
				};
			}

			@Override
			public int size() {
				return size;
			}
		};
		return build(keySetEmulator, key -> {
			Entry<String, V> entry = currentEntry.get();
			if (key != null && key.equals(entry.getKey())) {
				return entry.getValue();
			}
			else {
				return null;
			}
		}, size);
	}

	public static <V> MPHStringMap<V> fromData(SerializableData<V> data) {
		Function<String, Integer> mphFunction = (data.mphFunctionData.length == 0) ? x -> -1 : buildEvaluator(data.leafSize, data.avgBucketSize, data.mphFunctionData)::evaluate;
		return new MPHStringMap<V>(mphFunction, data);
	}

	private MPHStringMap(Function<String, Integer> mphFunction, SerializableData<V> data) {
		this.mphFunction = mphFunction;
		this.serializableMphMapData = data;
		this.keyValueMap = data.keyValueMap;
		this.values = data.values;
	}

	@Getter
	private final SerializableData<V> serializableMphMapData;

	private volatile Function<String, Integer> mphFunction;

	private volatile long[]  keyValueMap;
	private volatile List<V> values;

	private static long getVerifiableValueIndex(String originalKey, int valueIndex) {
		long encoded = originalKey.hashCode();
		encoded <<= 32;
		encoded |= valueIndex;
		return encoded;
	}

	private static int getVerifiedValueIndex(long key, String searchKey) {
		int keyChecksum = (int) (key >>> 32);
		return keyChecksum == searchKey.hashCode() ? (int) key : -1;
	}

	private int getValueIndex(String searchKey) {
		int keyIndex = mphFunction.apply(searchKey);
		return getVerifiedValueIndex(keyValueMap[keyIndex], searchKey);
	}

	@Override
	public int size() {
		return keyValueMap.length;
	}

	@Override
	public boolean isEmpty() {
		return keyValueMap.length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return getValueIndex(key.toString()) >= 0;
	}

	@Override
	public boolean containsValue(Object value) {
		boolean result = false;
		for (V v : values) {
			if ((value == null && v == null) || v.equals(value)) {
				result = true;
				break;
			}
		}
		return result;
	}

	@Override
	public V get(Object key) {
		int valueIndex = getValueIndex(key.toString());
		return valueIndex >= 0 ? values.get(valueIndex) : null;
	}

	@Override
	public Collection<V> values() {
		return new ArrayList<>(values);
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public V put(String key, V value) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public void putAll(Map<? extends String, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException due to immutability
	 */
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException since keys are not stored with the map
	 */
	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @throws UnsupportedOperationException since keys are not stored with the map
	 */
	@Override
	public Set<Entry<String, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
