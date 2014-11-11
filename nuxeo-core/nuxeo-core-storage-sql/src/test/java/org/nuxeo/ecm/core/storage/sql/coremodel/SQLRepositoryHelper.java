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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import org.nuxeo.ecm.core.storage.sql.SQLBackendHelper;

/**
 * Helper to set up and tear down a test database.
 * <p>
 * This can be used also to use another test database than Derby, for instance
 * PostgreSQL.
 *
 * @author Florent Guillaume
 */
public class SQLRepositoryHelper extends SQLBackendHelper {

    public static String getDeploymentContrib() {
        return String.format("OSGI-INF/test-repo-repository-%s-contrib.xml",
                DATABASE.toString().toLowerCase());
    }

}
