/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.lib.stream.computation.Record;

/**
 * Helper class to handle {@link Record} in bulk services.
 *
 * @since 10.2
 */
public class BulkRecords {

    protected static final String KEY_SEPARATOR = ":";

    protected static final String VALUE_SEPARATOR = "_";

    private BulkRecords() {
        // no instance allowed
    }

    /**
     * @return a new {@link Record} containing document ids respecting bulk format
     */
    public static Record of(String commandId, long currentCount, List<String> documentIds) {
        return of(commandId, currentCount, String.join(VALUE_SEPARATOR, documentIds));
    }

    /**
     * @return a new {@link Record} containing a string respecting bulk format
     */
    public static Record of(String commandId, long currentCount, String value) {
        String key = commandId + KEY_SEPARATOR + currentCount;
        return Record.of(key, value.getBytes(UTF_8));
    }

    /**
     * @return the command id extracted from {@link Record}
     */
    public static String commandIdFrom(Record record) {
        String key = record.getKey();
        return key.split(KEY_SEPARATOR)[0];
    }

    /**
     * @return the document ids extracted from {@link Record}
     */
    public static List<String> docIdsFrom(Record record) {
        String value = new String(record.getData(), UTF_8);
        return Arrays.asList(value.split(VALUE_SEPARATOR));
    }

    /**
     * @return the count extracted from {@link Record}
     */
    public static long countFrom(Record record) {
        String key = record.getKey();
        return Long.parseLong(key.split(KEY_SEPARATOR)[1]);
    }

    /**
     * @return the data as string extracted from {@link Record}
     */
    public static String dataFrom(Record record) {
        return new String(record.getData(), UTF_8);
    }
}
