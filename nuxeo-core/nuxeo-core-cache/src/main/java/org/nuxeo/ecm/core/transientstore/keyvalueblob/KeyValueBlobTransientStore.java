/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.transientstore.keyvalueblob;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.transientstore.api.MaximumTransientSpaceExceeded;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Transient Store storing properties in a Key/Value store, and storing blobs using a Blob Provider.
 * <p>
 * This transient store is configured with the following properties:
 * <ul>
 * <li><em>keyValueStore</em>: the name of the key/value store to use. If not provided, it defaults to "transient_" +
 * the transient store name.
 * <li><em>blobProvider</em>: the name of the blob provider to use. If not provided, it defaults to "transient_" + the
 * transient store name.
 * <li><em>defaultBlobProvider</em>: if the configured or defaulted blob provider doesn't exist, a namespaced copy of
 * this one will be used instead. The default is "default".
 * </ul>
 * <p>
 * The storage format is the following:
 *
 * <pre>
 *   __blobsize__:       storage size; because entries may expire without us being notified due to their TTL,
 *                       this may be higher than the actual storage size
 *
 *   entryKey.completed: "true" if completed, "false" if not; presence of this key marks entry existence
 *
 *   entryKey.paraminfo: ["foo", "bar"]
 *
 *   entryKey.param.foo: value for param foo
 *   entryKey.param.foo__format: "java" for java serializable format, otherwise string
 *
 *   entryKey.param.bar: value for param bar
 *   etc.
 *
 *   entryKey.bloblock:  "true" if there is a blob read/write in progress, null otherwise
 *   entryKey.blobinfo:  {"count": number of blobs,
 *                        "size": storage size of the blobs}
 *   entryKey.blob.0:    {"key": key in blob provider for first blob,
 *                        "mimetype": MIME Type,
 *                        "encoding": encoding,
 *                        "filename": filename,
 *                        "digest": digest}
 *   entryKey.blob.1:    {...} same for second blob
 *   etc.
 * </pre>
 *
 * @since 9.3
 */
public class KeyValueBlobTransientStore implements TransientStoreProvider {

    private static final Log log = LogFactory.getLog(KeyValueBlobTransientStore.class);

    public static final String SEP = ".";

    public static final String STORAGE_SIZE = "__blobsize__";

    public static final String DOT_COMPLETED = SEP + "completed";

    public static final String DOT_PARAMINFO = SEP + "paraminfo";

    public static final String DOT_PARAM_DOT = SEP + "param" + SEP;

    public static final String FORMAT = "__format";

    public static final String FORMAT_JAVA = "java";

    /** @since 11.1 */
    public static final String DOT_BLOBLOCK = SEP + "bloblock";

    public static final String DOT_BLOBINFO = SEP + "blobinfo";

    public static final String COUNT = "count";

    public static final String SIZE = "size";

    public static final String DOT_BLOB_DOT = SEP + "blob" + SEP;

    public static final String KEY = "key";

    public static final String MIMETYPE = "mimetype";

    public static final String ENCODING = "encoding";

    public static final String FILENAME = "filename";

    public static final String LENGTH = "length";

    public static final String DIGEST = "digest";

    public static final String CONFIG_KEY_VALUE_STORE = "keyValueStore";

    public static final String CONFIG_BLOB_PROVIDER = "blobProvider";

    /** @since 11.1 */
    public static final String CONFIG_DEFAULT_BLOB_PROVIDER = "defaultBlobProvider";

    /** @since 11.1 */
    public static final String CONFIG_DEFAULT_BLOB_PROVIDER_DEFAULT = "default";

    /** @since 11.1 */
    protected static final int BLOB_LOCK_TTL = 60; // don't keep any lock longer than 60s

    /** @since 11.1 */
    protected static final long LOCK_ACQUIRE_TIME_NANOS = TimeUnit.SECONDS.toNanos(5);

    /** @since 11.1 */
    protected static final long LOCK_EXPONENTIAL_BACKOFF_AFTER_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    protected String name;

