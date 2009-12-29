/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.datasource;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.AbstractRuntimeService;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.TransactionManagerConfiguration;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDataSourceComponent extends NXRuntimeTestCase {

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/h2";

    /** Property used in the datasource URL. */
    private static final String PROP_NAME = "ds.test.home";

    protected static void initJTA() throws Exception {
        NuxeoContainer.initTransactionManager(new TransactionManagerConfiguration());
        InitialContext context = new InitialContext();
        context.bind("java:comp/TransactionManager",
                NuxeoContainer.getTransactionManager());
        context.bind("java:comp/UserTransaction",
                NuxeoContainer.getUserTransaction());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        File dir = new File(DIRECTORY);
        FileUtils.deleteTree(dir);
        dir.mkdirs();
        ((AbstractRuntimeService) runtime).setProperty(PROP_NAME, dir.getPath());

        deployBundle("org.nuxeo.runtime.datasource");
    }

    public void testJNDIName() throws Exception {
        assertEquals("java:comp/env/jdbc/foo",
                DataSourceHelper.getDataSourceJNDIName("foo"));
    }

    protected static void checkDataSourceOk() throws Exception {
        DataSource ds = DataSourceHelper.getDataSource("foo");
        Connection conn = ds.getConnection();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT 123");
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(123, rs.getInt(1));
        st.close();
        conn.close();
    }

    public void testNonXANoTM() throws Exception {
        deployContrib("org.nuxeo.runtime.datasource.tests",
                "OSGI-INF/datasource-contrib.xml");
        checkDataSourceOk();
    }

    public void testNonXA() throws Exception {
        initJTA();
        deployContrib("org.nuxeo.runtime.datasource.tests",
                "OSGI-INF/datasource-contrib.xml");
        checkDataSourceOk();
    }

    public void testXANoTM() throws Exception {
        deployContrib("org.nuxeo.runtime.datasource.tests",
                "OSGI-INF/xadatasource-contrib.xml");
        try {
            DataSourceHelper.getDataSource("foo");
            fail("Should fail for XA with no TM");
        } catch (NamingException e) {
            // ok
        }
    }

    public void testXA() throws Exception {
        initJTA();
        deployContrib("org.nuxeo.runtime.datasource.tests",
                "OSGI-INF/xadatasource-contrib.xml");
        checkDataSourceOk();
    }

}
