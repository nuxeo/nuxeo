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
package org.nuxeo.targetplatforms.io.test;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hsqldb.jdbcDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.targetplatforms.api.TargetPackage;
import org.nuxeo.targetplatforms.api.TargetPackageInfo;
import org.nuxeo.targetplatforms.api.TargetPlatform;
import org.nuxeo.targetplatforms.api.TargetPlatformInfo;
import org.nuxeo.targetplatforms.api.TargetPlatformInstance;
import org.nuxeo.targetplatforms.api.service.TargetPlatformService;
import org.nuxeo.targetplatforms.core.service.DirectoryUpdater;
import org.nuxeo.targetplatforms.io.JSONExporter;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.runtime.jtajca", "org.nuxeo.ecm.core.schema",
        "org.nuxeo.ecm.core", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.targetplatforms.core",
        "org.nuxeo.targetplatforms.io" })
@LocalDeploy("org.nuxeo.targetplatforms.io:OSGI-INF/test-targetplatforms-contrib.xml")
public class TestJsonExporter {

    @Inject
    protected TargetPlatformService service;

    @Before
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

    @After
    public void tearDown() throws Exception {
        // remove all entries from directory
        new DirectoryUpdater(DirectoryUpdater.DEFAULT_DIR) {
            @Override
            public void run(DirectoryService service, Session session)
                    throws ClientException {
                for (DocumentModel doc : session.getEntries()) {
                    session.deleteEntry(doc.getId());
                }
            }
        }.run();
    }

    protected void checkJsonEquals(String filepath, ByteArrayOutputStream out)
            throws Exception {
        String path = FileUtils.getResourcePathFromContext(filepath);
        String expected = IOUtils.toString(new FileInputStream(path), "UTF-8");
        String actual = out.toString();
        try {
            JSONAssert.assertEquals(expected, actual, true);
        } catch (AssertionError e) {
            // System.err.println(actual);
            throw e;
        }
    }

    @Test
    public void testTargetPlatformExport() throws Exception {
        TargetPlatform tp = service.getTargetPlatform("cap-5.8");
        assertNotNull(tp);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tp, out, true);
        checkJsonEquals("target-platform-export.json", out);
    }

    @Test
    public void testTargetPlatformInfoExport() throws Exception {
        TargetPlatformInfo tpi = service.getTargetPlatformInfo("cap-5.8");
        assertNotNull(tpi);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tpi, out, true);
        checkJsonEquals("target-platform-info-export.json", out);
    }

    @Test
    public void testTargetPlatformInstanceExport() throws Exception {
        TargetPlatformInstance tpi = service.getTargetPlatformInstance(
                "cap-5.8", null);
        assertNotNull(tpi);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tpi, out, true);
        checkJsonEquals("target-platform-instance-export.json", out);
        tpi = service.getTargetPlatformInstance("cap-5.8",
                Arrays.asList(new String[] { "nuxeo-dm-5.8" }));
        assertNotNull(tpi);
        out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tpi, out, true);
        checkJsonEquals("target-platform-instance-with-packages-export.json",
                out);
    }

    @Test
    public void testTargetPackageExport() throws Exception {
        TargetPackage tp = service.getTargetPackage("nuxeo-dm-5.8");
        assertNotNull(tp);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tp, out, true);
        checkJsonEquals("target-package-export.json", out);
    }

    @Test
    public void testTargetPackageInfoExport() throws Exception {
        TargetPackageInfo tpi = service.getTargetPackageInfo("nuxeo-dm-5.8");
        assertNotNull(tpi);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tpi, out, true);
        checkJsonEquals("target-package-info-export.json", out);
    }

    @Test
    public void testTargetPlatformsExport() throws Exception {
        List<TargetPlatform> tps = service.getAvailableTargetPlatforms(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportToJson(tps, out, true);
        checkJsonEquals("target-platforms-export.json", out);
    }

    @Test
    public void testTargetPlatformsInfosExport() throws Exception {
        List<TargetPlatformInfo> tps = service.getAvailableTargetPlatformsInfo(null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONExporter.exportInfosToJson(tps, out, true);
        checkJsonEquals("target-platform-infos-export.json", out);
    }

}
