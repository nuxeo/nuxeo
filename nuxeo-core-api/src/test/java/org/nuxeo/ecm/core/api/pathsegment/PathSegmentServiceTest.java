/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.core.api.pathsegment;

/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class PathSegmentServiceTest extends NXRuntimeTestCase {

    public static class DocumentModelProxy implements InvocationHandler {

        public static DocumentModel newDocumentModel(String title) {
            return (DocumentModel) Proxy.newProxyInstance(
                    DocumentModelProxy.class.getClassLoader(),
                    new Class<?>[] { DocumentModel.class },
                    new DocumentModelProxy(title));
        }

        public String title;

        public DocumentModelProxy(String title) {
            this.title = title;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String name = method.getName();
            if (name.equals("getTitle")) {
                return title;
            }
            return null;
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
    }

    @Test
    public void testDefault() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("My Document", service.generatePathSegment(doc));
    }

    @Test
    public void testContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-pathsegment-contrib.xml");
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));
    }

    @Test
    public void testContribOverride() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-pathsegment-contrib.xml");
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-pathsegment-contrib2.xml");
        assertEquals("My Document", service.generatePathSegment(doc));
    }

}
