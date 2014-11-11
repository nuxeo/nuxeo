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

import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
import org.nuxeo.ecm.platform.relations.api.Resource;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class AnnotationServiceImplTest extends AbstractAnnotationTest {

    private AnnotationsServiceImpl service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        service = new AnnotationsServiceImpl();
    }

    public void testAddAnnotation() throws AnnotationException {
        assertNotNull(annotation);

        Annotation result = service.addAnnotation(annotation, user,
                "http://myexemple.com/nuxeo/Annotations/");
        assertNotNull(result);

        Resource subject = result.getSubject();
        assertNotNull(subject);
    }

}
