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
 *     anguenot
 *
 * $Id: IndexingThread.java 29925 2008-02-06 18:56:27Z tdelprat $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.nxcore;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;

/**
 * Indexing thread interface.
 *
 * @see org.nuxeo.ecm.core.search.threading.IndexingThreadImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface IndexingThread extends Runnable {

    /**
     * Returns the core session managed by this thread.
     * <p>
     * The initialization is done in a lazy way along with the JAAS login
     * initialization.
     * <p>
     * If the current managed session is not against the
     * <code>repositoryName</code> given as a parameter then a the actual
     * connection will be removed and replaced by a new one one the requested
     * repository name.
     *
     * @param repositoryName : the Nuxeo core repository name.
     * @return a connected <code>CoreSession</code> instance.
     * @throws Exception
     */
    CoreSession getCoreSession(String repositoryName) throws Exception;

    /**
     * Returns the search service session this indexing thread is bound against.
     * <p>
     * Search service session can't be null.
     *
     * @return a search service session.
     * @throws Exception : if no session can't be retrieved.
     */
    SearchServiceSession getSearchServiceSession() throws Exception;

    Boolean canBeRecycled();

    void markForRecycle();

}
