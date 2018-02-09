/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 */
public class AnnotationsComponentTest extends NXRuntimeTestCase {

    private AnnotationsService service;

    private AnnotationConfigurationService configuration;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployTestContrib("org.nuxeo.ecm.annotations", "test-ann-contrib.xml");
    }

    @Test
    public void testServices() throws Exception {
        service = Framework.getService(AnnotationsService.class);
        assertNotNull(service);

        configuration = Framework.getService(AnnotationConfigurationService.class);
        assertNotNull(configuration);

        AnnotationConfigurationServiceImpl configurationImpl = (AnnotationConfigurationServiceImpl) configuration;
        UriResolver resolver = configurationImpl.getUriResolver();
        assertNotNull(resolver);

        URLPatternFilter filter = configurationImpl.getUrlPatternFilter();
        assertNotNull(filter);

        MetadataMapper mapper = configurationImpl.getMetadataMapper();
        assertNotNull(mapper);

        PermissionManager manager = configurationImpl.getPermissionManager();
        assertNotNull(manager);

        AnnotabilityManager annManager = configurationImpl.getAnnotabilityManager();
        assertNotNull(annManager);

        List<EventListener> l = configurationImpl.getListeners();
        assertNotNull(l);
        assertEquals(1, l.size());

        AnnotationIDGenerator generator = configurationImpl.getIDGenerator();
        assertNotNull(generator);
        assertTrue(generator instanceof DefaultIDGenerator);
        assertEquals("readAnnotation", configurationImpl.getReadAnnotationPermission());
    }

}
