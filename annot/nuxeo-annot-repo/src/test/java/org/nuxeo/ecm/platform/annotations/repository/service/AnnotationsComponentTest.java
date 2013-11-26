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

package org.nuxeo.ecm.platform.annotations.repository.service;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.annotations.repository.DefaultNuxeoMetadataMapper;
import org.nuxeo.ecm.platform.annotations.repository.DefaultNuxeoUriResolver;
import org.nuxeo.ecm.platform.annotations.service.AnnotationConfigurationService;
import org.nuxeo.ecm.platform.annotations.service.AnnotationConfigurationServiceImpl;
import org.nuxeo.ecm.platform.annotations.service.MetadataMapper;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.runtime.api.Framework;
/**
 * @author Alexandre Russel
 *
 * Check the default configuration to the annotation service.
 */
public class AnnotationsComponentTest extends AbstractRepositoryTestCase {
    private AnnotationsService service;
    private AnnotationConfigurationService configuration;
    private AnnotationsRepositoryConfigurationService repositoryConfiguration;
    private AnnotationsRepositoryService repositoryService;

    @Test
    public void testServices() throws Exception {
        DocumentViewCodecManager viewCodec = Framework.getService(DocumentViewCodecManager.class);
        assertNotNull(viewCodec);
        service = Framework.getService(AnnotationsService.class);
        assertNotNull(service);
        configuration = Framework.getService(AnnotationConfigurationService.class);
        assertNotNull(configuration);
        AnnotationConfigurationServiceImpl configurationImpl = (AnnotationConfigurationServiceImpl) configuration;
        UriResolver resolver = configurationImpl.getUriResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof DefaultNuxeoUriResolver);
        DefaultNuxeoUriResolver uriResolver = (DefaultNuxeoUriResolver) resolver;
        assertNotNull(uriResolver);
        MetadataMapper metadataMapper = configurationImpl.getMetadataMapper();
        assertNotNull(metadataMapper);
        assertTrue(metadataMapper instanceof DefaultNuxeoMetadataMapper);
        assertEquals("viewDocument", configurationImpl.getReadAnnotationPermission());
        assertEquals(1, configurationImpl.getListeners().size());
        repositoryConfiguration = Framework.getService(AnnotationsRepositoryConfigurationService.class);
        assertNotNull(repositoryConfiguration);
        repositoryService = Framework.getService(AnnotationsRepositoryService.class);
        assertNotNull(repositoryService);
        List<String> eventIds = repositoryConfiguration.getEventIds();
        assertNotNull(eventIds);
        GraphManagerEventListener manager = repositoryConfiguration.getGraphManagerEventListener();
        assertNotNull(manager);
    }
}
