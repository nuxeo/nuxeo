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

import java.net.URI;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.relations.api.Resource;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class AnnotationsServiceTest extends AbstractAnnotationTest {
    private static final String HTTP_MYEXEMPLE_COM_NUXEO_ANNOTATIONS = "http://myexemple.com/nuxeo/Annotations/";

    @Test
    public void testAddAnnotation() throws Exception {
        assertNotNull(annotation);

        Annotation result = service.addAnnotation(annotation, user,
                HTTP_MYEXEMPLE_COM_NUXEO_ANNOTATIONS);
        assertNotNull(result);

        Resource subject = result.getSubject();
        assertNotNull(subject);

        String context = result.getContext();
        assertEquals(
                "http://www.w3.org/2005/Incubator/#xpointer(string-range(/html[1]/body[1]/div[3]/div[2]/p[1],\"\", 225, 17))",
                context);

        String annIdUri = subject.getUri();
        String annId = annIdUri.substring(annIdUri.lastIndexOf("/"),
                annIdUri.length());
        assertNotNull(annId);

        waitForEventsDispatched();
        List<Annotation> annotations = service.queryAnnotations(new URI(
                "http://www.w3.org/2005/Incubator/"), null, user);
        assertNotNull(annotations);

        // assertEquals(1, annotations.size()); TODO eclipse gave one result and maven two
        Annotation queriedAnnotation = annotations.get(0);
        assertNotNull(queriedAnnotation);
        assertEquals(
                "http://www.w3.org/2005/Incubator/#xpointer(string-range(/html[1]/body[1]/div[3]/div[2]/p[1],\"\", 225, 17))",
                queriedAnnotation.getContext());
    }
}
