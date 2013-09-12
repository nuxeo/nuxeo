/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.platform.importer.xml.parser;

import static org.junit.Assert.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 *
 */
public class TestDeployment extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestDeployment.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.platform.importer.core",
                "OSGI-INF/default-importer-service.xml");
        deployContrib("org.nuxeo.ecm.platform.scanimporter.test",
                "needed-contribution-for-factory-deployment.xml");
        deployContrib("nuxeo-importer-scan-xml-parser",
                "OSGI-INF/xml-importer-scan-config.xml");

    }

    @Test
    public void testImport() throws Exception {
        DefaultImporterService service = Framework.getLocalService(DefaultImporterService.class);
        assertNotNull(service);

        assertEquals("org.nuxeo.ecm.platform.importer.xml.parser.AdvancedScannedFileFactory", service.getDocModelFactoryClass().getName());
        assertEquals("", service.getSourceNodeClass().getName());

    }
}
