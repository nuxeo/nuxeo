/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.marklogic;

import static org.nuxeo.ecm.core.api.ScrollResultImpl.emptyResult;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.ScrollResultImpl;

/**
 * A cursor service which holds cursors on DB in order to perform scroll operations.
 *
 * @param <C> The cursor type.
 * @param <O> The cursor item type.
 * @since 9.1
 */
public class CursorService<C, O> {

    private static final Log log = LogFactory.getLog(CursorService.class);

    protected Map<String, CursorResult<C, O>> cursorResults = new ConcurrentHashMap<>();

    public void checkForTimedOutScroll() {
        cursorResults.entrySet().stream().forEach(e -> isScrollTimedOut(e.getKey(), e.getValue()));
    }

    protected boolean isScrollTimedOut(String scrollId, CursorResult<C, O> cursorResult) {
        if (cursorResult.timedOut()) {
            if (unregisterCursor(scrollId)) {
                log.warn("Scroll '" + scrollId + "' timed out");
            }
            return true;
        }
        return false;
    }

    /**
     * Registers the input {@link C} and generates a new <code>scrollId</code> to associate with.
     *
     * @return the scrollId associated to the cursor.
     */
    public String registerCursor(C cursor, int batchSize, int keepAliveSeconds) {
        return registerCursorResult(new CursorResult<>(cursor, batchSize, keepAliveSeconds));
    }

    /**
     * Registers the input {@link C} associated to the input <code>scrollId</code>.
     *
     * @return the scrollId associated to the cursor.
     */
    public String registerCursor(String scrollId, C cursor, int batchSize, int keepAliveSeconds) {
        return registerCursorResult(scrollId, new CursorResult<>(cursor, batchSize, keepAliveSeconds));
    }

    /**
     * Registers the input {@link CursorResult} and generates a new <code>scrollId</code> to associate with.
     *
     * @return the scrollId associated to the cursor result.
     */
    public String registerCursorResult(CursorResult<C, O> cursorResult) {
        String scrollId = UUID.randomUUID().toString();
        return registerCursorResult(scrollId, cursorResult);
    }

    /**
     * Registers the input {@link CursorResult} associated to the input <code>scrollId</code>.
     *
     * @return the scrollId associated to the cursor result.
     */
    public String registerCursorResult(String scrollId, CursorResult<C, O> cursorResult) {
        cursorResults.put(scrollId, cursorResult);
        return scrollId;
    }

    /**
     * Unregisters cursor associated to the input <code>scrollId</code>.
     *
     * @param scrollId The scoll id of {@link CursorResult} to unregister
     * @return Whether or not the cursor was unregistered.
     */
    public boolean unregisterCursor(String scrollId) {
        CursorResult<C, O> cursorResult = cursorResults.remove(scrollId);
        if (cursorResult != null) {
            cursorResult.close();
            return true;
        }
        return false;
    }

    /**
     * @return the next batch of cursor associated to the input <code>scrollId</code>
     */
    public ScrollResult scroll(String scrollId, Function<O, String> idExtractor) {
        CursorResult<C, O> cursorResult = cursorResults.get(scrollId);
        if (cursorResult == null) {
            throw new NuxeoException("Unknown or timed out scrollId");
        } else if (isScrollTimedOut(scrollId, cursorResult)) {
            throw new NuxeoException("Timed out scrollId");
        }
        cursorResult.touch();
        List<String> ids = new ArrayList<>(cursorResult.getBatchSize());
        synchronized (cursorResult) {
            if (!cursorResult.hasNext()) {
                unregisterCursor(scrollId);
                return emptyResult();
            }
            while (ids.size() < cursorResult.getBatchSize()) {
                if (!cursorResult.hasNext()) {
                    // Don't unregister cursor here because we don't want scroll API to throw an exception during next
                    // call as it's a legitimate case - but close cursor
                    cursorResult.close();
                    break;
                } else {
                    O obj = cursorResult.next();
                    String id = idExtractor.apply(obj);
                    if (id == null) {
                        log.error("Got a document without id: " + obj);
                    } else {
                        ids.add(id);
                    }
                }
            }
        }
        return new ScrollResultImpl(scrollId, ids);
    }

    /**
     * Clear and close all cursors owned by this service.
     */
    public void clear() {
        Iterator<CursorResult<C, O>> values = cursorResults.values().iterator();
        while (values.hasNext()) {
            values.next().close();
            values.remove();
        }
    }

    public static class CursorResult<C, O> implements Iterator<O>, Closeable {

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

}
