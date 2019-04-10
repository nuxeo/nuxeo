/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.impl;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.impl.CallContextImpl;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoRepository;

/**
 * Test the high-level session using a local connection.
 */
public class TestNuxeoSessionLocal extends NuxeoSessionTestCase {

    @Override
    public void setUpCmisSession() throws Exception {
        boolean objectInfoRequired = true; // for tests
        CallContextImpl context = new CallContextImpl(
                CallContext.BINDING_LOCAL, getRepositoryId(),
                objectInfoRequired);
        context.put(CallContext.USERNAME, USERNAME);
        context.put(CallContext.PASSWORD, PASSWORD);
        NuxeoRepository repository = new NuxeoRepository(getRepositoryId(),
                getRootFolderId());
        session = new NuxeoSession(getCoreSession(), repository, context);
    }

    @Override
    public void tearDownCmisSession() throws Exception {
        session = null;
    }

}
