/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.localconfiguration;

import org.nuxeo.ecm.core.api.DetachedAdapter;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Interface that must be implemented by other interface representing a local
 * configuration.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
public interface LocalConfiguration<T> extends DetachedAdapter {

    /**
     * Returns the related {@code DocumentRef} of this local configuration.
     */
    DocumentRef getDocumentRef();

    /**
     * Returns {@code true} if this {@code LocalConfiguration} accepted to be
     * merged with a parent configuration, {@code false} otherwise.
     */
    boolean canMerge();

    /**
     * Merge this {@code LocalConfiguration} with another one.
     */
    T merge(T other);

}
