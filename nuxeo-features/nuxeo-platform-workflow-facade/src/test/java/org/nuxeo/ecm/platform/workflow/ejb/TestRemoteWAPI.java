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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestRemoteWAPI.java 22250 2007-07-10 13:33:10Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import org.nuxeo.ecm.platform.workflow.api.client.delegate.WAPIBusinessDelegate;

/**
 * Testing against a remote deployed facade.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestRemoteWAPI extends AbstractTestAPI {

    @Override
    protected void _initializeTest() throws Exception {
        wapi = WAPIBusinessDelegate.getWAPI();
        assertNotNull(wapi);

        NB_DEPLOYED_DEFS = 2;
        DEFINITION_1_ID = "1";
    }

    @Override
    protected void _unInitializeTest() throws Exception {
        wapi = null;
    }

}
