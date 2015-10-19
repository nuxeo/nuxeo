/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.redis.contribs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.redis.RedisAdmin;
import org.nuxeo.ecm.core.redis.RedisCallable;
import org.nuxeo.ecm.core.redis.RedisExecutor;
import org.nuxeo.ecm.core.transientstore.AbstractTransientStore;
import org.nuxeo.ecm.core.transientstore.StorageEntry;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreConfig;
import org.nuxeo.runtime.api.Framework;

/**
 * Redis implementation (i.e. cluster aware) of the {@link TransientStore}.
 * <p>
 * Since hashes cannot be nested, a {@link StorageEntry} is flattened as follows:
 *
 * <pre>
 *  - Entry summary:
 * 
 *    transientStore:transientStoreName:entryKey {
 *      "blobCount":    number of blobs associated with the entry
 *      "size":         storage size of the blobs associated with the entry
 *      "completed":    entry status
 *    }
 * 
 * - Entry parameters:
 * 
 *   transientStore:transientStoreName:entryKey:params {
 *      "param1": value1
 *      "param2": value2
 *   }
 * 
 * - Entry blobs:
 * 
 *   transientStore:transientStoreName:entryKey:blobs:0 {
 *      "file"
 *      "filename"
 *      "encoding"
 *      "mimetype"
 *      "digest"
 *   }
 * 
 *   transientStore:transientStoreName:entryKey:blobs:1 {
 *      ...
 *   }
 * 
 *   ...
 * </pre>
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */

public class RedisTransientStore extends AbstractTransientStore {

    protected RedisExecutor redisExecutor;

    protected String namespace;

    protected String sizeKey;

    protected RedisAdmin redisAdmin;

    protected Log log = LogFactory.getLog(RedisTransientStore.class);

    public RedisTransientStore() {
        redisExecutor = Framework.getService(RedisExecutor.class);
        redisAdmin = Framework.getService(RedisAdmin.class);
    }

    @Override
    public void init(TransientStoreConfig config) {
        super.init(config);
        namespace = redisAdmin.namespace("transientStore", config.getName());
        sizeKey = namespace + "size";
    }

    @Override
    public void shutdown() {
        // Nothing to do here.
    }

    @Override
    public boolean exists(String key) {
        // Jedis#exists(String key) doesn't to work for a key created with hset or hmset
        return getSummary(key) != null || getParameters(key) != null;
    }

