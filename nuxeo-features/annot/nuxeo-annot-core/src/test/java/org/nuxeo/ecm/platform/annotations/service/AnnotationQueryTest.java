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

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationImpl;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
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
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.usermanager")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.annotations:test-ann-contrib.xml")
public class AnnotationQueryTest {

    private final AnnotationQuery query = new AnnotationQuery();

    private final AnnotationManager manager = new AnnotationManager();

    @Test
    public void testgetAnnotationsForURIs() throws Exception {
        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);

        annotation.setSubject(new ResourceImpl("http://foo/1"));
        assertEquals("http://www.w3.org/2005/Incubator/", annotation.getAnnotates().toString());

        AnnotationImpl ann = (AnnotationImpl) annotation;
        assertNotNull(ann);

        List<Annotation> annotations = query.getAnnotationsForURIs(ann.getAnnotates(), ann.getGraph());
        assertNotNull(annotations);
        assertEquals(1, annotations.size());

        annotation = annotations.get(0);
        assertEquals("http://www.w3.org/2005/Incubator/", annotation.getAnnotates().toString());
        assertEquals("Alexandre Russel", annotation.getCreator());
    }

}
