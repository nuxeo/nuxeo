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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/**
 * A cursor result which holds a DB cursor and additional information to scroll this DB cursor.
 * 
 * @param <C> The cursor type.
 * @param <O> The cursor item type.
 * @since 9.1
 */
public class CursorResult<C, O> implements Iterator<O>, Closeable {

    protected C cursor;

    protected final int batchSize;

    protected final int keepAliveSeconds;

    protected long lastCallTimestamp;

    public CursorResult(C cursor, int batchSize, int keepAliveSeconds) {
        this.cursor = cursor;
        this.batchSize = batchSize;
        this.keepAliveSeconds = keepAliveSeconds;
        this.lastCallTimestamp = System.currentTimeMillis();
    }

    public C getCursor() {
        return cursor;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void touch() {
        lastCallTimestamp = System.currentTimeMillis();
    }

    public boolean timedOut() {
        long now = System.currentTimeMillis();
        return now - lastCallTimestamp > keepAliveSeconds * 1000;
    }

    @Override
    public boolean hasNext() {
        if (cursor == null) {
            return false;
        } else if (cursor instanceof Iterator) {
            return ((Iterator) cursor).hasNext();
        }
        throw new IllegalStateException(
                "Cursor doesn't implement Iterator interface, you must provide an implementation of #hasNext and #next method");
    }

    @Override
    @SuppressWarnings("unchecked")
    public O next() {
        if (cursor instanceof Iterator) {
            return ((Iterator<O>) cursor).next();
        }
        throw new IllegalStateException(
                "Cursor doesn't implement Iterator interface, you must provide an implementation of #hasNext and #next method");
    }

    /**
     * CAUTION: if your cursor doesn't implement {@link Closeable}, we just set the field to null
     */
    @Override
    public void close() {
        if (cursor instanceof Closeable) {
            try {
                ((Closeable) cursor).close();
            } catch (IOException e) {
                throw new NuxeoException("Unable to close cursor", e);
            }
        }
        cursor = null;
    }

}
