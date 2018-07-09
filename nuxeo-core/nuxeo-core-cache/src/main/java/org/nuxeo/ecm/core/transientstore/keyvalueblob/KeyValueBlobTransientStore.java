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

import static java.util.function.Function.identity;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
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
 * The key/value store used is the one with the same name as the transient store itself.
 * <p>
 * The blob provider used is the one with the same name as the transient store itself.
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

    protected String name;

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
        mapper = new ObjectMapper();
        ttl = config.getFirstLevelTTL() * 60;
        releaseTTL = config.getSecondLevelTTL() * 60;
        targetMaxSize = config.getTargetMaxSizeMB() * 1024 * 1024;
        absoluteMaxSize = config.getAbsoluteMaxSizeMB() * 1024 * 1024;
    }

    protected KeyValueStore getKeyValueStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(name);
    }

    protected BlobProvider getBlobProvider() {
        return Framework.getService(BlobManager.class).getBlobProvider(name);
    }

    @Override
    public void shutdown() {
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

    protected void addStorageSize(long delta) {
        atomicUpdate(STORAGE_SIZE, size -> {
            long s = size == null ? 0 : Long.parseLong(size);
            return String.valueOf(s + delta);
        }, 0);
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
        BinaryGarbageCollector gc = bp.getBinaryManager().getGarbageCollector();
        gc.start();
        keyStream().map(this::getBlobs) //
                   .flatMap(Collection::stream)
                   .map(ManagedBlob.class::cast)
                   .map(ManagedBlob::getKey)
                   .forEach(gc::mark);
        gc.stop(true); // delete
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

    public void atomicUpdate(String key, Function<String, String> updateFunction, long ttl) {
        KeyValueStore kvs = getKeyValueStore();
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

    protected void markEntryExists(String key) {
        KeyValueStore kvs = getKeyValueStore();
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
        }, ttl);
        markEntryExists(key);
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
        return parameters.stream().collect(Collectors.toMap(identity(), p -> getParameter(key, p)));
    }

    protected void removeParameters(String key) {
        KeyValueStore kvs = getKeyValueStore();
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

        // remove previous blobs
        removeBlobs(key);

        KeyValueStore kvs = getKeyValueStore();
        BlobProvider bp = getBlobProvider();
        long totalSize = 0;
        int i = 0;
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
            // write blob data
            Map<String, String> blobMap = new HashMap<>();
            blobMap.put(KEY, blobKey);
            blobMap.put(MIMETYPE, blob.getMimeType());
            blobMap.put(ENCODING, blob.getEncoding());
            blobMap.put(FILENAME, blob.getFilename());
            blobMap.put(LENGTH, String.valueOf(size));
            blobMap.put(DIGEST, blob.getDigest());
            kvs.put(key + DOT_BLOB_DOT + i, toJson(blobMap), ttl);
            i++;
        }
        Map<String, String> blobInfoMap = new HashMap<>();
        blobInfoMap.put(COUNT, String.valueOf(blobs.size()));
        blobInfoMap.put(SIZE, String.valueOf(totalSize));
        kvs.put(key + DOT_BLOBINFO, toJson(blobInfoMap), ttl);
        addStorageSize(totalSize);
        markEntryExists(key);
    }

    protected void removeBlobs(String key) {
        KeyValueStore kvs = getKeyValueStore();
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
        addStorageSize(-size);
    }

    @Override
    public List<Blob> getBlobs(String key) {
        KeyValueStore kvs = getKeyValueStore();
        BlobProvider bp = getBlobProvider();
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
        List<Blob> blobs = new ArrayList<>();
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
                throw new NuxeoException(e);
            }
        }
        return blobs;
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

    protected void removeCompleted(String key) {
        KeyValueStore kvs = getKeyValueStore();
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
        removeBlobs(key);
        removeParameters(key);
        removeCompleted(key);
    }

}