    @Override
    public void putParameter(String key, String parameter, Serializable value) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.hset(getBytes(namespace + join(key, "params")), getBytes(parameter), serialize(value));
            return null;
        });
    }

    @Override
    public Serializable getParameter(String key, String parameter) {
        return redisExecutor.execute((RedisCallable<Serializable>) jedis -> {
            byte[] paramBytes = jedis.hget(getBytes(namespace + join(key, "params")), getBytes(parameter));
            if (paramBytes == null) {
                return null;
            }
            return deserialize(paramBytes);
        });
    }

    @Override
    public void putParameters(String key, Map<String, Serializable> parameters) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.hmset(getBytes(namespace + join(key, "params")), serialize(parameters));
            return null;
        });
    }

    @Override
    public Map<String, Serializable> getParameters(String key) {
        return redisExecutor.execute((RedisCallable<Map<String, Serializable>>) jedis -> {
            Map<byte[], byte[]> paramBytes = jedis.hgetAll(getBytes(namespace + join(key, "params")));
            if (paramBytes.isEmpty()) {
                return null;
            }
            return deserialize(paramBytes);
        });
    }

    @Override
    public List<Blob> getBlobs(String key) {
        // TODO https://jira.nuxeo.com/browse/NXP-18050
        // Use a transaction

        // Get blob count
        String blobCount = redisExecutor.execute((RedisCallable<String>) jedis -> {
            return jedis.hget(namespace + key, "blobCount");
        });
        if (blobCount == null) {
            // Check for existing parameters
            Map<String, Serializable> parameters = getParameters(key);
            if (parameters == null) {
                return null;
            } else {
                return new ArrayList<Blob>();
            }
        }

        // Get blobs
        int entryBlobCount = Integer.parseInt(blobCount);
        if (entryBlobCount <= 0) {
            return new ArrayList<>();
        }
        List<Map<String, String>> blobInfos = new ArrayList<>();
        for (int i = 0; i < entryBlobCount; i++) {
            String blobInfoIndex = String.valueOf(i);
            Map<String, String> entryBlobInfo = redisExecutor.execute((RedisCallable<Map<String, String>>) jedis -> {
                String blobInfoKey = namespace + join(key, "blobs", blobInfoIndex);
                Map<String, String> blobInfo = jedis.hgetAll(blobInfoKey);
                if (blobInfo.isEmpty()) {
                    throw new NuxeoException(String.format(
                            "Entry with key %s is inconsistent: blobCount = %d but key %s doesn't exist", key,
                            entryBlobCount, blobInfoKey));
                }
                return blobInfo;
            });
            blobInfos.add(entryBlobInfo);
        }

        // Load blobs from the file system
        return loadBlobs(blobInfos);
    }

    @Override
    public long getSize(String key) {
        return redisExecutor.execute((RedisCallable<Long>) jedis -> {
            String size = jedis.hget(namespace + key, "size");
            if (size == null) {
                return -1L;
            }
            return Long.parseLong(size);
        });
    }

    @Override
    public boolean isCompleted(String key) {
        return redisExecutor.execute((RedisCallable<Boolean>) jedis -> {
            String completed = jedis.hget(namespace + key, "completed");
            return Boolean.parseBoolean(completed);
        });
    }

    @Override
    public void setCompleted(String key, boolean completed) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.hset(namespace + key, "completed", String.valueOf(completed));
            return null;
        });
    }

    @Override
    public void remove(String key) {
        // TODO https://jira.nuxeo.com/browse/NXP-18050
        // Use a transaction

        Map<String, String> summary = getSummary(key);
        if (summary != null) {
            // Remove blobs
            String blobCount = summary.get("blobCount");
            deleteBlobInfos(key, blobCount);

            // Remove summary
            redisExecutor.execute((RedisCallable<Long>) jedis -> {
                return jedis.del(namespace + key);
            });

            // Decrement storage size
            String size = summary.get("size");
            if (size != null) {
                long entrySize = Integer.parseInt(size);
                if (entrySize > -1) {
                    decrementStorageSize(entrySize);
                }
            }
        }

        // Remove parameters
        redisExecutor.execute((RedisCallable<Long>) jedis -> {
            return jedis.del(getBytes(namespace + join(key, "params")));
        });
    }

    @Override
    public void release(String key) {
        if (config.getTargetMaxSizeMB() >= 0 && getStorageSize() > config.getTargetMaxSizeMB() * (1024 * 1024)) {
            remove(key);
        }
    }

    @Override
    protected void persistBlobs(String key, long sizeOfBlobs, List<Map<String, String>> blobInfos) {
        // TODO https://jira.nuxeo.com/browse/NXP-18050
        // Use a transaction

        Map<String, String> oldSummary = getSummary(key);

        // Update storage size
        long entrySize = -1;
        if (oldSummary != null) {
            String size = oldSummary.get("size");
            if (size != null) {
                entrySize = Long.parseLong(size);
            }
        }
        if (entrySize > -1) {
            incrementStorageSize(sizeOfBlobs - entrySize);
        } else {
            incrementStorageSize(sizeOfBlobs);
        }

        // Delete old blobs
        if (oldSummary != null) {
            String oldBlobCount = oldSummary.get("blobCount");
            deleteBlobInfos(key, oldBlobCount);
        }

        // Update entry size and blob count
        final Map<String, String> entrySummary = new HashMap<>();
        int blobCount = 0;
        if (blobInfos != null) {
            blobCount = blobInfos.size();
        }
        entrySummary.put("blobCount", String.valueOf(blobCount));
        entrySummary.put("size", String.valueOf(sizeOfBlobs));
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.hmset(namespace + key, entrySummary);
            return null;
        });

        // Set new blobs
        if (blobInfos != null) {
            for (int i = 0; i < blobInfos.size(); i++) {
                String blobInfoIndex = String.valueOf(i);
                Map<String, String> blobInfo = blobInfos.get(i);
                redisExecutor.execute((RedisCallable<String>) jedis -> {
                    return jedis.hmset(namespace + join(key, "blobs", blobInfoIndex), blobInfo);
                });
            }
        }
    }

    @Override
    public long getStorageSize() {
        return redisExecutor.execute((RedisCallable<Long>) jedis -> {
            String value = jedis.get(sizeKey);
            if (value == null) {
                return 0L;
            }
            return Long.parseLong(value);
        });
    }

    @Override
    protected void setStorageSize(final long newSize) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.set(sizeKey, "" + newSize);
            return null;
        });
    }

    @Override
    protected void incrementStorageSize(final long size) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.incrBy(sizeKey, size);
            return null;
        });
    }

    @Override
    protected void decrementStorageSize(final long size) {
        redisExecutor.execute((RedisCallable<Void>) jedis -> {
            jedis.decrBy(sizeKey, size);
            return null;
        });
    }

    @Override
    protected void removeAllEntries() {
        // TODO https://jira.nuxeo.com/browse/NXP-18050
    }

    protected Map<String, String> getSummary(String key) {
        return redisExecutor.execute((RedisCallable<Map<String, String>>) jedis -> {
            Map<String, String> summary = jedis.hgetAll(namespace + key);
            if (summary.isEmpty()) {
                return null;
            }
            return summary;
        });
    }

    protected void deleteBlobInfos(String key, String blobCountStr) {
        if (blobCountStr != null) {
            int blobCount = Integer.parseInt(blobCountStr);
            if (blobCount > 0) {
                for (int i = 0; i < blobCount; i++) {
                    String blobInfoIndex = String.valueOf(i);
                    redisExecutor.execute((RedisCallable<Long>) jedis -> {
                        return jedis.del(namespace + join(key, "blobs", blobInfoIndex));
                    });
                }
            }
        }
    }

    protected String join(String... fragments) {
        return StringUtils.join(fragments, ":");
    }

    protected byte[] getBytes(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    protected String getString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    protected byte[] serialize(Serializable value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(value);
            out.flush();
            out.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected Serializable deserialize(byte[] bytes) {
        try {
            InputStream bain = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bain);
            return (Serializable) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new NuxeoException(e);
        }
    }

    protected Map<byte[], byte[]> serialize(Map<String, Serializable> map) {
        Map<byte[], byte[]> serializedMap = new HashMap<>();
        for (String key : map.keySet()) {
            serializedMap.put(getBytes(key), serialize(map.get(key)));
        }
        return serializedMap;
    }

    protected Map<String, Serializable> deserialize(Map<byte[], byte[]> byteMap) {
        Map<String, Serializable> map = new HashMap<>();
        for (byte[] key : byteMap.keySet()) {
            map.put(getString(key), deserialize(byteMap.get(key)));
        }
        return map;
    }
}
