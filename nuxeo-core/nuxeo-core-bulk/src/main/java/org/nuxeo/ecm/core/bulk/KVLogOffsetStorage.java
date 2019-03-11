package org.nuxeo.ecm.core.bulk;

import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogOffsetStorage;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

public class KVLogOffsetStorage implements LogOffsetStorage {

    private static final String PREFIX = "nuxeo.streams.keyOffset:";

    protected String kvName;

    public KVLogOffsetStorage() {
        this(BulkServiceImpl.BULK_KV_STORE_NAME);
    }

    public KVLogOffsetStorage(String kvName) {
        this.kvName = kvName;
    }

    @Override
    public void store(String key, LogOffset offset) {
        String offsetAsString = offset.partition().name() + ":" + offset.partition().partition() + ":"
                + offset.offset();
        getStore().put(PREFIX + key, offsetAsString);
    }

    @Override
    public LogOffset getMostRecentOffset(String key) {
        String offsetAsString = getStore().getString(PREFIX + key);
        if (offsetAsString == null) {
            return null;
        }
        String[] parts = offsetAsString.split(":");
        return new LogOffsetImpl(parts[0], Integer.parseInt(parts[1]), Long.parseLong(parts[2]));

    }

    protected KeyValueStore getStore() {
        return Framework.getService(KeyValueService.class).getKeyValueStore(kvName);
    }

}
