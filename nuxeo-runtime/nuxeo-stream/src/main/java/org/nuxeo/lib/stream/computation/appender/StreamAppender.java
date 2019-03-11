/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */

package org.nuxeo.lib.stream.computation.appender;

import java.time.Duration;
import java.util.function.Function;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogOffsetStorage;
import org.nuxeo.lib.stream.log.internals.CloseableLogAppender;

/**
 * @since 11.1
 */
public class StreamAppender<M extends Record> implements CloseableLogAppender<M> {

    protected LogOffsetStorage storage;

    protected Function<M, String> extractor;

    protected LogAppender<M> delegate;

    public StreamAppender(LogAppender<M> logAppender, LogOffsetStorage storage, Function<M, String> extractor) {
        this.delegate = logAppender;
        this.extractor = extractor == null ? (record) -> record.getKey() : extractor;
        this.storage = storage;
    }

    @Override
    public LogOffset append(int partition, M message) {
        LogOffset offset = delegate.append(partition, message);
        if (storage != null) {
            storage.store(extractor.apply(message), offset);
        }
        return offset;
    }

    @Override
    public void close() {
        if (delegate instanceof CloseableLogAppender<?>) {
            ((CloseableLogAppender<?>) delegate).close();
        }
    }

    @Override
    public boolean closed() {
        return delegate.closed();
    }

    @Override
    public Codec<M> getCodec() {
        return delegate.getCodec();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean waitFor(LogOffset offset, String group, Duration timeout) throws InterruptedException {
        return delegate.waitFor(offset, group, timeout);
    }

}