    protected String keyValueStoreName;

    protected String blobProviderId;

    protected String defaultBlobProviderId;

    /** Basic TTL for all entries. */
    protected int ttl;

    /** TTL used to keep objects around a bit longer if there's space for them, for caching. */
    protected int releaseTTL;

    protected long targetMaxSize;

    protected long absoluteMaxSize;

    protected ObjectMapper mapper;

    // ---------- TransientStoreProvider ----------

    @Override
    public void init(TransientStoreConfig config) {
        name = config.getName();
        String defaultName = name;
        if (!defaultName.startsWith(BlobManagerComponent.TRANSIENT_ID_PREFIX)) {
            defaultName = BlobManagerComponent.TRANSIENT_ID_PREFIX + "_" + defaultName;
        }
        Map<String, String> properties = config.getProperties();
        if (properties == null) {
            properties = Collections.emptyMap();
        }
        keyValueStoreName = defaultIfBlank(properties.get(CONFIG_KEY_VALUE_STORE), defaultName);
        blobProviderId = defaultIfBlank(properties.get(CONFIG_BLOB_PROVIDER), defaultName);
        defaultBlobProviderId = defaultIfBlank(properties.get(CONFIG_DEFAULT_BLOB_PROVIDER),
                CONFIG_DEFAULT_BLOB_PROVIDER_DEFAULT);
        mapper = new ObjectMapper();
        ttl = config.getFirstLevelTTL() * 60;
        releaseTTL = config.getSecondLevelTTL() * 60;
        targetMaxSize = config.getTargetMaxSizeMB() * 1024L * 1024;
        absoluteMaxSize = config.getAbsoluteMaxSizeMB() * 1024L * 1024;
    }

