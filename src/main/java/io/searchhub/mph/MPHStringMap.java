package io.searchhub.mph;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.minperf.BitBuffer;
import org.minperf.RecSplitBuilder;
import org.minperf.RecSplitEvaluator;
import org.minperf.universal.StringHash;
import org.minperf.universal.UniversalHash;

/**
 * Immutable map using minimal perfect hashing for the keys + stores additional hash value per key to reduce risk of wrong mapping.
 * <p>Since keys are not stored, it's not possible to use `keySet` and `entrySet`.</p>
 * <p>
 * Also since immutable, put, putAll, clear and remove will throw an UnsupportedOperationException.
 * </p>
 *
 * @param <V>
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MPHStringMap<V> implements Map<String, V> {

	public static <V> MPHStringMap<V> build(Map<String, V> inputData) {
		return build(inputData.keySet(), inputData::get, inputData.size());
	}

	/**
	 * Use this builder in case you have duplicate values that can be stored once.
	 * To use it, the exact amount of values has to be known.
	 * The values SHOULD implement equals and hashCode to allow a correct deduplication.
	 *
	 * @param keys key-set
	 * @param valueLookup function to lookup a value for a key
	 * @param valueCount the exact count of values. If the value count is similar to the amount of keys, no deduplication is done.
	 * @return
	 * @param <V> value type
	 */
	public static <V> MPHStringMap<V> build(Set<String> keys, Function<String, V> valueLookup, int valueCount) {
		long[] keyValueMap = new long[keys.size()];
		V[] values = (V[]) new Object[valueCount];

		if (keys.isEmpty()) return new MPHStringMap<>(k -> -1, keyValueMap, values);

		RecSplitEvaluator<String> recSplitEvaluator = getMphFunction(keys);

		AtomicInteger valueIndex = new AtomicInteger(0);
		// if there are less values than keys, then use deduplication
		Map<V, Integer> valueDeduplication = valueCount == keys.size() ? null : new HashMap<>();
		for(String key :keys) {
			int keyIndex = recSplitEvaluator.evaluate(key);
			V value = valueLookup.apply(key);

			int _valueIndex;
			if (valueDeduplication != null) {
				_valueIndex = valueDeduplication.computeIfAbsent(value, v -> valueIndex.getAndIncrement());
			} else {
				_valueIndex = valueIndex.getAndIncrement();
			}

			if (_valueIndex >= values.length) {
				throw new IllegalArgumentException("Found more values than specified by valueCount "+valueCount);
			}
			values[_valueIndex] = value;
			keyValueMap[keyIndex] = getVerifiableValueIndex(key, _valueIndex);
		}

		return new MPHStringMap<>(recSplitEvaluator::evaluate, keyValueMap, values);
	}

	private static RecSplitEvaluator<String> getMphFunction(Set<String> keys) {
		int LEAF_SIZE = 8;
		int AVG_BUCKET_SIZE = 32;
		UniversalHash<String> hashFunction = new StringHash();
		BitBuffer mphFunction = RecSplitBuilder
				.newInstance(hashFunction)
				.leafSize(LEAF_SIZE)
				.averageBucketSize(AVG_BUCKET_SIZE)
				.generate(keys);
		return RecSplitBuilder
				.newInstance(hashFunction)
				.leafSize(LEAF_SIZE)
				.averageBucketSize(AVG_BUCKET_SIZE)
				.buildEvaluator(mphFunction);
	}

	private final Function<String, Integer> mphFunction;
	private final long[]                    keyValueMap;
	private final V[]                       values;

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
		return valueIndex >= 0 ? values[valueIndex] : null;
	}

	@Override
	public Collection<V> values() {
		return Arrays.asList(values);
	}

	@Override
	public V put(String key, V value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Entry<String, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
}
