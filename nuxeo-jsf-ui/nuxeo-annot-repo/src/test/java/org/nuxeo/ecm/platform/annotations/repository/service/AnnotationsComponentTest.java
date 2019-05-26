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

package org.nuxeo.ecm.platform.annotations.repository.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
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
 * @author Alexandre Russel Check the default configuration to the annotation service.
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
