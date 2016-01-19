/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Map;

/**
 * An iterable query result based on a cursor.
 * <p>
 * The {@link #close()} method MUST be called when the query result is no more needed, otherwise underlying resources
 * will be leaked. There is no auto-closing at the end of the iteration.
 */
public interface IterableQueryResult extends Iterable<Map<String, Serializable>>, Closeable {

    /**
     * Closes the query result and releases the underlying resources held by the cursor.
     * <p>
     * This MUST be called when the query result is no more needed, otherwise underlying resources will be leaked. There
     * is no auto-closing at the end of the iteration.
     */
    @Override
    void close();

    /**
     * Indicates if the query result has not been closed
     *
     * @return
     * @deprecated since 8.1 (misspelled), use {@link #mustBeClosed} instead
     */
    @Deprecated
    boolean isLife();

    /**
     * Indicates if the query result must be closed (because it holds resources).
     *
     * @return {@code true} if the query result must be closed, {@code false} otherwise
     * @since 8.1
     */
    boolean mustBeClosed();

    /**
     * Gets the total size of the query result.
     * <p>
     * Note that this may be costly, and that some backends may not be able to do this operation, in which case
     * {@code -1} will be returned.
     *
     * @return the size, or {@code -1} for an unknown size
     */
    long size();

    /**
     * Gets the current position in the iterator.
     * <p>
     * Positions start at {@code 0}.
     *
     * @return the position
     */
    long pos();

    /**
     * Skips to a given position in the iterator.
     * <p>
     * Positions start at {@code 0}.
     */
    void skipTo(long pos);

}
