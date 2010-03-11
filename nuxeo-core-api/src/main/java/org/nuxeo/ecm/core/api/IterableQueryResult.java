/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Map;

/**
 * An iterable query result based on a cursor.
 * <p>
 * The {@link #close()} method MUST be called when the query result is no more
 * needed, otherwise underlying resources will be leaked. There is no
 * auto-closing at the end of the iteration.
 */
public interface IterableQueryResult extends
        Iterable<Map<String, Serializable>> {

    /**
     * Closes the query result and releases the underlying resources held by the
     * cursor.
     * <p>
     * This MUST be called when the query result is no more needed, otherwise
     * underlying resources will be leaked. There is no auto-closing at the end
     * of the iteration.
     */
    void close();

    /**
     * Gets the total size of the query result.
     * <p>
     * Note that this may be costly, and that some backends may not be able to
     * do this operation, in which case {@code -1} will be returned.
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
