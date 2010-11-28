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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.annotations.api.AnnotationImpl;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationQueryTest extends RepositoryOSGITestCase {

    private AnnotationsService service;

    private final AnnotationQuery query = new AnnotationQuery();

    private final AnnotationManager manager = new AnnotationManager();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployBundle("org.nuxeo.ecm.annotations");
        deployTestContrib("org.nuxeo.ecm.annotations","/test-ann-contrib.xml");
        deployBundle("org.nuxeo.ecm.relations.jena");
        deployBundle("org.nuxeo.ecm.platform.usermanager");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        service = Framework.getService(AnnotationsService.class);
        assertNotNull(service);
    }

    public void testgetAnnotationsForURIs() throws AnnotationException {
        InputStream is = getClass().getResourceAsStream("/post-rdf.xml");
        assertNotNull(is);

        Annotation annotation = manager.getAnnotation(is);
        assertNotNull(annotation);

        annotation.setSubject(new ResourceImpl("http://foo/1"));
        assertEquals("http://www.w3.org/2005/Incubator/",
                annotation.getAnnotates().toString());

        AnnotationImpl ann = (AnnotationImpl) annotation;
        assertNotNull(ann);

        List<Annotation> annotations = query.getAnnotationsForURIs(
                Collections.singletonList(ann.getAnnotates()), ann.getGraph());
        assertNotNull(annotations);
        assertEquals(1, annotations.size());

        annotation = annotations.get(0);
        assertEquals("http://www.w3.org/2005/Incubator/",
                annotation.getAnnotates().toString());
        assertEquals("Alexandre Russel", annotation.getCreator());
    }

}
