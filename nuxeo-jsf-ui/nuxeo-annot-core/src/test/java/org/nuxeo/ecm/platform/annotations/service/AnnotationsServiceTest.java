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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(AnnotationFeature.class)
public class AnnotationsServiceTest {

    private static final String HTTP_MYEXEMPLE_COM_NUXEO_ANNOTATIONS = "http://myexemple.com/nuxeo/Annotations/";

    @Inject
    protected AnnotationsService service;

    @Inject
    protected EventService eventService;

    @Test
    public void testAddAnnotation() throws Exception {
        NuxeoPrincipal user = new UserPrincipal("bob", new ArrayList<String>(), false, false);

        Annotation annotation;
        try (InputStream is = getClass().getResourceAsStream("/post-rdf.xml")) {
            assertNotNull(is);
            annotation = new AnnotationManager().getAnnotation(is);
        }

        assertNotNull(annotation);

        Annotation result = service.addAnnotation(annotation, user, HTTP_MYEXEMPLE_COM_NUXEO_ANNOTATIONS);
        assertNotNull(result);

        Resource subject = result.getSubject();
        assertNotNull(subject);

        String context = result.getContext();
        assertEquals(
                "http://www.w3.org/2005/Incubator/#xpointer(string-range(/html[1]/body[1]/div[3]/div[2]/p[1],\"\", 225, 17))",
                context);

        String annIdUri = subject.getUri();
        String annId = annIdUri.substring(annIdUri.lastIndexOf("/"), annIdUri.length());
        assertNotNull(annId);

        waitForAsyncCompletion();

        int count = service.getAnnotationsCount(new URI("http://www.w3.org/2005/Incubator/"), user);
        // assertEquals(1, count); TODO 1 in Eclipse, 2 in maven

        List<Annotation> annotations = service.queryAnnotations(new URI("http://www.w3.org/2005/Incubator/"), user);
        assertNotNull(annotations);

        // assertEquals(1, annotations.size()); TODO 1 in Eclipse, 2 in maven
        Annotation queriedAnnotation = annotations.get(0);
        assertNotNull(queriedAnnotation);
        assertEquals(
                "http://www.w3.org/2005/Incubator/#xpointer(string-range(/html[1]/body[1]/div[3]/div[2]/p[1],\"\", 225, 17))",
                queriedAnnotation.getContext());
    }

    protected void waitForAsyncCompletion() {
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

}
