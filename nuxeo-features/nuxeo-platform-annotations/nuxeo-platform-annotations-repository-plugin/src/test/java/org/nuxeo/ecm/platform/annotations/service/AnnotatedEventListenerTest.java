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

import java.util.ArrayList;

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

    public void testEventLister() throws Exception {

        DummyDocumentEventListener.resetTest();
        assertNotNull(annotation);
        NuxeoPrincipal user = new UserPrincipal("bob", new ArrayList<String>(),
                false, false);
        Annotation createdAnnotation = service.addAnnotation(annotation, user,
                HTTP_LOCALHOST_8080_NUXEO);
        assertNotNull(createdAnnotation);
        int result = DummyDocumentEventListener.getTest();
        assertEquals(Integer.parseInt("11", 2), result);
        Statement statement = new StatementImpl(createdAnnotation.getSubject(),
                new ResourceImpl(AnnotationsConstants.A_BODY), new LiteralImpl(
                        "My new body"));
        createdAnnotation.setBody(statement);
        service.updateAnnotation(createdAnnotation, user,
                HTTP_LOCALHOST_8080_NUXEO);
        result = DummyDocumentEventListener.getTest();
        assertEquals(Integer.parseInt("110011", 2), result);
        createdAnnotation = service.getAnnotation(createdAnnotation.getId(),
                user, HTTP_LOCALHOST_8080_NUXEO);
        result = DummyDocumentEventListener.getTest();
        assertEquals(Integer.parseInt("111111", 2), result);
        service.deleteAnnotation(createdAnnotation, user);
        result = DummyDocumentEventListener.getTest();
        assertEquals(Integer.parseInt("11111111", 2), result);
    }

}
