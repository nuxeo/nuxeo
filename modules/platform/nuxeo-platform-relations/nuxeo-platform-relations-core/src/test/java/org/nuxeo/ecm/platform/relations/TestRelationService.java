/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestRelationService.java 22121 2007-07-06 16:33:15Z gracinet $
 */

package org.nuxeo.ecm.platform.relations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.tests:nxrelations-test-bundle.xml")
public class TestRelationService {

    @Inject
    private RelationManager service;

    @Test
    public void testGetGraphTypes() {
        List<String> types = ((RelationService) service).getGraphTypes();
        assertEquals(3, types.size());
        assertTrue(types.contains("core"));
        assertTrue(types.contains("dummygraph"));
        assertFalse(types.contains("foo"));
    }

    @Test
    public void testGetGraphByNameOk() {
        Graph graph = service.getGraphByName("myrelations");
        assertNotNull(graph);
        assertEquals(graph.getClass(), DummyGraphType.class);
        DummyGraphType realGraph = (DummyGraphType) graph;
        assertEquals("myrelations", realGraph.name);
        assertEquals("sql", realGraph.backend);
        assertEquals("localhost", realGraph.host);
        assertEquals("8080", realGraph.port);
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        namespaces.put("dc", "http://purl.org/dc/elements/1.1/");
        namespaces.put("nxtest", "http://nuxeo/org/nxrelations/test/");
        assertEquals(namespaces, realGraph.namespaces);
    }

    @Test
    public void testGetGraphByNameNoImpl() {
        try {
            service.getGraphByName("unexistentgraph");
            fail("Should have raised a RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    @Test
    public void testGetGraphByNameUnexistent() {
        try {
            service.getGraphByName("foo");
            fail("Should have raised a RuntimeException");
        } catch (RuntimeException e) {
        }
    }

    @Test
    public void testGetGraphByNameFactory() {
        Graph graph = service.getGraphByName("somerelations");
        assertNotNull(graph);
        assertEquals(DummyGraphType.class, graph.getClass());
    }

    @Test
    public void testGetResourceOK() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/test/", resourceLike, null);
        assertNotNull(resource);
        assertTrue(resource.isQNameResource());
        QNameResource qnameres = (QNameResource) resource;
        assertEquals("http://nuxeo.org/nxrelations/test/", qnameres.getNamespace());
        assertEquals("test", qnameres.getLocalName());
    }

    @Test
    public void testGetAllResourcesFor() {
        Serializable o = new DummyResourceLike("test");
        Set<Resource> resources = service.getAllResources(o, null);
        assertNotNull(resources);
        assertEquals(2, resources.size());

        // Check local names and extract namespaces
        Set<String> nameSpaces = new HashSet<>();
        for (Resource res : resources) {
            assertTrue(res instanceof QNameResource);
            QNameResource qn = (QNameResource) res;
            assertEquals("test", qn.getLocalName());
            nameSpaces.add(qn.getNamespace());
        }

        Set<String> expectedNameSpaces = new HashSet<>(
                Arrays.asList("http://nuxeo.org/nxrelations/test2/", "http://nuxeo.org/nxrelations/test/"));

        assertEquals(expectedNameSpaces, nameSpaces);
    }

    @Test
    public void testGetResourceNoImpl() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/test-dummy/", resourceLike, null);
        assertNull(resource);
    }

    @Test
    public void testGetResourceUnexistent() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/unexistent/", resourceLike, null);
        assertNull(resource);
    }

    @Test
    public void testGetResourceRepresentationOK() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/test/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/test/", resource, null);
        assertNotNull(object);
        assertTrue(object instanceof DummyResourceLike);
        assertEquals("test", ((DummyResourceLike) object).getId());
    }

    @Test
    public void testGetResourceRepresentationNoImpl() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/test-dummy/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/test-dummy/", resource, null);
        assertNull(object);
    }

    @Test
    public void testGetResourceRepresentationUnexistent() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/unexistent/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/unexistent/", resource, null);
        assertNull(object);
    }

    @Test
    public void testGetResourceOKWithContext() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/test/", resourceLike, null);
        assertNotNull(resource);
        assertTrue(resource.isQNameResource());
        QNameResource qnameres = (QNameResource) resource;
        assertEquals("http://nuxeo.org/nxrelations/test/", qnameres.getNamespace());
        assertEquals("test", qnameres.getLocalName());
    }

    @Test
    public void testGetAllResourcesForWithContext() {
        Serializable o = new DummyResourceLike("test");
        Set<Resource> resources = service.getAllResources(o, null);
        assertNotNull(resources);
        assertEquals(2, resources.size());

        // Check local names and extract namespaces
        Set<String> nameSpaces = new HashSet<>();
        for (Resource res : resources) {
            assertTrue(res instanceof QNameResource);
            QNameResource qn = (QNameResource) res;
            assertEquals("test", qn.getLocalName());
            nameSpaces.add(qn.getNamespace());
        }

        Set<String> expectedNameSpaces = new HashSet<>(
                Arrays.asList("http://nuxeo.org/nxrelations/test2/", "http://nuxeo.org/nxrelations/test/"));

        assertEquals(expectedNameSpaces, nameSpaces);
    }

    @Test
    public void testGetResourceNoImplWithContext() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/test-dummy/", resourceLike, null);
        assertNull(resource);
    }

    @Test
    public void testGetResourceUnexistentWithContext() {
        Serializable resourceLike = new DummyResourceLike("test");
        Resource resource = service.getResource("http://nuxeo.org/nxrelations/unexistent/", resourceLike, null);
        assertNull(resource);
    }

    @Test
    public void testGetResourceRepresentationOKWithContext() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/test/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/test/", resource, null);
        assertNotNull(object);
        assertTrue(object instanceof DummyResourceLike);
        assertEquals("test", ((DummyResourceLike) object).getId());
    }

    @Test
    public void testGetResourceRepresentationNoImplWithContext() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/test-dummy/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/test-dummy/", resource, null);
        assertNull(object);
    }

    @Test
    public void testGetResourceRepresentationUnexistentWithContext() {
        Resource resource = new QNameResourceImpl("http://nuxeo.org/nxrelations/unexistent/", "test");
        Object object = service.getResourceRepresentation("http://nuxeo.org/nxrelations/unexistent/", resource, null);
        assertNull(object);
    }

}
