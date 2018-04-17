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

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.annotations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.annotations:test-ann-contrib.xml")
public class AnnotationsComponentTest {

    @Inject
    public AnnotationsService service;

    @Inject
    public AnnotationConfigurationService configuration;

    @Test
    public void testServices() throws Exception {
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
