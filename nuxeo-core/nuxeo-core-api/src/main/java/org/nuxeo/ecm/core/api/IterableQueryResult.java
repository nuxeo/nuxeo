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
 * An iterable query result.
 * <p>
 * The {@link #close()} method MUST be called if the iterator is not consumed
 * completely, otherwise underlying resources will be leaked.
 *
 * @author Florent Guillaume
 */
public interface IterableQueryResult extends
        Iterable<Map<String, Serializable>> {

    /**
     * Closes the underlying resources held by the iterator.
     */
    void close();

    /**
     * Skips to a given point in the iterator.
     */
    void skipTo(long skipCount);

}
