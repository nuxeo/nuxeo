/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Indicates if the query result has not been closed
     * 
     * @return
     */
    boolean isLife();
    
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
