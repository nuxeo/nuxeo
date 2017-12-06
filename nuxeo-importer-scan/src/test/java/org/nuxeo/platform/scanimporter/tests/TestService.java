/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.platform.scanimporter.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileMappingDescriptor;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperComponent;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.scanimporter", "OSGI-INF/importerservice-framework.xml");
    }

    @Test
    public void testServiceLookup() {
        ScannedFileMapperService sfms = Framework.getService(ScannedFileMapperService.class);
        assertNotNull(sfms);
    }

    @Test
    public void testServiceContrib() throws Exception {

        ScannedFileMapperService sfms = Framework.getService(ScannedFileMapperService.class);
        ScanFileMappingDescriptor desc = ((ScannedFileMapperComponent) sfms).getMappingDesc();

        assertNull(desc);
        pushInlineDeployments("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-contrib0.xml");

        sfms = Framework.getService(ScannedFileMapperService.class);
        desc = ((ScannedFileMapperComponent) sfms).getMappingDesc();
        assertNotNull(desc);

        assertEquals(2, desc.getFieldMappings().size());
        assertEquals(1, desc.getBlobMappings().size());

        assertEquals("xpath1", desc.getFieldMappings().get(0).getSourceXPath());
        assertEquals("attr1", desc.getFieldMappings().get(0).getSourceAttribute());
        assertEquals("string", desc.getFieldMappings().get(0).getTargetType());
        assertEquals("dc:title", desc.getFieldMappings().get(0).getTargetXPath());;

        assertEquals("xpath3", desc.getBlobMappings().get(0).getSourceXPath());
        assertEquals("filePath1", desc.getBlobMappings().get(0).getSourcePathAttribute());
        assertEquals("fileName1", desc.getBlobMappings().get(0).getSourceFilenameAttribute());
        assertEquals("file:content", desc.getBlobMappings().get(0).getTargetXPath());

    }

    @Test
    public void testServiceConfig() throws Exception {
        pushInlineDeployments("org.nuxeo.ecm.platform.scanimporter.test:OSGI-INF/importerservice-test-config0.xml");

        ScannedFileMapperService sfms = Framework.getService(ScannedFileMapperService.class);
        ImporterConfig config = sfms.getImporterConfig();

        assertNotNull(config);

        assertEquals("/tmp/somefolder", config.getSourcePath());
        assertEquals("/tmp/processed", config.getProcessedPath());
        assertEquals(new Integer(2), config.getNbThreads());
        assertEquals(new Integer(5), config.getBatchSize());
        assertEquals("/default-domain/import", config.getTargetPath());

    }

}
