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
 *     matic
 */
package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.repository.RepositoryInitializationHandler;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;

/**
 * Initialize the contributed queues once the repository is first opened
 *
 * @author matic
 *
 */
public class QueuesInitializationHandler extends  RepositoryInitializationHandler {

    protected final QueueLocator locator;

    protected QueuesInitializationHandler(QueueLocator locator) {
        this.locator = locator;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.core.repository.RepositoryInitializationHandler#doInitializeRepository(org.nuxeo.ecm.core.api.CoreSession)
     */
    @Override
    public void doInitializeRepository(CoreSession session) throws ClientException {
        for (QueueManager<?> mgr : locator.getManagers()) {
            mgr.initialize();
        }

    }
}
