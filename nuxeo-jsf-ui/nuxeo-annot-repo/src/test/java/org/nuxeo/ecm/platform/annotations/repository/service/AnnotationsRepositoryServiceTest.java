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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.annotations")
@Deploy("org.nuxeo.ecm.annotations.repository")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.url.core")
public class AnnotationsRepositoryServiceTest {

    @Test
    public void testServices() throws Exception {
        AnnotationsRepositoryService service = Framework.getService(AnnotationsRepositoryService.class);
        assertNotNull(service);
        AnnotationsRepositoryServiceImpl impl = (AnnotationsRepositoryServiceImpl) service;
        DocumentAnnotability annotability = impl.getAnnotability();
        assertNotNull(annotability);
        assertTrue(annotability instanceof DefaultDocumentAnnotability);
    }

}
