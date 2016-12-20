/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.externalblob.ExternalBlobAdapter;
import org.nuxeo.ecm.core.api.externalblob.FileSystemExternalBlobAdapter;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.ExternalBlobProperty;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSRepository;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.reload.ReloadService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.convert", //
        "org.nuxeo.ecm.core.convert.plugins", //
        "org.nuxeo.runtime.reload", //
})
@LocalDeploy({ "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-repo-core-types-contrib.xml",
        "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-restriction-contrib.xml",
        // deploy specific adapter for testing external blobs: files are stored
        // in temporary directory
        "org.nuxeo.ecm.core.test.tests:OSGI-INF/test-externalblob-adapters-contrib.xml", })
public class TestSQLRepositoryProperties {

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected EventService eventService;

    @Inject
    protected BlobHolderAdapterService blobHolderAdapterService;

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected CoreSession session;

    @Inject
    protected ReloadService reloadService;

    DocumentModel doc;

    @Before
    public void setUp() throws Exception {
        // set container to temp directory here in case that depends on the OS
        // or machine configuration and add funny characters to avoid problems
        // due to xml parsing
        ExternalBlobAdapter adapter = blobHolderAdapterService.getExternalBlobAdapterForPrefix("fs");
        Map<String, String> props = new HashMap<>();
        props.put(FileSystemExternalBlobAdapter.CONTAINER_PROPERTY_NAME,
                "\n" + Environment.getDefault().getTemp().getPath() + " ");
        adapter.setProperties(props);

        doc = session.createDocumentModel("TestDocument");
        doc.setPathInfo("/", "doc");
        doc = session.createDocument(doc);
    }

    protected boolean isDBSMarkLogic() {
        return coreFeature.getStorageConfiguration().isDBSMarkLogic();
    }

    protected CoreSession openSessionAs(String username) {
        return CoreInstance.openCoreSession(session.getRepositoryName(), username);
    }

    protected void reopenSession() {
        session = coreFeature.reopenCoreSession();
    }

