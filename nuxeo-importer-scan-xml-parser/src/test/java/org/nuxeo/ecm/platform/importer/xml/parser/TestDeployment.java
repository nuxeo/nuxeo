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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 5.7.3
 *
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({
        "org.nuxeo.ecm.core.api",
        "org.nuxeo.ecm.core",
        "org.nuxeo.ecm.core.schema",
        "org.nuxeo.ecm.platform.importer.core:OSGI-INF/default-importer-service.xml",
        "nuxeo-importer-scan-xml-parser-test:OSGI-INF/xml-importer-scan-config-without-requires.xml" })
public class TestDeployment {

    private static final Log log = LogFactory.getLog(TestDeployment.class);

    @Test
    public void testImport() throws Exception {
        DefaultImporterService service = Framework.getLocalService(DefaultImporterService.class);
        assertNotNull(service);

        assertEquals(
                "org.nuxeo.ecm.platform.importer.xml.parser.AdvancedScannedFileFactory",
                service.getDocModelFactoryClass().getName());
        assertEquals(
                "org.nuxeo.ecm.platform.importer.xml.parser.XMLFileSourceNode",
                service.getSourceNodeClass().getName());

    }
}
