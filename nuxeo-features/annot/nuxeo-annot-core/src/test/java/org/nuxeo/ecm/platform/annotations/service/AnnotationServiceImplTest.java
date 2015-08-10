/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
