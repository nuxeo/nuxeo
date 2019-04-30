/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.work;

import java.util.EnumSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.RecordFilter;
import org.nuxeo.lib.stream.log.LogOffset;

/**
 * Base for filter that saves long record's value in an alternate storage. The record is then marked with an internal
 * flag and contains an empty value.
 *
 * @since 11.1
 */
public abstract class BaseOverflowRecordFilter implements RecordFilter {
    private static final Log log = LogFactory.getLog(BaseOverflowRecordFilter.class);

    public static final String STORE_NAME_OPTION = "storeName";

    public static final String DEFAULT_STORE_NAME = "default";

    public static final String STORE_TTL_OPTION = "storeTTLSeconds";

    public static final int DEFAULT_STORE_TTL = 3600;

    public static final String THRESHOLD_SIZE_OPTION = "thresholdSize";

    public static final int DEFAULT_THRESHOLD_SIZE = 1_000_000;

    public static final String PREFIX_OPTION = "prefix";

    public static final String DEFAULT_PREFIX = "bigRecord:";

    protected String prefix;

    protected int thresholdSize;

    protected int storeTTLSeconds;

    protected String storeName;

    /**
     * Sets the value associated to the key.
     */
    protected abstract void storeValue(String key, byte[] data);

    /**
     * Fetches a value previously stored by {@link #storeValue(String, byte[])}
     *
     * @return the value, or {@code null} if there is no value
     */
    protected abstract byte[] fetchValue(String key);

    @Override
    public void init(Map<String, String> options) {
        storeName = options.getOrDefault(STORE_NAME_OPTION, DEFAULT_STORE_NAME);
        prefix = options.getOrDefault(PREFIX_OPTION, DEFAULT_PREFIX);
        thresholdSize = parseIntOrDefault(options.get(THRESHOLD_SIZE_OPTION), DEFAULT_THRESHOLD_SIZE);
        storeTTLSeconds = parseIntOrDefault(options.get(STORE_TTL_OPTION), DEFAULT_STORE_TTL);
    }

    protected int parseIntOrDefault(String valueAsString, int defaultValue) {
        if (StringUtils.isEmpty(valueAsString)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(valueAsString);
        } catch (NumberFormatException e) {
            log.error("Invalid number for RecordFilter option: " + valueAsString, e);
            return defaultValue;
        }
    }

    @Override
    public Record beforeAppend(Record record) {
        if (record.data == null || record.data.length <= getThresholdSize()) {
            return record;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Record: %s overflow value of size: %d", record.key, record.data.length));
        }
        EnumSet<Record.Flag> flags = EnumSet.copyOf(record.flags);
        flags.add(Record.Flag.EXTERNAL_VALUE);
        storeValue(getUniqRecordKey(record), record.data);
        return new Record(record.key, null, record.watermark, flags);
    }

    protected String getUniqRecordKey(Record record) {
        // this is needed to support different records using an identical key
        return String.format("%s:%d", record.key, record.watermark);
    }

    @Override
    public Record afterRead(Record record, LogOffset offset) {
        if (record.flags.contains(Record.Flag.EXTERNAL_VALUE) && (record.data == null || record.data.length == 0)) {
            byte[] value = fetchValue(getUniqRecordKey(record));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Record: %s retrieve value of size: %d", record.key,
                        record.data == null ? 0 : record.data.length));
            }
            if (value == null || value.length == 0) {
                log.error(String.format("Record %s offset %s value not found, the record is lost, skipping",
                        record.toString(), offset));
                return null;
            }
            EnumSet<Record.Flag> flags = record.flags;
            flags.remove(Record.Flag.EXTERNAL_VALUE);
            return new Record(record.key, value, record.watermark, flags);
        }
        return record;
    }

    public int getThresholdSize() {
        return thresholdSize;
    }

    public void setThresholdSize(int thresholdSize) {
        this.thresholdSize = thresholdSize;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public int getStoreTTLSeconds() {
        return storeTTLSeconds;
    }

    public void setStoreTTL(int storeTTLSeconds) {
        this.storeTTLSeconds = storeTTLSeconds;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    protected String getPrefixedKey(String recordKey) {
        return getPrefix() + recordKey;
    }
}
