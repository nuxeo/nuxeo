/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: NXCoreIndexableResource.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;

/**
 * Nuxeo core indexable resource interface.
 * <p>
 * Extends base indexable resource interface to add Nuxeo Core speficic API that
 * might ease the definition of new indexable resources that needs to interract
 * with a Nuxeo Core instance.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface NXCoreIndexableResource extends IndexableResource {

    /**
     * Returns a Nuxeo core session.
     * <p>
     * This is the implementation job to either authenticate or reuse a new
     * existing session. Check implementation for details.
     *
     * @return a initialized <code>CoreSession</code> instance.
     * @throws IndexingException
     */
    CoreSession getCoreSession() throws IndexingException;

    /**
     * Explicitly closes the inner core session.
     * <p>
     * It is the caller's responsibility to close the connection for
     * optimization sake.
     *
     * @throws IndexingException
     */
    void closeCoreSession() throws IndexingException;

    /**
     * Returns the repository identifier where the document is stored.
     *
     * @return the repository identifier
     */
    String getDocRepositoryName();

}
