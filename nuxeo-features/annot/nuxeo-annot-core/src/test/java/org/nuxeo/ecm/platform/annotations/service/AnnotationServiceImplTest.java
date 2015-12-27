/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.annotations.service;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(AnnotationFeature.class)
public class AnnotationServiceImplTest {

    @Inject
    protected AnnotationsService service;

    @Test
    public void testAddAnnotation() throws Exception {
        NuxeoPrincipal user = new UserPrincipal("bob", new ArrayList<String>(), false, false);

        Annotation annotation;
        try (InputStream is = getClass().getResourceAsStream("/post-rdf.xml")) {
            assertNotNull(is);
            annotation = new AnnotationManager().getAnnotation(is);
        }

        Annotation result = service.addAnnotation(annotation, user, "http://myexemple.com/nuxeo/Annotations/");
        assertNotNull(result);

        Resource subject = result.getSubject();
        assertNotNull(subject);
    }

}
