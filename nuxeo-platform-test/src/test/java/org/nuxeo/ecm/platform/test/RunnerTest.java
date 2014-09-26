/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.server.DataSource;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Tests features available in platform runner
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@LocalDeploy("org.nuxeo.ecm.core:local-resource-test.xml")
public class RunnerTest {

    @Inject protected DirectoryService dirs;
    @Inject protected CoreSession session;
    @Inject protected SchemaManager sm;

    @Test
    public void testLocalResource() {
       DocumentType dt = sm.getDocumentType("MyFolder");
       assertEquals("MyFolder", dt.getName());
    }

    @Test
    @Ignore
    public void testDatasourceBinding() throws Exception {
        DataSource ds = (DataSource)new InitialContext().lookup(DataSourceHelper.getDataSourceJNDIName("nxsqldirectory"));
        assertNotNull(ds);
    }

    @Test
    public void testServiceInjection() {
        assertNotNull(dirs);
    }

    @Test public void testCoreSessionInjection() {
        assertNotNull(session);
    }

}
