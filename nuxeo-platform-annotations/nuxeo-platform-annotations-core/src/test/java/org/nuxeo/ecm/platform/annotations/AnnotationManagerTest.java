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

package org.nuxeo.ecm.platform.annotations;

import java.io.InputStream;

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.annotations.service.DefaultUriResolver;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationManagerTest extends NXRuntimeTestCase {

    private final AnnotationManager manager = new AnnotationManager();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.relations.jena");
    }

    public void testGetPostNewAnnotation() throws AnnotationException {
        assertNotNull(manager);

        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);
        assertEquals("http://www.w3.org/2005/Incubator/",
                annotation.getAnnotates().toString());

        annotation.setSubject(new ResourceImpl("http://foo/1"));
        assertEquals("http://www.w3.org/2005/Incubator/",
                annotation.getAnnotates().toString());
    }

    public void testReadAnnoteaSpecPost() throws AnnotationException {
        assertNotNull(manager);

        InputStream is = getClass().getResourceAsStream(
                "/annotea-spec-post.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);
    }

    public void testGetCreatedAnnotation() throws AnnotationException {
        InputStream is = getClass().getResourceAsStream("/repo-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);

        Resource resource = annotation.getSubject();
        assertNotNull(resource);
    }

    public void testTranslateAnnotationFromRepo() throws AnnotationException {
        final String baseUrl = "http://myexemple.com/nuxeo/Annotations/";
        UriResolver resolver = new DefaultUriResolver();
        assertNotNull(resolver);

        Annotation annotation = manager.getAnnotation(getClass().getResourceAsStream(
                "/repo-rdf.xml"));
        assertNotNull(annotation);

        Resource resource = annotation.getSubject();
        assertNotNull(resource);
        assertEquals("urn:annotation:3ACF6D754", resource.getUri());

        Annotation result = manager.translateAnnotationFromRepo(resolver,
                baseUrl, annotation);
        assertNotNull(result);

        resource = result.getSubject();
        assertNotNull(resource);
        assertEquals(resource.getUri(), baseUrl + "3ACF6D754");
    }
}