    protected void waitForAsyncCompletion() {
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            nextTransaction();
        }
        eventService.waitForAsyncCompletion();
    }

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    protected File createTempFile() throws Exception {
        File file = Framework.createTempFile("testExternalBlob", ".txt");
        Framework.trackFile(file, file);
        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Hello External Blob");
        out.close();
        return file;
    }

    @Test
    public void testUnknownProperty() throws Exception {
        try {
            doc.getPropertyValue("nosuchprop");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("nosuchprop", e.getPath());
            assertNull(e.getDetail());
        }
        try {
            doc.getPropertyValue("tp:nosuchprop");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("tp:nosuchprop", e.getPath());
            assertEquals("segment nosuchprop cannot be resolved", e.getDetail());
        }
        try {
            doc.getPropertyValue("nosuchschema:nosuchprop");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("nosuchschema:nosuchprop", e.getPath());
            assertEquals("No such schema", e.getDetail());
        }
        try {
            doc.getPropertyValue("tp:complexChain/nosuchprop");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("tp:complexChain/nosuchprop", e.getPath());
            assertEquals("segment nosuchprop cannot be resolved", e.getDetail());
        }
        try {
            doc.getPropertyValue("tp:complexChain/complex/nosuchprop");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("tp:complexChain/complex/nosuchprop", e.getPath());
            assertEquals("segment nosuchprop cannot be resolved", e.getDetail());
        }
        try {
            doc.getPropertyValue("tp:complexList/notaninteger/foo");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("tp:complexList/notaninteger/foo", e.getPath());
            assertEquals("segment notaninteger cannot be resolved", e.getDetail());
        }
        try {
            doc.getPropertyValue("tp:complexList/0/foo");
            fail("Should throw PropertyNotFoundException");
        } catch (PropertyNotFoundException e) {
            assertEquals("tp:complexList/0/foo", e.getPath());
            assertEquals("segment 0 cannot be resolved", e.getDetail());
        }
    }

    // NXP-2467
    @Test
    public void testCreationWithDefaultPrefetch() throws Exception {
        DocumentModel doc = session.createDocumentModel("TestDocumentWithDefaultPrefetch");
        doc.setPathInfo("/", "docwithDefaultPrefetch");
        session.createDocument(doc);
    }

    @Test
    public void testStringArray() throws Exception {
        assertNotNull(doc.getPropertyValue("tp:stringArray"));
        String[] values = { "foo", "bar" };
        doc.setPropertyValue("tp:stringArray", values);
        doc = session.saveDocument(doc);
        assertTrue(Arrays.equals(values, (Object[]) doc.getPropertyValue("tp:stringArray")));
    }

    // NXP-2454
    @Test
    public void testDateArray() throws Exception {
        assertNotNull(doc.getPropertyValue("tp:dateArray"));
        Calendar cal = Calendar.getInstance();
        cal.set(2008, 6, 10);
        Calendar[] values = { cal };
        doc.setPropertyValue("tp:dateArray", values);
        doc = session.saveDocument(doc);
        // currently returning long[] instead of Calendar[]
        assertTrue(Arrays.equals(values, (Object[]) doc.getPropertyValue("tp:dateArray")));
    }

    // NXP-2454
    @Test
    public void testIntArray() throws Exception {
        assertNotNull(doc.getPropertyValue("tp:intArray"));
        Long[] values = { 1L, 2L, 3L };
        doc.setPropertyValue("tp:intArray", values);
        doc = session.saveDocument(doc);
        // currently returning long[], maybe this is the wanted behaviour (?)
        assertTrue(Arrays.equals(values, (Object[]) doc.getPropertyValue("tp:intArray")));
    }

    @Test
    public void testArrayWithNullFirst() throws Exception {
        assumeTrue("MarkLogic repository doesn't handle null values", !isDBSMarkLogic());
        assertNotNull(doc.getPropertyValue("tp:stringArray"));
        String[] values = { null, "bar" };
        doc.setPropertyValue("tp:stringArray", values);
        session.saveDocument(doc);
        session.save();
        nextTransaction();
        coreFeature.reopenCoreSession();
        doc = session.getDocument(doc.getRef());
        assertTrue(Arrays.equals(values, (Object[]) doc.getPropertyValue("tp:stringArray")));
    }

    @Test
    public void testComplexList() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:complexList") instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue("tp:complexList")).size());
        ArrayList<Map<String, Serializable>> values = new ArrayList<>();
        Map<String, Serializable> item = new HashMap<>();
        item.put("string", "foo");
        Calendar cal = Calendar.getInstance();
        cal.set(2008, 6, 10);
        item.put("date", cal);
        item.put("int", 3L);
        values.add(item);
        doc.setPropertyValue("tp:complexList", values);
        doc = session.saveDocument(doc);

        Serializable actual = doc.getPropertyValue("tp:complexList");
        assertTrue(actual instanceof List);
        assertEquals(1, ((List) actual).size());
        assertEquals(item, ((List) actual).get(0));
    }

    @Test
    public void testComplexListChange() throws Exception {
        ArrayList<Map<String, Serializable>> values = new ArrayList<>();
        Map<String, Serializable> item1 = new HashMap<>();
        Map<String, Serializable> item2 = new HashMap<>();
        Map<String, Serializable> item3 = new HashMap<>();
        List<?> actual;

        item1.put("string", "foo");
        item1.put("int", Long.valueOf(123));
        values.add(item1);
        doc.setPropertyValue("tp:complexList", values);
        doc = session.saveDocument(doc);

        session.save();

        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));

        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(1, actual.size());
        assertComplexListElements(actual, 0, "foo", 123);

        // add to list
        // (also modifies first element)
        item1.put("string", "foo2");

        item2.put("string", "bar");
        item2.put("int", Long.valueOf(999));
        values.add(item2);
        item3.put("string", "baz");
        item3.put("int", Long.valueOf(314));
        values.add(item3);
        doc.setPropertyValue("tp:complexList", values);
        doc = session.saveDocument(doc);

        session.save();

        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));

        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(3, actual.size());
        assertComplexListElements(actual, 0, "foo2", 123);
        assertComplexListElements(actual, 1, "bar", 999);
        assertComplexListElements(actual, 2, "baz", 314);

        // change list

        item1.put("int", Long.valueOf(111));
        item2.put("int", Long.valueOf(222));
        item3.put("int", Long.valueOf(333));
        doc.setPropertyValue("tp:complexList", values);
        doc = session.saveDocument(doc);

        session.save();

        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));

        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(3, actual.size());
        assertComplexListElements(actual, 0, "foo2", 111);
        assertComplexListElements(actual, 1, "bar", 222);
        assertComplexListElements(actual, 2, "baz", 333);

        // remove from list

        values.remove(0);
        values.remove(0);
        doc.setPropertyValue("tp:complexList", values);
        doc = session.saveDocument(doc);

        session.save();

        reopenSession();
        doc = session.getDocument(new PathRef("/doc"));

        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(1, actual.size());
        assertComplexListElements(actual, 0, "baz", 333);
    }

    // DBS-only test for in-db data corruption(?) (NXP-21278)
    @Test
    public void testComplexListElementNullInStorage() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isDBS());

        doc.setPropertyValue("tp:complexList",
                (Serializable) Arrays.asList( //
                        Collections.singletonMap("string", "foo"), //
                        Collections.singletonMap("string", "bar")));
        doc = session.saveDocument(doc);
        String id = doc.getId();
        session.save();
        nextTransaction();

        // change data
        StateDiff diff = new StateDiff();
        diff.put("tp:complexList",
                (Serializable) Arrays.asList( //
                        null, // null as first element of the list
                        Collections.singletonMap("string", "bar")));
        changeDoc(id, diff);

        // check that we don't crash on read
        doc = session.getDocument(doc.getRef());
        List<?> list = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(2, list.size());
        assertComplexListElements(list, 0, null, -1);
        assertComplexListElements(list, 1, "bar", -1);
    }

    @Test
    public void testComplexListPropertyRemove() throws Exception {
        List<Map<String, Serializable>> values = Arrays.asList( //
                Collections.singletonMap("string", "foo"), //
                Collections.singletonMap("string", "bar"), //
                Collections.singletonMap("string", "gee"));
        doc.setPropertyValue("tp:complexList", (Serializable) values);
        doc = session.saveDocument(doc);

        doc = session.getDocument(new PathRef("/doc"));
        List<?> actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(3, actual.size());
        assertComplexListElements(actual, 0, "foo", -1);
        assertComplexListElements(actual, 1, "bar", -1);
        assertComplexListElements(actual, 2, "gee", -1);

        // change ListProperty by removing non-final element

        // remove using index
        ListProperty prop = (ListProperty) doc.getProperty("tp:complexList");
        prop.remove(1);
        doc = session.saveDocument(doc);

        doc = session.getDocument(new PathRef("/doc"));
        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(2, actual.size());
        assertComplexListElements(actual, 0, "foo", -1);
        assertComplexListElements(actual, 1, "gee", -1);

        // remove using property
        prop = (ListProperty) doc.getProperty("tp:complexList");
        prop.remove(prop.get(0));
        doc = session.saveDocument(doc);

        doc = session.getDocument(new PathRef("/doc"));
        actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(1, actual.size());
        assertComplexListElements(actual, 0, "gee", -1);

    }

    @Test
    public void testComplexListPartialUpdate() throws Exception {
        List<Map<String, Serializable>> list = Arrays.asList(Collections.singletonMap("string", "foo"));
        doc.setPropertyValue("tp:complexList", (Serializable) list);
        doc = session.saveDocument(doc);
        session.save();

        // change just the int
        Property prop = doc.getProperty("tp:complexList/0/int");
        prop.setValue(Long.valueOf(1));
        doc = session.saveDocument(doc);
        session.save();

        // refetch, the string should still be here
        doc = session.getDocument(doc.getRef());
        List<?> actual = (List<?>) doc.getPropertyValue("tp:complexList");
        assertEquals(1, actual.size());
        assertComplexListElements(actual, 0, "foo", 1);
    }

    protected static void assertComplexListElements(List<?> list, int i, String string, int theint) {
        Map<String, Serializable> map = (Map<String, Serializable>) list.get(i);
        assertEquals(string, map.get("string"));
        assertEquals(theint == -1 ? null : Long.valueOf(theint), map.get("int"));
    }

    // NXP-912
    @Test
    public void testNewBlob() throws Exception {
        // simple
        Object value = null;
        Field field = schemaManager.getField("tp:fileList");
        Type type = field.getType();
        Type itemType = ((ListType) type).getFieldType();
        value = itemType.newInstance();
        assertNull(value);

        // complex
        field = schemaManager.getField("tp:fileComplexList");
        type = field.getType();
        itemType = ((ListType) type).getFieldType();
        Map<String, Serializable> map = (Map) itemType.newInstance();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("filename"));
        assertTrue(map.containsKey("blob"));
        assertNull(map.get("filename"));
        assertNull(map.get("blob"));
    }

    // NXP-2468
    @Test
    public void testBlobListValue() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:fileList") instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue("tp:fileList")).size());
        ArrayList<Blob> values = new ArrayList<>();
        Blob blob = Blobs.createBlob("My content");
        values.add(blob);
        doc.setPropertyValue("tp:fileList", values);
        doc = session.saveDocument(doc);

        Serializable actual = doc.getPropertyValue("tp:fileList");
        assertTrue(actual instanceof List);

        List<Blob> blobs = (List) actual;
        assertEquals(1, blobs.size());
        assertNotNull(blobs.get(0));
    }

    // NXP-2301
    @Test
    public void testSubBlobValue() throws Exception {
        // not null on list
        assertTrue(doc.getPropertyValue("tp:fileComplexList") instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue("tp:fileComplexList")).size());
        ArrayList<Map<String, Serializable>> values = new ArrayList<>();
        Map<String, Serializable> item = new HashMap<>();
        Blob blob = Blobs.createBlob("My content");
        item.put("blob", (Serializable) blob);
        item.put("filename", "My filename");
        values.add(item);
        doc.setPropertyValue("tp:fileComplexList", values);
        doc = session.saveDocument(doc);

        Object actual = doc.getPropertyValue("tp:fileComplexList");
        assertTrue(actual instanceof List);
        List<Map<String, Serializable>> items = (List) actual;
        assertEquals(1, items.size());
        assertNotNull(items.get(0));
        Map<String, Serializable> actualItem = items.get(0);
        assertEquals("My filename", actualItem.get("filename"));
        assertTrue(actualItem.get("blob") instanceof Blob);

        Object actualBlob = doc.getProperty("tp:fileComplexList/0/blob").getValue(Blob.class);
        assertTrue(actualBlob instanceof Blob);
    }

    @Test
    public void testComplexParallelFetch() throws Exception {
        DocumentModel doc2 = session.createDocumentModel("/", "doc2", "TestDocument2");
        doc2.setPropertyValue("dc:title", "doc2");
        doc2 = session.createDocument(doc2);
        session.save();
        // has not created the complex properties at that point

        try (CoreSession s1 = openSessionAs("Administrator"); //
                CoreSession s2 = openSessionAs("Administrator")) {
            DocumentModel d1 = s1.getDocument(new IdRef(doc2.getId()));
            DocumentModel d2 = s2.getDocument(new IdRef(doc2.getId()));
            // read the complex prop to trigger documentpart fetch
            // and node creation (SQLSession.makeProperties)
            d1.getProperty("tp:complex");
            d2.getProperty("tp:complex");
            // write an unrelated property, to trigger flush()
            d1.setPropertyValue("dc:title", "d1");
            d2.setPropertyValue("dc:title", "d2");
            s1.saveDocument(d1);
            s2.saveDocument(d2);
            s1.save();
            // without the fix the following save would cause a second insert
            s2.save();
        }
    }

    @Test
    public void testComplex2() throws Exception {
        doc = session.createDocumentModel("/", "doc2", "TestDocument2");
        doc = session.createDocument(doc);
        session.save();
        // has not created the complex properties at that point

        // read the a complex propy to trigger documentpart fetch
        // and node creation (SQLSession.makeProperties)
        doc.getProperty("tp:complex");
        // write an unrelated property, to trigger flush()
        doc.setPropertyValue("dc:title", "doc2bis");
        doc = session.saveDocument(doc);
        session.save();
    }

    @Test
    public void testComplexNotDirtyOnRead() throws Exception {
        doc = session.createDocumentModel("/", "doc2", "TestDocument2");
        doc = session.createDocument(doc);
        session.save();

        // reread doc
        doc = session.getDocument(doc.getRef());
        assertFalse(doc.isDirty());
        // read a complex prop
        Property prop = doc.getProperty("tp:complex");
        // check that this does not mark the doc dirty
        assertFalse(doc.isDirty());
        // but changing the property does
        prop.setValue(Collections.singletonMap("string", "abc"));
        assertTrue(doc.isDirty());
    }

    @Test
    public void testComplexNotDirtyOnVersionRead() throws Exception {
        doc = session.createDocumentModel("/", "doc2", "TestDocument2");
        doc = session.createDocument(doc);
        DocumentRef verRef = doc.checkIn(null, null);

        // reread doc
        DocumentModel ver = session.getDocument(verRef);
        assertFalse(ver.isDirty());
        // read a complex prop
        Property prop = ver.getProperty("tp:complex");
        // check that this does not mark the doc dirty
        assertFalse(ver.isDirty());
        assertFalse(prop.isDirty());
        // modify the version (allowed property)
        ver.setPropertyValue("dc:issued", new Date());
        // try to re-save the version
        // works if the complex document part is not dirty
        ver = session.saveDocument(ver);
    }

    @Test
    public void testComplexPropertyChain() throws Exception {
        Property p = doc.getProperty("tp:complexChain");
        assertTrue(p.getValue() instanceof Map);
        assertEquals(2, ((Map) p.getValue()).size());
        p.setValue("string", "test");
        Map<String, Serializable> map = new HashMap<>();
        map.put("string", "test2");
        p.setValue("complex", map);

        doc = session.saveDocument(doc);

        p = doc.getProperty("tp:complexChain");
        assertTrue(p.getValue() instanceof Map);
        assertEquals("test", p.getValue("string"));
        assertEquals("test2", p.getValue("complex/string"));
        p = p.get("complex");
        assertTrue(p.getValue() instanceof Map);
        assertEquals("test2", p.getValue("string"));
    }

    @Test
    public void testComplexPropertySubValue() throws Exception {
        doc.setPropertyValue("tp:complex/string", "test");
        doc = session.saveDocument(doc);
        assertEquals("test", doc.getPropertyValue("tp:complex/string"));
    }

    // NOTE that this test cannot pass if DEBUG_UUIDS=true due to the reset of the uuid counter
    @Test
    public void testComplexPropertySchemaUpdate() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());

        // create a doc
        doc.setPropertyValue("tp:complex/string", "test");
        doc = session.saveDocument(doc);
        session.save();

        waitForAsyncCompletion();
        coreFeature.releaseCoreSession();

        // add complexschema to TestDocument
        runtimeHarness.deployContrib("org.nuxeo.ecm.core.test.tests", "OSGI-INF/test-schema-update.xml");
        try {
            reloadService.reloadRepository();
            // reload repo with new doctype
            session = coreFeature.createCoreSession();

            doc = session.getDocument(new IdRef(doc.getId()));

            // this property did not exist on document creation, after updating the
            // doctype it should not fail
            Property prop = doc.getProperty("cmpf:attachedFile");
            Map<String, Object> expected = new HashMap<>();
            expected.put("name", null);
            expected.put("vignettes", Collections.emptyList());
            assertEquals(expected, prop.getValue());

            // check that we can write to it as well
            prop.setValue(Collections.singletonMap("vignettes",
                    Collections.singletonList(Collections.singletonMap("width", Long.valueOf(123)))));
            doc = session.saveDocument(doc);
        } finally {
            runtimeHarness.undeployContrib("org.nuxeo.ecm.core.test.tests", "OSGI-INF/test-schema-update.xml");
        }
    }

    // NXP-2318: i don't get what's supposed to be answered to these questions
    @Test
    @Ignore
    public void testArrayOrListProperties() throws Exception {
        Property prop = doc.getProperty("tp:stringArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:dateArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:intArray");
        assertFalse(prop.isContainer());
        assertFalse(prop.isList());
        assertTrue(prop.isScalar());

        prop = doc.getProperty("tp:complex");
        assertTrue(prop.isContainer());
        assertFalse(prop.isList());
        assertFalse(prop.isScalar());

        prop = doc.getProperty("tp:complexList");
        assertTrue(prop.isContainer());
        assertTrue(prop.isList());
        assertFalse(prop.isScalar());
    }

    @Test
    public void testExternalBlobDocumentProperty() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());

        File file = createTempFile();
        HashMap<String, String> map = new HashMap<>();
        String uri = String.format("fs:%s", file.getName());
        map.put(ExternalBlobProperty.URI, uri);
        map.put(ExternalBlobProperty.FILE_NAME, "hello.txt");
        doc.setPropertyValue("tp:externalcontent", map);
        doc = session.saveDocument(doc);

        Object blob = doc.getPropertyValue("tp:externalcontent");

        assertNotNull(blob);
        assertTrue(blob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) blob).getString());
        assertEquals("hello.txt", ((Blob) blob).getFilename());
        assertEquals("hello.txt", doc.getPropertyValue("tp:externalcontent/name"));
        assertEquals(uri, doc.getPropertyValue("tp:externalcontent/uri"));
    }

    // this time only set the uri
    @Test
    public void testExternalBlobDocumentProperty2() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());

        File file = createTempFile();
        String uri = String.format("fs:%s", file.getName());
        doc.setPropertyValue("tp:externalcontent/uri", uri);
        doc = session.saveDocument(doc);

        Object blob = doc.getPropertyValue("tp:externalcontent");

        assertNotNull(blob);
        assertTrue(blob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) blob).getString());
        assertEquals(file.getName(), ((Blob) blob).getFilename());
        assertNull(doc.getPropertyValue("tp:externalcontent/name"));
        assertEquals(uri, doc.getPropertyValue("tp:externalcontent/uri"));
    }

    // ignore externalblob stuff
    @Ignore
    @Test
    public void testExternalBlobListValue() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());

        // not null on list
        String propName = "tp:externalFileList";
        assertTrue(doc.getPropertyValue(propName) instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue(propName)).size());

        File file = createTempFile();
        ArrayList<Map> values = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        String uri = String.format("fs:%s", file.getName());
        map.put(ExternalBlobProperty.URI, uri);
        map.put(ExternalBlobProperty.FILE_NAME, "hello.txt");
        values.add(map);

        doc.setPropertyValue(propName, values);
        doc = session.saveDocument(doc);

        Serializable actual = doc.getPropertyValue(propName);
        assertTrue(actual instanceof List);
        List<Blob> blobs = (List) actual;
        assertEquals(1, blobs.size());
        assertNotNull(blobs.get(0));
        assertTrue(blobs.get(0) instanceof Blob);
        Blob actualBlob = blobs.get(0);
        assertEquals("Hello External Blob", actualBlob.getString());
        assertEquals("hello.txt", actualBlob.getFilename());
        assertEquals("hello.txt", doc.getPropertyValue(propName + "/0/name"));
        assertEquals(uri, doc.getPropertyValue(propName + "/0/uri"));
    }

    // ignore externalblob stuff
    @Ignore
    @Test
    public void testSubExternalBlobValue() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isVCS());

        String propName = "tp:externalFileComplexList";
        // not null on list
        assertTrue(doc.getPropertyValue(propName) instanceof List);
        assertEquals(0, ((List) doc.getPropertyValue(propName)).size());
        ArrayList<Map<String, Serializable>> values = new ArrayList<>();
        Map<String, Serializable> item = new HashMap<>();

        File file = createTempFile();
        HashMap<String, String> blobMap = new HashMap<>();
        String uri = String.format("fs:%s", file.getName());
        blobMap.put(ExternalBlobProperty.URI, uri);
        blobMap.put(ExternalBlobProperty.FILE_NAME, "hello.txt");

        item.put("blob", blobMap);
        item.put("filename", "My filename");
        values.add(item);
        doc.setPropertyValue(propName, values);
        doc = session.saveDocument(doc);

        Object actual = doc.getPropertyValue(propName);
        assertTrue(actual instanceof List);
        List<Map<String, Serializable>> items = (List) actual;
        assertEquals(1, items.size());
        assertNotNull(items.get(0));
        Map<String, Serializable> actualItem = items.get(0);
        assertEquals("My filename", actualItem.get("filename"));
        assertTrue(actualItem.get("blob") instanceof Blob);

        Object actualBlob = doc.getProperty(propName + "/0/blob").getValue();
        assertTrue(actualBlob instanceof Blob);
        assertEquals("Hello External Blob", ((Blob) actualBlob).getString());
        assertEquals("hello.txt", ((Blob) actualBlob).getFilename());
        assertEquals("hello.txt", doc.getPropertyValue(propName + "/0/blob/name"));
        assertEquals(uri, doc.getPropertyValue(propName + "/0/blob/uri"));
    }

    @Test
    public void testSaveComplexTwice() throws Exception {
        testComplexList();
        doc.setPropertyValue("tp:stringArray", new String[] {}); // dirty dp
        doc = session.saveDocument(doc); // rewrites complex list again
        session.save();
    }

    // not many tests, logs have to be looked at to confirm behavior
    @Test
    public void testUpdateMinimalChanges() throws Exception {
        // populate some properties
        testStringArray();
        testDateArray();
        testComplexList();
        testBlobListValue();
        session.save();

        reopenSession();
        // change just one of the collection properties
        doc.setPropertyValue("tp:stringArray", new String[] { "baz" });
        doc = session.saveDocument(doc);
        session.save();
        // check that the minimal number of updates are done in the db
    }

    // toplevel complex list
    @Test
    public void testXPath1() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        List<Object> files = new ArrayList<>(2);
        Map<String, Object> f = new HashMap<>();
        Blob blob = Blobs.createBlob("blob1");
        f.put("file", blob);
        files.add(f);
        doc.setProperty("files", "files", files);
        assertEquals(blob, doc.getPropertyValue("files/0/file"));
        assertEquals(blob, doc.getPropertyValue("files/item[0]/file"));
    }

    // other complex list
    @Test
    public void testXPath2() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");
        HashMap<String, Object> attachedFile = new HashMap<>();
        List<Map<String, Object>> vignettes = new ArrayList<>();
        attachedFile.put("vignettes", vignettes);
        Map<String, Object> vignette = new HashMap<>();
        Long width = Long.valueOf(123);
        vignette.put("width", width);
        vignettes.add(vignette);
        doc.setPropertyValue("cmpf:attachedFile", attachedFile);
        assertEquals(width, doc.getPropertyValue("cmpf:attachedFile/vignettes/0/width"));
        assertEquals(width, doc.getPropertyValue("cmpf:attachedFile/vignettes/vignette[0]/width"));
    }

    private static String canonXPath(String xpath) {
        return ComplexTypeImpl.canonicalXPath(xpath);
    }

    @Test
    public void testCanonicalizeXPath() throws Exception {
        assertEquals("foo", canonXPath("foo"));
        assertEquals("foo", canonXPath("/foo"));
        assertEquals("foo", canonXPath("//foo"));
        assertEquals("foo/bar", canonXPath("foo/bar"));
        assertEquals("foo/bar", canonXPath("/foo/bar"));
        assertEquals("foo/bar/baz", canonXPath("foo/bar/baz"));
        assertEquals("foo/0/bar", canonXPath("foo/0/bar"));
        assertEquals("foo/bar/0", canonXPath("foo/bar/0"));
        assertEquals("foo/0/bar", canonXPath("foo/gee[0]/bar"));
        assertEquals("foo/*/bar", canonXPath("foo/gee[*]/bar"));
        assertEquals("foo/bar/0", canonXPath("foo/bar/gee[0]"));
        assertEquals("foo/0/bar/123/moo", canonXPath("foo/gee[0]/bar/baz[123]/moo"));
        assertEquals("foo/0/bar/*/moo", canonXPath("foo/gee[0]/bar/baz[*]/moo"));
    }

    @Test
    public void testPrefetchDefault() throws Exception {
        doc = session.createDocument(session.createDocumentModel("/", "doc2", "TestDocumentWithDefaultPrefetch"));
        assertTrue(doc.isPrefetched("dc:title"));
        assertTrue(doc.isPrefetched("dc:description"));
        assertTrue(doc.isPrefetched("dc:created"));
        assertTrue(doc.isPrefetched("dc:modified"));
        assertTrue(doc.isPrefetched("dc:creator"));
        assertTrue(doc.isPrefetched("dc:lastContributor"));
        assertTrue(doc.isPrefetched("icon"));
        assertTrue(doc.isPrefetched("dublincore", "title"));
        assertTrue(doc.isPrefetched("common", "icon"));
        assertFalse(doc.isPrefetched("dc:contributors"));
        assertNull(doc.getPropertyValue("dc:title"));
        assertNull(doc.getProperty("dublincore", "title"));

        doc.setPropertyValue("dc:title", "foo");
        assertFalse(doc.isPrefetched("dc:title"));
        assertFalse(doc.isPrefetched("dublincore", "title"));
        assertEquals("foo", doc.getPropertyValue("dc:title"));
        assertEquals("foo", doc.getProperty("dublincore", "title"));

        // set using schema + name
        Calendar cal = Calendar.getInstance();
        doc.setProperty("dublincore", "modified", cal);
        assertFalse(doc.isPrefetched("dc:modified"));
        assertFalse(doc.isPrefetched("dublincore", "modified"));
        assertEquals(cal, doc.getPropertyValue("dc:modified"));
        assertEquals(cal, doc.getProperty("dublincore", "modified"));

        // with no schema prefix
        doc.setPropertyValue("icon", "myicon");
        assertFalse(doc.isPrefetched("icon"));
        assertFalse(doc.isPrefetched("common", "icon"));
        assertEquals("myicon", doc.getPropertyValue("icon"));
        assertEquals("myicon", doc.getProperty("common", "icon"));
    }

    @Test
    public void testPrefetchComplexProperty() throws Exception {
        doc = session.createDocumentModel("/", "doc2", "MyDocType");

        doc.setPropertyValue("book:author/pJob", "somejob");
        doc.setPropertyValue("dc:subjects", new String[] { "bar" });
        doc.setPropertyValue("participants", new String[] { "bar" });
        Blob blob = Blobs.createBlob("foo");
        blob.setFilename("fooname");
        LinkedList<Object> blobs = new LinkedList<>();
        blobs.add(blob);
        doc.setPropertyValue("attachments", blobs);

        doc = session.createDocument(doc);
        doc = session.getDocument(doc.getRef());

        assertTrue(doc.isPrefetched("dc:title"));
        // assertTrue(doc.isPrefetched("dc:subjects"));
        // assertTrue(doc.isPrefetched("attachments/0/name"));
        assertTrue(doc.isPrefetched("book:author/pJob"));
        // assertEquals("fooname", doc.getPropertyValue("attachments/0/name"));
        assertEquals("somejob", doc.getPropertyValue("book:author/pJob"));

        Serializable participants = doc.getPropertyValue("participants");
        assertNotNull(participants);
        assertTrue(participants.getClass().getName(), participants instanceof String[]);
        String[] array = (String[]) participants;
        assertEquals(Arrays.asList("bar"), Arrays.asList(array));
        // array mutability
        array[0] = "moo";
        // different mutable array returned each time
        participants = doc.getPropertyValue("participants");
        array = (String[]) participants;
        assertEquals(Arrays.asList("bar"), Arrays.asList(array));

        // set another prop in same schema
        doc.setPropertyValue("book:author/pAge", null);
        // not prefetched anymore as schema was loaded
        assertFalse(doc.isPrefetched("book:author/pJob"));
    }

    @Test
    public void testRestriction() throws Exception {
        doc = session.createDocumentModel("/", "doc2", "Restriction");
        doc.setPropertyValue("restr:shortstring", "foo");
        doc = session.createDocument(doc);
        doc = session.getDocument(doc.getRef());
        String value = doc.getProperty("restr:shortstring").getValue(String.class);
        assertEquals("foo", value);
    }

    @Test
    public void testPropertyIsSameAsBlob() throws Exception {
        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        DocumentPart part = doc.getPart("file");
        assertTrue(part.isSameAs(part));

        DocumentModel doc2 = session.createDocumentModel("/", "file2", "File");
        Blob blob2 = Blobs.createBlob("hello world!");
        doc2.setPropertyValue("file:content", (Serializable) blob2);
        doc2 = session.createDocument(doc2);
        DocumentPart part2 = doc2.getPart("file");
        assertTrue(part2.isSameAs(part2));

        assertFalse(part2.isSameAs(part));
        assertFalse(part.isSameAs(part2));

        // same blob content, should compare equal

        DocumentModel doc3 = session.createDocumentModel("/", "file3", "File");
        Blob blob3 = Blobs.createBlob("hello world!");
        doc3.setPropertyValue("file:content", (Serializable) blob3);
        doc3 = session.createDocument(doc3);
        DocumentPart part3 = doc3.getPart("file");
        assertTrue(part2.isSameAs(part3));

        // different blob content

        DocumentModel doc4 = session.createDocumentModel("/", "file3", "File");
        Blob blob4 = Blobs.createBlob("this is goodbye");
        doc4.setPropertyValue("file:content", (Serializable) blob4);
        doc4 = session.createDocument(doc4);
        DocumentPart part4 = doc4.getPart("file");
        assertFalse(part2.isSameAs(part4));

        // compare directly two StorageBlobs
        // same
        assertEquals(doc2.getPropertyValue("file:content"), doc3.getPropertyValue("file:content"));
        // different
        assertFalse(doc2.getPropertyValue("file:content").equals(doc4.getPropertyValue("file:content")));

        // compare a StringBlob and a StorageBlob
        assertEquals(blob2, doc3.getPropertyValue("file:content"));
        assertEquals(doc3.getPropertyValue("file:content"), blob2);

        // compare a StringBlob and a StorageBlob
        assertEquals(blob3, doc3.getPropertyValue("file:content"));
        assertEquals(doc3.getPropertyValue("file:content"), blob3);

        // compare a StringBlob and a StringBlob
        assertEquals(blob2, blob3);
        assertEquals(blob3, blob2);

        // compare a StringBlob with a different StringBlob
        assertFalse(blob2.equals(blob4));
        assertFalse(blob4.equals(blob2));
        assertFalse(blob3.equals(blob4));
        assertFalse(blob4.equals(blob3));
    }

    @Test
    public void testPropertyDelta() throws Exception {
        int base = 100;
        int delta = 123;
        doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc.setPropertyValue("my:integer", Long.valueOf(base));
        doc = session.createDocument(doc);
        session.save();

        doc.setPropertyValue("my:integer", DeltaLong.valueOf(Long.valueOf(base), delta));

        // re-reading the property before saveDocument() returns the Delta
        Serializable value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof DeltaLong);
        assertEquals(DeltaLong.valueOf(Long.valueOf(base), delta), value);

        doc = session.saveDocument(doc);

        // write another property in the same schema
        // to make sure the delta is not applied twice
        doc.setPropertyValue("my:string", "foo");
        doc = session.saveDocument(doc);

        // after saveDocument() we still have a DeltaLong which is needed to keep base context
        value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof DeltaLong);
        assertEquals(DeltaLong.valueOf(Long.valueOf(base), delta), value);

        // even if we refetch it's still a DeltaLong
        doc = session.getDocument(new IdRef(doc.getId()));
        value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof DeltaLong);
        assertEquals(DeltaLong.valueOf(Long.valueOf(base), delta), value);

        session.save();
        doc = session.getDocument(new IdRef(doc.getId()));

        // after save() and refetch we now have a Long
        value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(Long.valueOf(base + delta), value);

        // write another property in the same schema
        // to make sure the delta is not applied twice
        doc.setPropertyValue("my:string", "bar");
        doc = session.saveDocument(doc);
        session.save();

        reopenSession();

        // after refetch it's a Long with the correct incremented value
        doc = session.getDocument(new IdRef(doc.getId()));
        value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(Long.valueOf(base + delta), value);
    }

    @Test
    public void testPropertyDeltaTwice() throws Exception {
        int base = 100;
        int delta = 123;
        doc = session.createDocumentModel("/", "doc", "MyDocType");
        Long v1 = Long.valueOf(base);
        doc.setPropertyValue("my:integer", v1);
        doc = session.createDocument(doc);
        session.save();

        DeltaLong v2 = DeltaLong.valueOf(v1, delta);
        doc.setPropertyValue("my:integer", v2);
        doc = session.saveDocument(doc);
        DeltaLong v3 = DeltaLong.valueOf(v2, delta);
        doc.setPropertyValue("my:integer", v3);
        doc = session.saveDocument(doc);

        session.save();

        reopenSession();

        // after refetch it's a Long with the correct incremented value
        doc = session.getDocument(new IdRef(doc.getId()));
        Serializable value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(Long.valueOf(base + delta * 2), value);
    }

    @Test
    public void testPropertyDeltaWithoutSave() throws Exception {
        doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc.setPropertyValue("my:integer", Long.valueOf(100));
        doc = session.createDocument(doc);
        session.save();

        // reset to 0 then apply increment without intervening database save
        doc.setPropertyValue("my:integer", Long.valueOf(0));
        doc = session.saveDocument(doc);
        doc.setPropertyValue("my:integer", DeltaLong.valueOf(Long.valueOf(0), 123));
        doc = session.saveDocument(doc);
        session.save();

        reopenSession();

        // refetch
        doc = session.getDocument(new IdRef(doc.getId()));
        Serializable value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(Long.valueOf(123), value);
    }

    @Test
    public void testPropertyDeltaAfterNull() throws Exception {
        doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc.setPropertyValue("my:integer", Long.valueOf(0));
        doc = session.createDocument(doc);
        session.save();

        // set to null
        doc.setPropertyValue("my:integer", null);
        doc = session.saveDocument(doc);
        session.save();

        // now apply the delta
        doc.setPropertyValue("my:integer", DeltaLong.valueOf(null, 123));
        doc = session.saveDocument(doc);
        session.save();

        reopenSession();

        // after refetch it's a Long with the correct value
        doc = session.getDocument(new IdRef(doc.getId()));
        Serializable value = doc.getPropertyValue("my:integer");
        assertNotNull(value);
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(123, ((Long) value).longValue());
    }

    /**
     * Checks that writing several documents using batching with some of them having Delta and some not doesn't fail.
     */
    @Test
    public void testPropertyDeltaBatching() throws Exception {
        int n = 10;
        int base = 100;
        for (int i = 0; i < n; i++) {
            DocumentModel doc = session.createDocumentModel("/", "doc" + i, "MyDocType");
            doc.setPropertyValue("my:integer", Long.valueOf(base));
            doc = session.createDocument(doc);
        }
        session.save();

        // updates

        for (int i = 0; i < n; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/doc" + i));
            Serializable value;
            if (i < n / 2) {
                value = Long.valueOf(i);
            } else {
                // delta whose base is not the actual base, to check
                // that we really do an increment instead of setting
                // the full value
                value = DeltaLong.valueOf(Long.valueOf(base), i);
            }
            doc.setPropertyValue("my:integer", value);
            if (i % 2 == 0) {
                // also sometimes change another property
                doc.setPropertyValue("my:string", "foo" + i);
            }
            doc = session.saveDocument(doc);
        }
        session.save();

        // check result after re-reading from database
        reopenSession();

        for (int i = 0; i < n; i++) {
            DocumentModel doc = session.getDocument(new PathRef("/doc" + i));
            Serializable value = doc.getPropertyValue("my:integer");
            Serializable expected;
            if (i < n / 2) {
                expected = Long.valueOf(i);
            } else {
                expected = Long.valueOf(base + i);
            }
            assertEquals(doc.getName(), expected, value);
        }
    }

    /**
     * Checks that even on document creation using a Delta doesn't fail.
     */
    @Test
    public void testPropertyDeltaOnCreate() throws Exception {
        doc = session.createDocumentModel("/", "doc", "MyDocType");
        doc.setPropertyValue("my:integer", DeltaLong.valueOf(Long.valueOf(100), 123));
        doc = session.createDocument(doc);
        session.save();

        Serializable value = doc.getPropertyValue("my:integer");
        assertTrue(value.getClass().getName(), value instanceof Long);
        assertEquals(223, ((Long) value).longValue());
    }

    // DBS-only test for in-db data migration
    @Test
    public void testMigrationListVsString() throws Exception {
        assumeTrue(coreFeature.getStorageConfiguration().isDBS());

        // create a doc
        DocumentModel doc = session.createDocumentModel("/", "domain", "MyDocType");
        doc = session.createDocument(doc);
        String id = doc.getId();
        session.save();

        nextTransaction();

        // change data
        StateDiff diff = new StateDiff();
        diff.put("dc:subjects", "a"); // put a string instead of an array
        diff.put("dc:title", new String[] { "aa", "bb" }); // put an array instead of a string
        changeDoc(id, diff);

        // check that we don't crash on read
        doc = session.getDocument(doc.getRef());
        assertEquals(Arrays.asList("a"), Arrays.asList((Object[]) doc.getPropertyValue("dc:subjects")));
        assertEquals("aa", doc.getPropertyValue("dc:title"));
        assertEquals(Collections.emptyList(), Arrays.asList((Object[]) doc.getPropertyValue("dc:contributors")));
    }

    // change data in the database to make it like we just migrated the fields from different types
    protected void changeDoc(String id, StateDiff diff) throws Exception {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(session.getRepositoryName());
        ((DBSRepository) repository).updateState(id, diff);
    }

    @Test
    public void testComplexListArrayElementResetWithNull() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "MyDocType");
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "foo" })));
        doc = session.createDocument(doc);
        session.save();

        // set map value to null
        doc.setPropertyValue("complexlist", (Serializable) Arrays.asList(Collections.singletonMap("array", null)));
        doc = session.saveDocument(doc);
        session.save();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) doc.getPropertyValue("complexlist");
        assertEquals(1, list.size());
        Map<String, Serializable> map = list.get(0);
        assertEquals(0, ((String[]) map.get("array")).length);
    }

    @Test
    public void testComplexListArrayElementResetWithEmptyArray() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "MyDocType");
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "foo" })));
        doc = session.createDocument(doc);
        session.save();

        // set map value to empty array
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] {})));
        doc = session.saveDocument(doc);
        session.save();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) doc.getPropertyValue("complexlist");
        assertEquals(1, list.size());
        Map<String, Serializable> map = list.get(0);
        assertEquals(0, ((String[]) map.get("array")).length);
    }

    @Test
    public void testComplexListArrayElementModify() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "MyDocType");
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "foo" })));
        doc = session.createDocument(doc);
        session.save();

        // replace map value with new array of same size
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "bar" })));
        doc = session.saveDocument(doc);
        session.save();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) doc.getPropertyValue("complexlist");
        assertEquals(1, list.size());
        Map<String, Serializable> map = list.get(0);
        assertEquals(1, ((String[]) map.get("array")).length);
        assertEquals("bar", ((String[]) map.get("array"))[0]);
    }

    @Test
    public void testComplexListArrayElementGrow() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "MyDocType");
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "foo" })));
        doc = session.createDocument(doc);
        session.save();

        // replace map value with new array of bigger size
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "bar", "baz" })));
        doc = session.saveDocument(doc);
        session.save();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) doc.getPropertyValue("complexlist");
        assertEquals(1, list.size());
        Map<String, Serializable> map = list.get(0);
        assertEquals(2, ((String[]) map.get("array")).length);
        assertEquals("bar", ((String[]) map.get("array"))[0]);
        assertEquals("baz", ((String[]) map.get("array"))[1]);
    }

    @Test
    public void testComplexListArrayElementShrink() {
        DocumentModel doc = session.createDocumentModel("/", "mydoc", "MyDocType");
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "foo", "bar" })));
        doc = session.createDocument(doc);
        session.save();

        // replace map value with new array of smaller size
        doc.setPropertyValue("complexlist",
                (Serializable) Arrays.asList(Collections.singletonMap("array", new String[] { "baz" })));
        doc = session.saveDocument(doc);
        session.save();

        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> list = (List<Map<String, Serializable>>) doc.getPropertyValue("complexlist");
        assertEquals(1, list.size());
        Map<String, Serializable> map = list.get(0);
        assertEquals(1, ((String[]) map.get("array")).length);
        assertEquals("baz", ((String[]) map.get("array"))[0]);
    }

}
