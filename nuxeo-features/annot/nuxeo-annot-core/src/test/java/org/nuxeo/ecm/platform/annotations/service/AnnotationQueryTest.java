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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationImpl;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author Alexandre Russel
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.relations", //
        "org.nuxeo.ecm.annotations", //
        "org.nuxeo.ecm.relations.jena", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.types.api", //
})
@LocalDeploy("org.nuxeo.ecm.annotations:test-ann-contrib.xml")
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
