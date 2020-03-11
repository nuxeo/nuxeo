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
package org.nuxeo.lib.stream.computation.internals;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.RecordFilter;
import org.nuxeo.lib.stream.computation.RecordFilterChain;
import org.nuxeo.lib.stream.log.LogOffset;

/**
 * Chains multiple record filters.
 *
 * @since 11.1
 */
public class RecordFilterChainImpl implements RecordFilterChain {

    public static final RecordFilterChain NONE = new NoFilterChain();

    protected final Deque<RecordFilter> filters = new ArrayDeque<>();

    @Override
    public RecordFilterChain addFilter(RecordFilter filter) {
        Objects.requireNonNull(filter);
        filters.add(filter);
        return this;
    }

    @Override
    public Record beforeAppend(Record record) {
        for (Iterator<RecordFilter> iterator = filters.iterator(); record != null && iterator.hasNext();) {
            RecordFilter filter = iterator.next();
            record = filter.beforeAppend(record);
        }
        return record;
    }

    @Override
    public void afterAppend(Record record, LogOffset offset) {
        for (Iterator<RecordFilter> iterator = filters.iterator(); record != null && iterator.hasNext();) {
            RecordFilter filter = iterator.next();
            filter.afterAppend(record, offset);
        }
    }

    @Override
    public Record afterRead(Record record, LogOffset offset) {
        for (Iterator<RecordFilter> iterator = filters.descendingIterator(); record != null && iterator.hasNext();) {
            RecordFilter filter = iterator.next();
            record = filter.afterRead(record, offset);
        }
        return record;
    }
}
