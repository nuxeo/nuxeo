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
package org.nuxeo.lib.stream.computation;

import java.util.Map;

import org.nuxeo.lib.stream.log.LogOffset;

/**
 * Record filtering enables to modify/skip record while it is append or read from a stream. Filter can also be used to add custom code.
 *
 * @since 11.1
 */
public interface RecordFilter {

    /**
     * Initialiaze the filter.
     */
    default void init(Map<String, String> options) {
        // nothing
    }

    /**
     * Called before appending a record to a stream. This hook enables to change the record or to skip it when returning
     * null.
     *
     * @param record the record that will be appended to a stream
     */
    default Record beforeAppend(Record record) {
        return record;
    }

    /**
     * Called after a record is appended to a stream.
     *
     * @param record the written record
     * @param offset the record's offset
     */
    default void afterAppend(Record record, LogOffset offset) {
        // nothing
    }

    /**
     * Called after reading a record. This hook enables to change the record or to skip it when returning null.
     *
     * @param record the record
     * @param offset the offset of the record
     */
    default Record afterRead(Record record, LogOffset offset) {
        return record;
    }
}
