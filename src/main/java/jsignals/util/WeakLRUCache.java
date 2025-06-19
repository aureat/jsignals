package jsignals.util;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class WeakLRUCache<K, V> {

    private final Map<WeakKey<K>, V> cache;

    private final int initialCapacity;
    private final float loadFactor;
    private final int maxSize;

    public WeakLRUCache(int initialCapacity, float loadFactor, int maxSize) {
        this.initialCapacity = initialCapacity;
        this.loadFactor = loadFactor;
        this.maxSize = maxSize;
        cache = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity, loadFactor, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<WeakKey<K>, V> eldest) {
                return size() > maxSize;
            }
        });
    }

    public WeakLRUCache(int maxSize) {
        this(16, 0.75f, maxSize);
    }

    public WeakLRUCache() {
        this(16, 0.75f, 100);
    }

    public WeakLRUCache(int initialCapacity, int maxSize) {
        this(initialCapacity, 0.75f, maxSize);
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public float getLoadFactor() {
        return loadFactor;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        WeakKey<K> weakKey = new WeakKey<>(key);
        synchronized (cache) {
            V value = cache.get(weakKey);
            if (value == null) {
                value = mappingFunction.apply(key);
                cache.put(weakKey, value);
            }
            return value;
        }
    }

    private static class WeakKey<T> {

        private final WeakReference<T> ref;
        private final int hash;

        WeakKey(T key) {
            this.ref = new WeakReference<>(key);
            this.hash = key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof WeakKey)) return false;
            T thisKey = ref.get();
            @SuppressWarnings("unchecked") T thatKey = ((WeakKey<T>) obj).ref.get();
            return thisKey != null && thisKey.equals(thatKey);
        }

        @Override
        public int hashCode() {
            return hash;
        }

    }

}