/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.core.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.service.DirectoryUpdater;

/**
 * @since 5.7.1
 */
public class TestTargetPlatformComponent extends NXRuntimeTestCase {

    protected TargetPlatformService service;

    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.targetplatforms.core");
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");

        service = Framework.getService(TargetPlatformService.class);
        assertNotNull(service);

        String contrib = "OSGI-INF/test-targetplatforms-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);

        setUpContextFactory();
    }

    public void setUpContextFactory() throws NamingException {
        DataSource datasourceAutocommit = new SimpleDataSource(
                "jdbc:hsqldb:mem:memid", jdbcDriver.class.getName(), "SA", "") {
            @Override
            public Connection getConnection() throws SQLException {
                Connection con = super.getConnection();
                con.setAutoCommit(true);
                return con;
            }
        };
        NuxeoContainer.addDeepBinding("java:comp/env/jdbc/nxsqldirectory",
                datasourceAutocommit);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testPlatformRegistration() throws ClientException {
        TargetPlatform tp = service.getTargetPlatform("cap-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testPackageRegistration() throws ClientException {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());
    }

    @Test
    public void testOverrideDirectoryRegistration() throws Exception {
        assertEquals(DirectoryUpdater.DEFAULT_DIR,
                service.getOverrideDirectory());
        String contrib = "OSGI-INF/test-targetplatforms-dir-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);
        assertEquals("test", service.getOverrideDirectory());
    }

    @Test
    public void testPlatformRegistrationOverride() throws Exception {
        TargetPlatform tpOld = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tpOld);
        assertFalse(tpOld.isEnabled());

        TargetPlatform tpNew = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tpNew);
        assertTrue(tpNew.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);

        tpOld = service.getTargetPlatform("dm-5.3.0");
        assertNotNull(tpOld);
        assertTrue(tpOld.isEnabled());

        tpNew = service.getTargetPlatform("cap-5.9.2");
        assertNotNull(tpNew);
        assertFalse(tpNew.isEnabled());
    }

    @Test
    public void testPackageRegistrationOverride() throws Exception {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertTrue(tp.isEnabled());

        String contrib = "OSGI-INF/test-targetplatforms-override-contrib.xml";
        URL url = getClass().getClassLoader().getResource(contrib);
        deployTestContrib("org.nuxeo.targetplatforms.core", url);

        tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        assertFalse(tp.isEnabled());
    }
}