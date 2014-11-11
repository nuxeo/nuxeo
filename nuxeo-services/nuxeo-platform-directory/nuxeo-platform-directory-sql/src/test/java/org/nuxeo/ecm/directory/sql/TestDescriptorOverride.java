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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.directory.sql;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.nuxeo.ecm.directory.Directory;

public class TestDescriptorOverride extends SQLDirectoryTestCase {

    @Test
    public void testOverride() throws Exception {
        Directory dir = getDirectory("userDirectory");
        SQLDirectory sqlDir = (SQLDirectory) dir;

        SQLDirectoryDescriptor config = sqlDir.getConfig();

        assertEquals("always", config.getCreateTablePolicy());
        assertEquals(100, config.getQuerySizeLimit());
        assertFalse(config.isAutoincrementIdField());
        assertTrue(config.isComputeMultiTenantId());
        Assert.assertNull(config.cacheEntryName);
        Assert.assertNull(config.cacheEntryWithoutReferencesName);
        assertEquals("test-users.csv", config.getDataFileName());

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-sql-directories-override-bundle.xml");

        dir = getDirectory("userDirectory");
        sqlDir = (SQLDirectory) dir;
        config = sqlDir.getConfig();

        // override
        assertEquals("never", config.getCreateTablePolicy());
        assertEquals(123, config.getQuerySizeLimit());
        assertTrue(config.isAutoincrementIdField());
        assertFalse(config.isComputeMultiTenantId());
        Assert.assertEquals("override-entry-cache", config.cacheEntryName);
        Assert.assertEquals("override-entry-cache-wo-ref",
                config.cacheEntryWithoutReferencesName);

        // inherit
        assertEquals("test-users.csv", config.getDataFileName());
        assertEquals(1, config.getTableReferences().length);
    }

}
