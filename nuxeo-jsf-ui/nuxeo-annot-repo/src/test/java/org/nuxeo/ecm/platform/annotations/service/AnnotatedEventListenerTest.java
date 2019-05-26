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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.repository.AbstractRepositoryTestCase;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;

/**
 * @author Alexandre Russel
 */
public class AnnotatedEventListenerTest extends AbstractRepositoryTestCase {

    private static final String HTTP_LOCALHOST_8080_NUXEO = "http://localhost:8080/nuxeo";

    @Test
    public void testEventLister() throws Exception {

        DummyDocumentEventListener.resetTest();
        assertNotNull(annotation);
        NuxeoPrincipal user = new UserPrincipal("bob", new ArrayList<String>(), false, false);
        Annotation createdAnnotation = service.addAnnotation(annotation, user, HTTP_LOCALHOST_8080_NUXEO);
        assertNotNull(createdAnnotation);
        Set<String> set = DummyDocumentEventListener.getTest();
        assertEquals(new HashSet<>(Arrays.asList("bc", "ac")), set);

        DummyDocumentEventListener.resetTest();
        Statement statement = new StatementImpl(createdAnnotation.getSubject(), new ResourceImpl(
                AnnotationsConstants.A_BODY), new LiteralImpl("My new body"));
        createdAnnotation.setBody(statement);
        service.updateAnnotation(createdAnnotation, user, HTTP_LOCALHOST_8080_NUXEO);
        set = DummyDocumentEventListener.getTest();
        assertEquals(new HashSet<>(Arrays.asList("bu", "au")), set);

        nextTransaction();

        DummyDocumentEventListener.resetTest();
        createdAnnotation = service.getAnnotation(createdAnnotation.getId(), user, HTTP_LOCALHOST_8080_NUXEO);
        set = DummyDocumentEventListener.getTest();
        assertEquals(new HashSet<>(Arrays.asList("br", "ar")), set);

        DummyDocumentEventListener.resetTest();
        service.deleteAnnotation(createdAnnotation, user);
        set = DummyDocumentEventListener.getTest();
        assertEquals(new HashSet<>(Arrays.asList("bd", "ad")), set);
    }

}
