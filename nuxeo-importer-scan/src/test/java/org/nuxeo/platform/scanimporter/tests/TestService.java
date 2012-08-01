/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.platform.scanimporter.tests;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;
import org.nuxeo.ecm.platform.scanimporter.service.ScanFileMappingDescriptor;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperComponent;
import org.nuxeo.ecm.platform.scanimporter.service.ScannedFileMapperService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.scanimporter", "OSGI-INF/importerservice-framework.xml");
    }

    @Test
    public void testServiceLookup() {
        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        assertNotNull(sfms);
    }

    @Test
    public void testServiceContrib() throws Exception {

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);
        ScanFileMappingDescriptor desc =  ((ScannedFileMapperComponent)sfms).getMappingDesc();

        assertNull(desc);
        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib0.xml");

        desc =  ((ScannedFileMapperComponent)sfms).getMappingDesc();
        assertNotNull(desc);

        assertEquals(2,desc.getFieldMappings().size());
        assertEquals(1,desc.getBlobMappings().size());

        assertEquals("xpath1",desc.getFieldMappings().get(0).getSourceXPath());
        assertEquals("attr1",desc.getFieldMappings().get(0).getSourceAttribute());
        assertEquals("string",desc.getFieldMappings().get(0).getTargetType());
        assertEquals("dc:title",desc.getFieldMappings().get(0).getTargetXPath());;


        assertEquals("xpath3",desc.getBlobMappings().get(0).getSourceXPath());
        assertEquals("filePath1",desc.getBlobMappings().get(0).getSourcePathAttribute());
        assertEquals("fileName1",desc.getBlobMappings().get(0).getSourceFilenameAttribute());
        assertEquals("file:content",desc.getBlobMappings().get(0).getTargetXPath());

    }

    @Test
    public void testServiceConfig() throws Exception {

        ScannedFileMapperService sfms = Framework.getLocalService(ScannedFileMapperService.class);

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-config0.xml");

        ImporterConfig config = sfms.getImporterConfig();

        assertNotNull(config);

        assertEquals("/tmp/somefolder",config.getSourcePath());
        assertEquals("/tmp/processed",config.getProcessedPath());
        assertEquals(new Integer(2),config.getNbThreads());
        assertEquals(new Integer(5),config.getBatchSize());
        assertEquals("/default-domain/import",config.getTargetPath());

    }


}
