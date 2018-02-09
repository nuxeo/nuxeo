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

package org.nuxeo.ecm.platform.annotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.service.DefaultUriResolver;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 */
public class AnnotationManagerTest extends NXRuntimeTestCase {

    private final AnnotationManager manager = new AnnotationManager();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
    }

    @Test
    public void testGetPostNewAnnotation() throws Exception {
        assertNotNull(manager);

        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);
        assertEquals("http://www.w3.org/2005/Incubator/", annotation.getAnnotates().toString());

        annotation.setSubject(new ResourceImpl("http://foo/1"));
        assertEquals("http://www.w3.org/2005/Incubator/", annotation.getAnnotates().toString());
    }

    @Test
    public void testReadAnnoteaSpecPost() throws Exception {
        assertNotNull(manager);

        InputStream is = getClass().getResourceAsStream("/annotea-spec-post.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);
    }

    @Test
    public void testGetCreatedAnnotation() throws Exception {
        InputStream is = getClass().getResourceAsStream("/repo-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);

        Resource resource = annotation.getSubject();
        assertNotNull(resource);
    }

    @Test
    public void testTranslateAnnotationFromRepo() throws Exception {
        final String baseUrl = "http://myexemple.com/nuxeo/Annotations/";
        UriResolver resolver = new DefaultUriResolver();
        assertNotNull(resolver);

        Annotation annotation = manager.getAnnotation(getClass().getResourceAsStream("/repo-rdf.xml"));
        assertNotNull(annotation);

        Resource resource = annotation.getSubject();
        assertNotNull(resource);
        assertEquals("urn:annotation:3ACF6D754", resource.getUri());

        Annotation result = manager.translateAnnotationFromRepo(resolver, baseUrl, annotation);
        assertNotNull(result);

        resource = result.getSubject();
        assertNotNull(resource);
        assertEquals(resource.getUri(), baseUrl + "3ACF6D754");
    }
}
