/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.util.List;

import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationsComponentTest extends NXRuntimeTestCase {

    private AnnotationsService service;

    private AnnotationConfigurationService configuration;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployTestContrib("org.nuxeo.ecm.annotations","test-ann-contrib.xml");
    }

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
        assertEquals("readAnnotation",
                configurationImpl.getReadAnnotationPermission());
    }

}
