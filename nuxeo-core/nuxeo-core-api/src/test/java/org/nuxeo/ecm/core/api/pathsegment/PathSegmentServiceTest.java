/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

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
            return (DocumentModel) Proxy.newProxyInstance(DocumentModelProxy.class.getClassLoader(),
                    new Class<?>[] { DocumentModel.class }, new DocumentModelProxy(title));
        }

        public String title;

        public DocumentModelProxy(String title) {
            this.title = title;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-pathsegment-contrib.xml");
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));
    }

    @Test
    public void testContribOverride() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-pathsegment-contrib.xml");
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));
        deployContrib("org.nuxeo.ecm.core.api.tests", "OSGI-INF/test-pathsegment-contrib2.xml");
        assertEquals("My Document", service.generatePathSegment(doc));
    }

}
