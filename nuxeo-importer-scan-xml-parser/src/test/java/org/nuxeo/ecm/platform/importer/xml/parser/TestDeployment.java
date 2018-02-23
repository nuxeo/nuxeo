/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Benjamin JALON<bjalon@nuxeo.com>
 */
package org.nuxeo.ecm.platform.importer.xml.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.importer.core:OSGI-INF/default-importer-service.xml")
@Deploy("nuxeo-importer-scan-xml-parser-test:OSGI-INF/xml-importer-scan-config-without-requires.xml")
public class TestDeployment {

    @Test
    public void testImport() {
        DefaultImporterService service = Framework.getService(DefaultImporterService.class);
        assertNotNull(service);

        assertEquals("org.nuxeo.ecm.platform.importer.xml.parser.AdvancedScannedFileFactory",
                service.getDocModelFactoryClass().getName());
        assertEquals("org.nuxeo.ecm.platform.importer.xml.parser.XMLFileSourceNode",
                service.getSourceNodeClass().getName());

    }
}
