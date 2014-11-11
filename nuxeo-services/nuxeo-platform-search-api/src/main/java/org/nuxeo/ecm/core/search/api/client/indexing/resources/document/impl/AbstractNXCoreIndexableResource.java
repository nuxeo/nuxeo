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
 * $Id: AbstractNXCoreIndexableResource.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.NXCoreIndexableResource;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract Nuxeo Core indexable resource implementation.
 * <p>
 * Indexable resource that wish to communicate with Nuxeo Core can extend this
 * class.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public abstract class AbstractNXCoreIndexableResource extends
        AbstractIndexableResource implements NXCoreIndexableResource {

    private static final Log log = LogFactory.getLog(AbstractNXCoreIndexableResource.class);

    private static final long serialVersionUID = -5078465836275084998L;

    protected CoreSession coreSession;

    protected final String docRepositoryName;

    protected final String sid;

    protected AbstractNXCoreIndexableResource() {
        sid = null;
        docRepositoryName = null;
    }

    protected AbstractNXCoreIndexableResource(String name,
            IndexableResourceConf configuration) {
        super(name, configuration);
        sid = null;
        docRepositoryName = null;
    }

    protected AbstractNXCoreIndexableResource(String name,
            IndexableResourceConf configuration, String sid,
            String docRepositoryName) {
        super(name, configuration);
        this.docRepositoryName = docRepositoryName;
        this.sid = sid;
    }

    public CoreSession getCoreSession() throws IndexingException {
        if (coreSession == null) {
            if (docRepositoryName != null && !docRepositoryName.equals("")) {
                if (sid != null) {
                    coreSession = CoreInstance.getInstance().getSession(sid);
                    if (coreSession != null) {
                        log.debug("Using an app level managed Nuxeo Core session...");
                        return coreSession;
                    } else {
                        throw new IndexingException(String.format(
                                "the managed session %s is invalid", sid));
                    }
                }
                try {
                    log.debug("Opening a new Session against Nuxeo Core");
                    RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                    coreSession = mgr.getRepository(docRepositoryName).open();
                } catch (Exception e) {
                    throw new IndexingException(
                            "Could not open new core session: "
                                    + e.getMessage(), e);
                }
            }
        }
        return coreSession;
    }

    /**
     * Close the core session only if it was not managed externally, i.e.
     * managedSessionId was not explicitly provided to the resource constructor
     */
    public void closeCoreSession() throws IndexingException {
        // Disconnect from Nuxeo Core
        try {
            if (sid == null) {
                // Session is not managed externally, we must really close it
                if (coreSession != null) {
                    log.debug("Closing Nuxeo Core connection..");
                    coreSession.cancel(); // read only operation here.
                    CoreInstance.getInstance().close(coreSession);
                    coreSession = null;
                }
            }
        } catch (ClientException ce) {
            throw new IndexingException(
                    "error while disconnection core session: "
                            + ce.getMessage(), ce);
        }
    }

    public String getDocRepositoryName() {
        return docRepositoryName;
    }

}
