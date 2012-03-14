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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.io.plugins.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.hsqldb.jdbcDriver;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.directory.sql.SimpleDataSource;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.platform.forms.layout.io.JSONLayoutExporter;
import org.nuxeo.ecm.platform.forms.layout.service.WebLayoutManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 * @since 5.5
 */
public class TestLayoutExport extends NXRuntimeTestCase {

    protected static final String TEST_CATEGORY = "standalone";

    protected LayoutStore service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // layout deps
        deployBundle("org.nuxeo.ecm.platform.forms.layout.core");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.client",
                "OSGI-INF/layouts-framework.xml");
        deployBundle("org.nuxeo.ecm.platform.forms.layout.io.plugins");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.io.plugins.tests",
                "layouts-test-contrib.xml");

        // other deps
        setUpContextFactory();
        // deploy directory service
        deployBundle("org.nuxeo.ecm.directory");
        deployBundle("org.nuxeo.ecm.directory.sql");
        // deploy schemas
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/CoreExtensions.xml");
        // default dirs
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
        deployContrib("org.nuxeo.ecm.platform.forms.layout.io.plugins.tests",
                "test-directories-contrib.xml");

        service = Framework.getService(LayoutStore.class);
        assertNotNull(service);
    }

    @Override
    public void tearDown() throws Exception {
        NuxeoContainer.uninstallNaming();
        super.tearDown();
    }

    public static void setUpContextFactory() throws NamingException {
        DataSource datasourceAutocommit = new SimpleDataSource(
                "jdbc:hsqldb:mem:memid", jdbcDriver.class.getName(), "SA", "") {
            @Override
            public Connection getConnection() throws SQLException {
                Connection con = super.getConnection();
                con.setAutoCommit(true);
                return con;
            }
        };
        NuxeoContainer.installNaming();
        NuxeoContainer.addDeepBinding("java:comp/env/jdbc/nxsqldirectory",
                datasourceAutocommit);
    }

    public void testLayoutDefinitionExport() throws Exception {
        LayoutDefinition layoutDef = service.getLayoutDefinition(
                WebLayoutManager.JSF_CATEGORY, "dublincore");
        assertNotNull(layoutDef);

        check(layoutDef, "en");
        check(layoutDef, "fr");
        check(layoutDef, null);
    }

    protected void check(LayoutDefinition layoutDef, String lang)
            throws Exception {
        LayoutConversionContext ctx = new LayoutConversionContext(lang, null);
        List<LayoutDefinitionConverter> layoutConverters = service.getLayoutConverters(TEST_CATEGORY);
        for (LayoutDefinitionConverter conv : layoutConverters) {
            layoutDef = conv.getLayoutDefinition(layoutDef, ctx);
        }
        List<WidgetDefinitionConverter> widgetConverters = service.getWidgetConverters(TEST_CATEGORY);

        String langFilePath = lang;
        if (langFilePath == null) {
            langFilePath = "nolang";
        }
        File file = File.createTempFile("layout-export-" + langFilePath,
                ".json");
        FileOutputStream out = new FileOutputStream(file);
        JSONLayoutExporter.export(WebLayoutManager.JSF_CATEGORY, layoutDef,
                ctx, widgetConverters, out);

        InputStream written = new FileInputStream(file);
        InputStream expected = new FileInputStream(
                FileUtils.getResourcePathFromContext("layout-export-"
                        + langFilePath + ".json"));

        String expectedString = FileUtils.read(expected);
        String writtenString = FileUtils.read(written);
        assertTrue(FileUtils.areFilesContentEquals(expectedString,
                writtenString));
    }

}
