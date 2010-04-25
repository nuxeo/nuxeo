/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.search.backend.core;

import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;

/*
 * @author Florent Guillaume
 */
public class TestCoreSearchBackendSQL extends CoreSearchBackendTestCase {

    @Override
    public void deployRepository() throws Exception {
        DatabaseHelper.DATABASE.setUp();
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                DatabaseHelper.DATABASE.getDeploymentContrib());
    }

    @Override
    public void undeployRepository() throws Exception {
        DatabaseHelper.DATABASE.tearDown();
    }
}
