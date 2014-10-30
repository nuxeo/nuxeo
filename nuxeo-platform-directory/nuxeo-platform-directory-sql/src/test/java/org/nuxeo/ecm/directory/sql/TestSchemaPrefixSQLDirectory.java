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
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;

public class TestSchemaPrefixSQLDirectory extends SQLDirectoryTestCase {

    private static final String SCHEMA = "user";

    @Test
    public void testSchemaWithPrefix() throws Exception {
        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-schema-prefix.xml");
        Session session = getSession("userDirectory");
        try {
            DocumentModel dm = session.getEntry("user_1");
            assertEquals("user_1", dm.getProperty(SCHEMA, "username"));
        } finally {
            session.close();
        }
    }

}
