/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.directory.service;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.sql.SQLDirectory;
import org.nuxeo.ecm.directory.sql.SQLDirectoryTestCase;

/**
 * Test hot reload of registrations using mock directory factories
 *
 * @since 5.6
 */
public class TestDirectoryServiceRegistration extends SQLDirectoryTestCase {

    @Test
    public void testOverride() throws Exception {
        Directory dir = getDirectory("userDirectory");
        assertTrue(dir instanceof SQLDirectory);

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-directories-memory-factory.xml");

        deployContrib("org.nuxeo.ecm.directory.sql.tests",
                "test-directories-several-factories.xml");

        dir = getDirectory("userDirectory");
        assertTrue(dir instanceof MemoryDirectory);
    }

}
