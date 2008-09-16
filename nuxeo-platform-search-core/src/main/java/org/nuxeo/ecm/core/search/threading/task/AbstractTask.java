/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.threading.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.Task;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResources;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourcesFactory;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * 
 */
public abstract class AbstractTask implements Task {

    private static final Log log = LogFactory.getLog(AbstractTask.class);

    protected final static String PREFIXED_NAME = "NuxeoCoreIndexing_";

    protected final DocumentRef docRef;

    protected final String repositoryName;

    protected final ResolvedResources resources;

    protected AbstractTask(DocumentRef docRef, String repositoryName) {
        this.docRef = docRef;
        this.repositoryName = repositoryName;
        resources = null;
    }

    protected AbstractTask(ResolvedResources resources) {
        this.docRef = null;
        this.repositoryName = null;
        this.resources = resources;
    }

    public DocumentRef getDocumentRef() {
        return docRef;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    protected CoreSession getCoreSession() throws IndexingException {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof IndexingThread) {
            IndexingThread thread = (IndexingThread) currentThread;
            try {
                return thread.getCoreSession(repositoryName);
            } catch (Exception e) {
                throw new IndexingException(
                        "Unable to get a core session for repository name: "
                                + repositoryName);
            }
        } else {
            throw new IndexingException("Not in an IndexingThread");
        }
    }

    protected static IndexableResources computeResourcesFor(DocumentModel dm)
            throws IndexingException {
        return IndexableResourcesFactory.computeResourcesFor(dm);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        log.debug("Finalize...........................");
    }

}