    protected KeyValueStore getKeyValueStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(keyValueStoreName);
    }

    protected BlobProvider getBlobProvider() {
        BlobProvider blobProvider = Framework.getService(BlobManager.class)
                                             .getBlobProviderWithNamespace(blobProviderId, defaultBlobProviderId);
        if (blobProvider == null) {
            throw new NuxeoException("No blob provider with id: " + blobProviderId);
        }
        if (!blobProvider.isTransient()) {
            throw new NuxeoException("Blob provider: " + blobProviderId + " used for Key/Value store: "
                    + keyValueStoreName + " must be configured as transient");
        }
        return blobProvider;
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

    @Override
    public Stream<String> keyStream() {
        KeyValueStoreProvider kvs = (KeyValueStoreProvider) getKeyValueStore();
        int len = DOT_COMPLETED.length();
        return kvs.keyStream() //
                  .filter(key -> key.endsWith(DOT_COMPLETED))
                  .map(key -> key.substring(0, key.length() - len));
    }

    @Override
    public long getStorageSize() {
        KeyValueStore kvs = getKeyValueStore();
        String sizeStr = kvs.getString(STORAGE_SIZE);
        return sizeStr == null ? 0 : Long.parseLong(sizeStr);
    }

    /** @deprecated since 11.1 */
    @Deprecated
    protected void addStorageSize(long delta) {
        KeyValueStore kvs = getKeyValueStore();
        addStorageSize(delta, kvs);
    }

    protected void addStorageSize(long delta, KeyValueStore kvs) {
        atomicUpdate(STORAGE_SIZE, size -> {
            long s = size == null ? 0 : Long.parseLong(size);
            return String.valueOf(s + delta);
        }, 0, kvs);
    }

    /**
     * Computes an exact value for the current storage size (sum of all blobs size). THIS METHOD IS COSTLY.
     * <p>
     * Does not take into account blob de-duplication that may be done by the blob provider.
     * <p>
     * Does not take into account blobs that still exist in the blob provider but are not referenced anymore (due to TTL
     * expiration or GC not having been done).
     */
    protected void computeStorageSize() {
        KeyValueStore kvs = getKeyValueStore();
        long size = keyStream().map(this::getBlobs) //
                               .flatMap(Collection::stream)
                               .mapToLong(Blob::getLength)
                               .sum();
        kvs.put(STORAGE_SIZE, String.valueOf(size));
    }

    // also recomputes the exact storage size
    @Override
    public void doGC() {
        BlobProvider bp = getBlobProvider();
        BinaryGarbageCollector gc = bp.getBinaryGarbageCollector();
        boolean delete = false;
        gc.start();
        try {
            keyStream().map(this::getBlobKeys) //
                       .flatMap(Collection::stream)
                       .forEach(gc::mark);
            delete = true;
        } finally {
            // don't delete if there's an exception, but still stop the GC
            gc.stop(delete);
        }
        computeStorageSize();
    }

    @Override
    public void removeAll() {
        KeyValueStoreProvider kvs = (KeyValueStoreProvider) getKeyValueStore();
        kvs.clear();
        doGC();
    }

    // ---------- TransientStore ----------

    protected static final TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {
    };

    protected static final TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<Map<String, String>>() {
    };

    protected List<String> jsonToList(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, LIST_STRING);
        } catch (IOException e) {
            log.error("Invalid JSON array: " + json);
            return null;
        }
    }

    protected Map<String, String> jsonToMap(String json) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, MAP_STRING_STRING);
        } catch (IOException e) {
            log.error("Invalid JSON object: " + json);
            return null;
        }
    }

    protected String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /** @deprecated since 11.1 */
    @Deprecated
    public void atomicUpdate(String key, Function<String, String> updateFunction, long ttl) {
        KeyValueStore kvs = getKeyValueStore();
        atomicUpdate(key, updateFunction, ttl, kvs);
    }

    protected void atomicUpdate(String key, Function<String, String> updateFunction, long ttl, KeyValueStore kvs) {
        for (;;) {
            String oldValue = kvs.getString(key);
            String newValue = updateFunction.apply(oldValue);
            if (kvs.compareAndSet(key, oldValue, newValue, ttl)) {
                break;
            }
        }
    }

    @Override
    public boolean exists(String key) {
        KeyValueStore kvs = getKeyValueStore();
        return kvs.getString(key + DOT_COMPLETED) != null;
    }

    /** @deprecated since 11.1 */
    @Deprecated
    protected void markEntryExists(String key) {
        KeyValueStore kvs = getKeyValueStore();
        markEntryExists(key, kvs);
    }

    protected void markEntryExists(String key, KeyValueStore kvs) {
        kvs.compareAndSet(key + DOT_COMPLETED, null, "false", ttl);
    }

    @Override
    public void putParameter(String key, String parameter, Serializable value) {
        KeyValueStore kvs = getKeyValueStore();
        String k = key + DOT_PARAM_DOT + parameter;
        if (value instanceof String) {
            kvs.put(k, (String) value, ttl);
            kvs.put(k + FORMAT, (String) null);
        } else {
            byte[] bytes = SerializationUtils.serialize(value);
            kvs.put(k, bytes, ttl);
            kvs.put(k + FORMAT, FORMAT_JAVA, ttl);
        }
        // atomically add key to param info
        atomicUpdate(key + DOT_PARAMINFO, json -> {
            List<String> parameters = jsonToList(json);
            if (parameters == null) {
                parameters = new ArrayList<>();
            }
            if (!parameters.contains(parameter)) {
                parameters.add(parameter);
            }
            return toJson(parameters);
        }, ttl, kvs);
        markEntryExists(key, kvs);
    }

    @Override
    public Serializable getParameter(String key, String parameter) {
        KeyValueStore kvs = getKeyValueStore();
        String k = key + DOT_PARAM_DOT + parameter;
        String format = kvs.getString(k + FORMAT);
        if (format == null) {
            return kvs.getString(k);
        } else {
            byte[] bytes = kvs.get(k);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
                    ObjectInput in = new ObjectInputStream(bis)) {
                return (Serializable) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    public void putParameters(String key, Map<String, Serializable> parameters) {
        parameters.forEach((param, value) -> putParameter(key, param, value));
    }

    @Override
    public Map<String, Serializable> getParameters(String key) {
        KeyValueStore kvs = getKeyValueStore();
        // get the list of keys
        String json = kvs.getString(key + DOT_PARAMINFO);
        List<String> parameters = jsonToList(json);
        if (parameters == null) {
            // if the entry doesn't exist at all return null, otherwise empty
            if (kvs.getString(key + DOT_COMPLETED) == null) {
                return null;
            } else {
                return Collections.emptyMap();
            }
        }
        // get values
        Map<String, Serializable> map = new HashMap<>();
        for (String p : parameters) {
            Serializable value = getParameter(key, p);
            if (value != null) {
                map.put(p, value);
            }
        }
        return map;
    }

    /** @deprecated since 11.1 */
    @Deprecated
    protected void removeParameters(String key) {
        KeyValueStore kvs = getKeyValueStore();
        removeParameters(key, kvs);
    }

    protected void removeParameters(String key, KeyValueStore kvs) {
        String json = kvs.getString(key + DOT_PARAMINFO);
        List<String> parameters = jsonToList(json);
        if (parameters != null) {
            for (String parameter : parameters) {
                String k = key + DOT_PARAM_DOT + parameter;
                kvs.put(k, (String) null);
                kvs.put(k + FORMAT, (String) null);
            }
        }
        kvs.put(key + DOT_PARAMINFO, (String) null);
    }

    @Override
    public void putBlobs(String key, List<Blob> blobs) {
        if (absoluteMaxSize > 0 && getStorageSize() > absoluteMaxSize) {
            // do the costly computation of the exact storage size if needed
            doGC();
            if (getStorageSize() > absoluteMaxSize) {
                throw new MaximumTransientSpaceExceeded();
            }
        }

        // first, outside the lock
        // store the blobs, and compute the total size and the blob maps
        BlobProvider bp = getBlobProvider();
        long totalSize = 0;
        List<String> blobMapJsons = new ArrayList<>();
        for (Blob blob : blobs) {
            long size = blob.getLength();
            if (size >= 0) {
                totalSize += size;
            }
            // store blob
            String blobKey;
            try {
                blobKey = bp.writeBlob(blob);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            // compute blob data
            Map<String, String> blobMap = new HashMap<>();
            blobMap.put(KEY, blobKey);
            blobMap.put(MIMETYPE, blob.getMimeType());
            blobMap.put(ENCODING, blob.getEncoding());
            blobMap.put(FILENAME, blob.getFilename());
            blobMap.put(LENGTH, String.valueOf(size));
            blobMap.put(DIGEST, blob.getDigest());
            String blobMapJson = toJson(blobMap);
            blobMapJsons.add(blobMapJson);
        }
        Map<String, String> blobInfoMap = new HashMap<>();
        blobInfoMap.put(COUNT, String.valueOf(blobs.size()));
        blobInfoMap.put(SIZE, String.valueOf(totalSize));
        String blobInfoMapJson = toJson(blobInfoMap);

        // acquire a lock while writing
        KeyValueStore kvs = getKeyValueStore();
        acquireBlobLockOrThrow(key, kvs);
        try {
            // remove previous blobs
            removeBlobs(key, kvs);
            // write new blobs maps
            int i = 0;
            for (String blobMapJson : blobMapJsons) {
                kvs.put(key + DOT_BLOB_DOT + i, blobMapJson, ttl);
                i++;
            }
            // write blob info
            kvs.put(key + DOT_BLOBINFO, blobInfoMapJson, ttl);
            addStorageSize(totalSize, kvs);
            markEntryExists(key, kvs);
        } finally {
            releaseBlobLock(key, kvs);
        }
    }

    /** @deprecated since 11.1 */
    @Deprecated
    protected void removeBlobs(String key) {
        KeyValueStore kvs = getKeyValueStore();
        removeBlobs(key, kvs);
    }

    protected void removeBlobs(String key, KeyValueStore kvs) {
        String json = kvs.getString(key + DOT_BLOBINFO);
        Map<String, String> map = jsonToMap(json);
        if (map == null) {
            return;
        }
        String countStr = map.get(COUNT);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);
        String sizeStr = map.get(SIZE);
        long size = sizeStr == null ? 0 : Long.parseLong(sizeStr);

        // remove blobs
        for (int i = 0; i < count; i++) {
            kvs.put(key + DOT_BLOB_DOT + i, (String) null);
        }
        kvs.put(key + DOT_BLOBINFO, (String) null);
        // fix storage size
        addStorageSize(-size, kvs);
    }

    @Override
    public List<Blob> getBlobs(String key) {
        KeyValueStore kvs = getKeyValueStore();
        BlobProvider bp = getBlobProvider();
        List<String> blobMapJsons = new ArrayList<>();

        // try to acquire a lock but still proceed without the lock (best effort)
        boolean lockAcquired = tryAcquireBlobLock(key, kvs);
        try {
            String info = kvs.getString(key + DOT_BLOBINFO);
            if (info == null) {
                // if the entry doesn't exist at all return null, otherwise empty
                if (kvs.getString(key + DOT_COMPLETED) == null) {
                    return null;
                } else {
                    return Collections.emptyList();
                }
            }
            Map<String, String> blobInfoMap = jsonToMap(info);
            String countStr = blobInfoMap.get(COUNT);
            if (countStr == null) {
                return Collections.emptyList();
            }
            int count = Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                String blobMapJson = kvs.getString(key + DOT_BLOB_DOT + i);
                blobMapJsons.add(blobMapJson);
            }
        } finally {
            if (lockAcquired) {
                releaseBlobLock(key, kvs);
            }
        }

        // compute blobs from read blob maps
        List<Blob> blobs = new ArrayList<>();
        for (String blobMapJson : blobMapJsons) {
            if (blobMapJson == null) {
                // corrupted entry, bail out
                break;
            }
            Map<String, String> blobMap = jsonToMap(blobMapJson);
            String blobKey = blobMap.get(KEY);
            if (blobKey == null) {
                // corrupted entry, bail out
                break;
            }
            String mimeType = blobMap.get(MIMETYPE);
            String encoding = blobMap.get(ENCODING);
            String filename = blobMap.get(FILENAME);
            String lengthStr = blobMap.get(LENGTH);
            Long length = lengthStr == null ? null : Long.valueOf(lengthStr);
            String digest = blobMap.get(DIGEST);
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = blobKey;
            blobInfo.mimeType = mimeType;
            blobInfo.encoding = encoding;
            blobInfo.filename = filename;
            blobInfo.length = length;
            blobInfo.digest = digest;
            try {
                Blob blob = bp.readBlob(blobInfo);
                blobs.add(blob);
            } catch (IOException e) {
                // ignore, the blob was removed from the blob provider
                // maybe by a concurrent GC from this transient store
                // or from the blob provider itself (if it's incorrectly shared)
                log.debug("Failed to read blob: " + digest + " in blob provider: " + blobProviderId
                        + " for transient store: " + name);
            }
        }
        return blobs;
    }

    // used by GC
    protected List<String> getBlobKeys(String key) {
        KeyValueStore kvs = getKeyValueStore();
        String info = kvs.getString(key + DOT_BLOBINFO);
        if (info == null) {
            return Collections.emptyList();
        }
        Map<String, String> blobInfoMap = jsonToMap(info);
        String countStr = blobInfoMap.get(COUNT);
        if (countStr == null) {
            return Collections.emptyList();
        }
        int count = Integer.parseInt(countStr);
        List<String> blobKeys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String blobMapJson = kvs.getString(key + DOT_BLOB_DOT + i);
            if (blobMapJson == null) {
                // corrupted entry, bail out
                break;
            }
            Map<String, String> blobMap = jsonToMap(blobMapJson);
            String blobKey = blobMap.get(KEY);
            if (blobKey == null) {
                // corrupted entry, bail out
                break;
            }
            blobKeys.add(blobKey);
        }
        return blobKeys;
    }

    protected void acquireBlobLockOrThrow(String key, KeyValueStore kvs) {
        if (tryAcquireBlobLock(key, kvs)) {
            return;
        }
        throw new NuxeoException("Failed to acquire blob lock for: " + key);
    }

    protected boolean tryAcquireBlobLock(String key, KeyValueStore kvs) {
        return acquireLock(() -> tryAcquireOnceBlobLock(key, kvs));
    }

    protected boolean tryAcquireOnceBlobLock(String key, KeyValueStore kvs) {
        return kvs.compareAndSet(key + DOT_BLOBLOCK, null, "true", BLOB_LOCK_TTL);
    }

    protected void releaseBlobLock(String key, KeyValueStore kvs) {
        kvs.put(key + DOT_BLOBLOCK, (String) null);
    }

    protected boolean acquireLock(BooleanSupplier tryAcquireOnce) {
        long start = System.nanoTime();
        long sleep = 1; // ms
        long elapsed;
        while ((elapsed = System.nanoTime() - start) < LOCK_ACQUIRE_TIME_NANOS) {
            if (tryAcquireOnce.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(sleep);
                if (elapsed > LOCK_EXPONENTIAL_BACKOFF_AFTER_NANOS) {
                    sleep *= 2;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public long getSize(String key) {
        KeyValueStore kvs = getKeyValueStore();
        String json = kvs.getString(key + DOT_BLOBINFO);
        Map<String, String> map = jsonToMap(json);
        String size;
        if (map == null || (size = map.get(SIZE)) == null) {
            return -1;
        }
        return Long.parseLong(size);
    }

    @Override
    public boolean isCompleted(String key) {
        KeyValueStore kvs = getKeyValueStore();
        String completed = kvs.getString(key + DOT_COMPLETED);
        return Boolean.parseBoolean(completed);
    }

    @Override
    public void setCompleted(String key, boolean completed) {
        KeyValueStore kvs = getKeyValueStore();
        kvs.put(key + DOT_COMPLETED, String.valueOf(completed), ttl);
    }

    /** @deprecated since 11.1 */
    @Deprecated
    protected void removeCompleted(String key) {
        KeyValueStore kvs = getKeyValueStore();
        removeCompleted(key, kvs);
    }

    protected void removeCompleted(String key, KeyValueStore kvs) {
        kvs.put(key + DOT_COMPLETED, (String) null);
    }

    @Override
    public void release(String key) {
        if (targetMaxSize > 0 && getStorageSize() > targetMaxSize) {
            // do the costly computation of the exact storage size if needed
            doGC();
            if (getStorageSize() > targetMaxSize) {
                remove(key);
                return;
            }
        }
        setReleaseTTL(key);
    }

    // set TTL on all keys for this entry
    protected void setReleaseTTL(String key) {
        KeyValueStore kvs = getKeyValueStore();
        kvs.setTTL(key + DOT_COMPLETED, releaseTTL);
        String json = kvs.getString(key + DOT_PARAMINFO);
        List<String> parameters = jsonToList(json);
        if (parameters != null) {
            parameters.stream().forEach(parameter -> {
                String k = key + DOT_PARAM_DOT + parameter;
                kvs.setTTL(k, releaseTTL);
                kvs.setTTL(k + FORMAT, releaseTTL);
            });
        }
        kvs.setTTL(key + DOT_PARAMINFO, releaseTTL);
        json = kvs.getString(key + DOT_BLOBINFO);
        Map<String, String> map = jsonToMap(json);
        if (map != null) {
            String countStr = map.get(COUNT);
            int count = countStr == null ? 0 : Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                kvs.setTTL(key + DOT_BLOB_DOT + i, releaseTTL);
            }
        }
        kvs.setTTL(key + DOT_BLOBINFO, releaseTTL);
    }

    @Override
    public void remove(String key) {
        KeyValueStore kvs = getKeyValueStore();
        removeBlobs(key, kvs);
        removeParameters(key, kvs);
        removeCompleted(key, kvs);
    }

}
