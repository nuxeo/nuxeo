/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api")
public class PathSegmentServiceTest {

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

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testDefault() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("My Document", service.generatePathSegment(doc));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-pathsegment-contrib.xml")
    public void testContrib() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-pathsegment-contrib.xml")
    public void testContribOverride() throws Exception {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        DocumentModel doc = DocumentModelProxy.newDocumentModel("My Document");
        assertEquals("my-document", service.generatePathSegment(doc));

        hotDeployer.deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-pathsegment-contrib2.xml");

        service = Framework.getService(PathSegmentService.class);
        assertEquals("My Document", service.generatePathSegment(doc));
    }

    @Test
    public void testGeneratePathSegment() {
        PathSegmentService service = Framework.getService(PathSegmentService.class);
        assertNotNull(service);

        String s;
        // stupid ids -> random
        for (String id : Arrays.asList("", " ", "  ", "-", "./", ".", "..", " . ", " .. ", "\"", "'", "/", "//")) {
            String newId = service.generatePathSegment(id);
            assertTrue(id + " -> " + newId, newId.length() > 6);
            assertTrue(newId, Character.isDigit(newId.charAt(0)));
        }

        // keeps normal names
        s = "My Document.pdf";
        assertEquals(s, service.generatePathSegment(s));
        // keeps non-ascii chars and capitals
        s = "C'est l'\u00E9t\u00E9   !!";
        assertEquals(s, service.generatePathSegment(s));
        // trims spaces
        s = "  Foo  bar  ";
        assertEquals("Foo  bar", service.generatePathSegment(s));
        // converts slashes
        s = "foo/bar";
        assertEquals("foo-bar", service.generatePathSegment(s));

    }

}
