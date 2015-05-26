/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Tests using the low-level Session / Document API.
 * <p>
 * It's important to test them as they may be used by user code for security policies or versioning service
 * contributions, or blob providers.
 *
 * @since 7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml")
public class TestDocument {

    @Inject
    protected CoreSession coreSession;

    protected Session session;

    @Before
    public void setUp() {
        session = ((AbstractSession) coreSession).getSession();
    }

    @FunctionalInterface
    private interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    protected final BiFunction<Document, String, Object> DocumentGetValue = (Document doc, String xpath) -> {
        try {
            return doc.getValue(xpath);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    };

    protected final TriConsumer<Document, String, Object> DocumentSetValue = (Document doc, String xpath,
            Object value) -> {
        try {
            doc.setValue(xpath, value);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    };

    @Test
    public void testGetValueErrors() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "TestDocument");
        tryUnknownProperty(xpath -> DocumentGetValue.apply(doc, xpath));
    }

    @Test
    public void testSetValueErrors() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "TestDocument");
        tryUnknownProperty(xpath -> DocumentSetValue.accept(doc, xpath, null));
    }

    protected void tryUnknownProperty(Consumer<String> c) {
        check(c, "nosuchprop", null);
        check(c, "tp:nosuchprop", null);
        check(c, "nosuchschema:nosuchprop", null);
        check(c, "tp:complexChain/nosuchprop", "Unknown segment: nosuchprop");
        check(c, "tp:complexChain/complex/nosuchprop", "Unknown segment: nosuchprop");
        check(c, "tp:complexChain/0", "Cannot use index after segment: tp:complexChain");
        check(c, "tp:complexList/notaninteger/foo", "Missing list index after segment: tp:complexList");
        check(c, "tp:complexList/0/foo", "Index out of bounds: 0");
        check(c, "tp:stringArray/foo", "Segment must be last: tp:stringArray");
    }

    protected void check(Consumer<String> c, String xpath, String detail) {
        try {
            c.accept(xpath);
            fail();
        } catch (PropertyNotFoundException e) {
            assertEquals(xpath, e.getPath());
            assertEquals(detail, e.getDetail());
        }
    }

    @Test
    public void testSetValueErrors2() throws Exception {
        Document root = session.getRootDocument();
        Document doc1 = root.addChild("doc", "TestDocument");
        Document doc2 = root.addChild("doc", "File");
        doc1.setValue("tp:complexList", Arrays.asList(Collections.emptyMap()));

        BiConsumer<String, Object> c1 = (xpath, value) -> DocumentSetValue.accept(doc1, xpath, value);
        checkSet(c1, "tp:complexList", Long.valueOf(0),
                "Expected List value for: tp:complexList, got java.lang.Long instead");
        checkSet(c1, "tp:complexList/0", Long.valueOf(0), "Expected Map value for: item, got java.lang.Long instead");
        checkSet(c1, "tp:complexList/0", Collections.singletonMap("foo", null), "Unknown key: foo for item");

        BiConsumer<String, Object> c2 = (xpath, value) -> DocumentSetValue.accept(doc2, xpath, value);
        checkSet(c2, "content", Long.valueOf(0), "Expected Blob value for: content, got java.lang.Long instead");
    }

    protected void checkSet(BiConsumer<String, Object> c, String xpath, Object value, String message) {
        try {
            c.accept(xpath, value);
            fail();
        } catch (PropertyException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testSimple() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "File");

        // basic property
        doc.setValue("dc:title", "title");
        assertEquals("title", doc.getValue("dc:title"));

        // array
        doc.setValue("dc:subjects", new Object[] { "a", "b" });
        assertEquals(Arrays.asList("a", "b"), Arrays.asList((Object[]) doc.getValue("dc:subjects")));
        doc.setValue("dc:subjects", Arrays.asList("c", "d"));
        assertEquals(Arrays.asList("c", "d"), Arrays.asList((Object[]) doc.getValue("dc:subjects")));

        // blob
        Blob blob = Blobs.createBlob("hello world!");
        doc.setValue("content", blob);
        blob = (Blob) doc.getValue("content");
        assertEquals("hello world!", blob.getString());
    }

    @Test
    public void testComplex() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "ComplexDoc");

        String content2 = "My content 2";
        Blob blob = Blobs.createBlob("My content");
        Blob blob2 = Blobs.createBlob(content2);
        Long size1 = Long.valueOf(123);
        Long size2 = Long.valueOf(456);
        Long size3 = Long.valueOf(789);

        Map<String, Object> attachedFile = new HashMap<>();
        List<Map<String, Object>> vignettes = new ArrayList<>();
        attachedFile.put("vignettes", vignettes);
        Map<String, Object> vignette = new HashMap<>();
        vignette.put("width", size1);
        vignette.put("content", blob);
        vignettes.add(vignette); // 0
        vignettes.add(Collections.singletonMap("height", size3)); // 1
        vignettes.add(Collections.singletonMap("height", size3)); // 2
        vignettes.add(Collections.singletonMap("height", size3)); // 3

        // set recursive
        doc.setValue("cmpf:attachedFile", attachedFile);

        // set deep
        doc.setValue("cmpf:attachedFile/vignettes/0/content", blob2);
        doc.setValue("cmpf:attachedFile/vignettes/0/content/mime-type", "text/foo");
        doc.setValue("cmpf:attachedFile/vignettes/1/width", size2);
        doc.setValue("cmpf:attachedFile/vignettes/vignette[1]/width", size2); // non-canonical xpath
        doc.setValue("cmpf:attachedFile/vignettes/2", new HashMap<>()); // overwrite
        doc.setValue("cmpf:attachedFile/vignettes/3", null); // overwrite

        // get deep
        assertEquals("text/foo", doc.getValue("cmpf:attachedFile/vignettes/0/content/mime-type"));
        assertEquals(size1, doc.getValue("cmpf:attachedFile/vignettes/0/width"));
        assertEquals(size2, doc.getValue("cmpf:attachedFile/vignettes/1/width"));
        assertEquals(size3, doc.getValue("cmpf:attachedFile/vignettes/1/height"));
        assertNull(doc.getValue("cmpf:attachedFile/vignettes/2/height")); // was overwritten
        assertNull(doc.getValue("cmpf:attachedFile/vignettes/3/height")); // was overwritten

        // get recursive blob
        Object b = doc.getValue("cmpf:attachedFile/vignettes/0/content");
        assertTrue(b instanceof Blob);
        assertEquals(content2, ((Blob) b).getString());
        Map<String, Object> vignette2 = new HashMap<>();
        vignette2.put("width", size1);
        vignette2.put("content", blob);

        // get recursive list item
        @SuppressWarnings("unchecked")
        Map<String, Object> v0 = (Map<String, Object>) doc.getValue("cmpf:attachedFile/vignettes/0");
        assertEquals(size1, v0.get("width"));
        b = (Blob) v0.get("content");
        assertEquals(content2, ((Blob) b).getString());
        Object v1 = doc.getValue("cmpf:attachedFile/vignettes/1");
        Map<String, Object> ev1 = new HashMap<>();
        ev1.put("width", size2);
        ev1.put("height", size3);
        ev1.put("label", null);
        ev1.put("content", null);
        assertEquals(ev1, v1);
        Object v2 = doc.getValue("cmpf:attachedFile/vignettes/2");
        Map<String, Object> ev2or3 = new HashMap<>();
        ev2or3.put("width", null);
        ev2or3.put("height", null);
        ev2or3.put("label", null);
        ev2or3.put("content", null);
        assertEquals(ev2or3, v2);
        Object v3 = doc.getValue("cmpf:attachedFile/vignettes/3");
        assertEquals(ev2or3, v3);

        // get recursive list
        Object list = doc.getValue("cmpf:attachedFile/vignettes");
        assertTrue(list instanceof List);
        assertEquals(4, ((List<?>) list).size());
        assertEquals(Arrays.asList(v0, ev1, ev2or3, ev2or3), list);

        // get recursive map
        Map<String, Object> atf = new HashMap<>();
        atf.put("name", null);
        atf.put("vignettes", Arrays.asList(v0, ev1, ev2or3, ev2or3));
        assertEquals(atf, doc.getValue("cmpf:attachedFile"));
    }

    @Test
    public void testComplexFiles() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "File");

        doc.setValue("files", Arrays.asList(Collections.singletonMap("filename", "f1")));
        assertEquals("f1", doc.getValue("files/0/filename"));
    }

    @Test
    public void testBlobList() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "TestDocument");

        Object list = doc.getValue("tp:fileList");
        assertTrue(list instanceof List);
        assertEquals(0, ((List<?>) list).size());

        doc.setValue("tp:fileList", Arrays.asList(Blobs.createBlob("My content")));

        list = doc.getValue("tp:fileList");
        assertTrue(list instanceof List);
        @SuppressWarnings("unchecked")
        List<Blob> blobs = (List<Blob>) list;
        assertEquals(1, blobs.size());
        assertEquals("My content", blobs.get(0).getString());
    }

    @Test
    public void testFacet() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "File");

        try {
            doc.getValue("age:age");
            fail();
        } catch (PropertyNotFoundException e) {
            assertEquals("age:age", e.getPath());
            assertNull(e.getDetail());
        }
        try {
            doc.setValue("age:age", "123");
            fail();
        } catch (PropertyNotFoundException e) {
            assertEquals("age:age", e.getPath());
            assertNull(e.getDetail());
        }

        doc.addFacet("Aged");
        doc.setValue("age:age", "123");
        assertEquals("123", doc.getValue("age:age"));
    }

    @Test
    public void testProxySchema() throws Exception {
        Document root = session.getRootDocument();
        Document doc = root.addChild("doc", "File");
        Document proxy = session.createProxy(doc, root);
        session.save();

        try {
            doc.getValue("info:info");
        } catch (PropertyNotFoundException e) {
            assertEquals("info:info", e.getPath());
            assertNull(e.getDetail());
        }
        try {
            doc.setValue("info:info", "docinfo");
        } catch (PropertyNotFoundException e) {
            assertEquals("info:info", e.getPath());
            assertNull(e.getDetail());
        }

        assertNull(proxy.getValue("info:info"));
        proxy.setValue("info:info", "proxyinfo");
        session.save();

        assertEquals("proxyinfo", proxy.getValue("info:info"));
    }

}
