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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.ReadOnlyPropertyException;
import org.nuxeo.ecm.core.api.model.ValueExporter;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.DateProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.LongProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Bogdan Stefanescu
 */
// We're declaring variables as HashMaps / ArrayLists so they can be Serializable
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-propmodel-types-contrib.xml")
public class TestPropertyModel {

    protected RuntimeService runtime;

    protected Schema schema;

    protected DocumentPartImpl dp;

    static <T> ArrayList<T> arrayList(T... args) {
        ArrayList<T> list = new ArrayList<>(args.length);
        list.addAll(Arrays.asList(args));
        return list;
    }

    private static class Name {
        String firstName;

        String lastName;

        HashMap<String, Serializable> getMap() {
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("lastName", lastName);
            map.put("firstName", firstName);
            return map;
        }
    }

    private static class Author {
        Name name = new Name();

        Long age;

        Author() {
        }

        Author(long age) {
            this.age = Long.valueOf(age);
        }

        HashMap<String, Serializable> getMap() {
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("name", name.getMap());
            map.put("age", age);
            return map;
        }
    }

    private class FileName implements Serializable {
        private static final long serialVersionUID = -3238719896844696496L;

        String extension;

        HashMap<String, Serializable> getMap() {
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("name", null);
            map.put("extension", extension);
            return map;
        }
    }

    private class BlobFile implements Serializable {
        private static final long serialVersionUID = 4486693420148155780L;

        final FileName fileName = new FileName();

        Blob blob;

        HashMap<String, Serializable> getMap() {
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("fileName", fileName.getMap());
            map.put("blob", (Serializable) blob);
            return map;
        }
    }

    private class Book {

        private String title;

        private Calendar creationDate;

        private Long price;

        private String[] keywords;

        private ArrayList<String> references;

        private ArrayList<Author> authors;

        private BlobFile file;

        HashMap<String, Serializable> getMap() {
            HashMap<String, Serializable> map = new HashMap<>();
            map.put("book:title", title);
            map.put("book:creationDate", creationDate);
            map.put("book:price", price);
            map.put("book:keywords", keywords);
            if (references == null) {
                map.put("book:references", new ArrayList<Serializable>());
            } else {
                map.put("book:references", references);
            }
            map.put("book:file", file != null ? file.getMap() : new HashMap<String, Serializable>());
            if (authors == null) {
                map.put("book:authors", new ArrayList<HashMap<String, Serializable>>());
            } else {
                ArrayList<HashMap<String, Serializable>> list = new ArrayList<>();
                for (Author author : authors) {
                    list.add(author.getMap());
                }
                map.put("book:authors", list);
            }
            return map;
        }
    }

    @Before
    public void setUp() throws Exception {
        SchemaManager mgr = Framework.getService(SchemaManager.class);
        // XSDLoader loader = new XSDLoader((SchemaManagerImpl) mgr);
        // schema = loader.loadSchema("test", "book",
        // getResource("TestSchema.xsd"));
        schema = mgr.getSchema("test");
        dp = new DocumentPartImpl(schema);
    }

    @SuppressWarnings("unchecked")
    protected static void clearMap(Map<String, Serializable> map) {
        Iterator<Map.Entry<String, Serializable>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Serializable> entry = it.next();
            Serializable v = entry.getValue();
            if (v == null) {
                it.remove();
            } else if (v instanceof Map) {
                clearMap((Map<String, Serializable>) v);
            } else if (v instanceof List) {
                for (Serializable el : (List<Serializable>) v) {
                    if (el instanceof Map) {
                        clearMap((Map<String, Serializable>) el);
                    }
                }
            }
        }
    }

    protected static Map<String, Serializable> unPrefixedMap(Map<String, Serializable> map) {
        Map<String, Serializable> res = new HashMap<>();
        for (Entry<String, Serializable> e : map.entrySet()) {
            String key = e.getKey();
            int pos = key.indexOf(':');
            if (pos > -1) {
                key = key.substring(pos + 1);
            }
            res.put(key, e.getValue());
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    protected static boolean valueEquals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        if (o1 instanceof Map) {
            if (!(o2 instanceof Map)) {
                return false;
            }
            Map<String, Serializable> map1 = (Map<String, Serializable>) o1;
            Map<String, Serializable> map2 = (Map<String, Serializable>) o2;
            if (map1.size() != map2.size()) {
                return false;
            }
            for (String key : map1.keySet()) {
                if (!valueEquals(map1.get(key), map2.get(key))) {
                    return false;
                }
            }
        } else if (o1 instanceof List) {
            if (!(o2 instanceof List)) {
                return false;
            }
            List<Serializable> list1 = (List<Serializable>) o1;
            List<Serializable> list2 = (List<Serializable>) o2;
            if (list1.size() != list2.size()) {
                return false;
            }
            for (int i = 0; i < list1.size(); i++) {
                if (!valueEquals(list1.get(i), list2.get(i))) {
                    return false;
                }
            }
        } else if (!o1.equals(o2)) {
            return false;
        }
        return true;
    }

    public static URL getResource(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource);
    }

    @Test
    public void testXPath() throws Exception {
        Property prop = dp.get("file");
        assertEquals("book:file", prop.getXPath());
        prop = prop.get("fileName");
        assertEquals("book:file/fileName", prop.getXPath());

        prop = dp.resolvePath("file/blob");
        assertEquals("book:file/blob", prop.getXPath());

        Author author = new Author();
        dp.get("authors").addValue(author.getMap());
        prop = dp.get("book:authors");
        assertEquals("book:authors", prop.getXPath());
        prop = prop.get("0");
        assertEquals("book:authors/0", prop.getXPath());
        prop = prop.get("name");
        assertEquals("book:authors/0/name", prop.getXPath());
        prop = prop.get("firstName");
        assertEquals("book:authors/0/name/firstName", prop.getXPath());

        // schema without prefix: no prefix either in xpath
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        DocumentPartImpl dp2 = new DocumentPartImpl(schemaManager.getSchema("wihtoutpref"));
        prop = dp2.get("blobname");
        assertEquals("blobname", prop.getXPath());
        prop = dp2.resolvePath("blob/mime-type");
        assertEquals("blob/mime-type", prop.getXPath());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPath() throws Exception {
        String path = dp.get("file").get("fileName").getPath();
        assertEquals("/book:file/fileName", path);

        Author author = new Author();
        dp.get("authors").addValue(author.getMap());
        path = dp.resolvePath("book:authors/author[0]/name/firstName").getPath();
        assertEquals("/book:authors/author[0]/name/firstName", path);
    }

    @Test
    public void testPropertyAccess() throws Exception {
        // test complex property access
        Property prop = dp.get("title");
        assertTrue(prop.isScalar());
        assertTrue(prop.isPhantom());
        assertNull(prop.getValue());

        prop = dp.get("price");
        assertTrue(prop.isScalar());
        assertTrue(prop.isPhantom());
        assertEquals(111L, prop.getValue());

        Property authorsProp = dp.get("authors");
        assertTrue(authorsProp.isList());
        assertTrue(authorsProp.isPhantom());

        Property keysProp = dp.get("keywords");
        assertTrue(keysProp.isList());
        assertTrue(keysProp.isPhantom());

        Property refsProp = dp.get("references");
        assertTrue(refsProp.isList());
        assertTrue(refsProp.isPhantom());

        prop = dp.get("creationDate");
        assertTrue(prop.isScalar());
        assertTrue(prop.isPhantom());
        assertNull(prop.getValue());

        Property fileProp = dp.get("file");
        assertTrue(fileProp.isComplex());
        assertTrue(fileProp.isPhantom());

        Property fileNameProp = fileProp.get("fileName");
        assertTrue(fileNameProp.isComplex());
        assertTrue(fileNameProp.isPhantom());

        prop = fileNameProp.get("name");
        assertTrue(prop.isScalar());
        assertTrue(prop.isPhantom());
        assertNull(prop.getValue());

        prop = fileNameProp.get("extension");
        assertTrue(prop.isScalar());
        assertTrue(prop.isPhantom());
        assertNull(prop.getValue());

        // test getRoot
        assertEquals(dp, prop.getRoot());

        // test raw values
        Map<String, Serializable> expected = new Book().getMap();
        expected.put("book:price", 111L);
        assertTrue(valueEquals(expected, dp.getValue()));

        // test resolve path
        prop = dp.resolvePath("title");
        assertEquals(prop, dp.get("title"));
        assertEquals(fileNameProp, dp.resolvePath("file/fileName"));
        assertEquals(fileNameProp.get("name"), dp.resolvePath("file/fileName/name"));
        assertEquals(fileNameProp.get("name"), fileNameProp.resolvePath("name"));
        assertEquals(fileNameProp.get("name"), fileNameProp.resolvePath("../fileName/name"));
        assertEquals(dp, fileNameProp.resolvePath("../.."));
        assertEquals(dp.get("title"), fileNameProp.resolvePath("../../title"));

        // using prefixed names
        assertEquals(fileNameProp.resolvePath("name/../extension"), dp.resolvePath("book:file/fileName/extension"));

        // testing list access - for this we need a phantom property??
        assertEquals(keysProp, dp.resolvePath("book:keywords"));

        // testing list access - for this we need a phantom property??
        prop = refsProp.addValue("key1");
        assertEquals(prop, dp.resolvePath("book:references/reference[0]"));

        prop = authorsProp.addValue(new Author().getMap());
        assertEquals(prop, dp.resolvePath("authors/author[0]"));

        prop = prop.get("name").get("firstName");
        assertEquals(prop, dp.resolvePath("authors/author[0]/name/firstName"));

        // absolute paths
        assertEquals(prop.resolvePath("/book:title"), dp.get("title"));
    }

    @Test
    public void testPropertyValueAccess() throws Exception {
        // test setters
        Property prop = dp.get("title");
        prop.setValue("The title");
        assertEquals("The title", prop.getValue());

        dp.setValue("title", "The title 2");
        assertEquals("The title 2", prop.getValue());

        dp.setValue("file/fileName/extension", "jpg");
        assertEquals("jpg", dp.getValue("book:file/fileName/extension"));

        Author author = new Author();
        author.age = 100L;
        author.name = new Name();
        author.name.firstName = "Toto";
        prop = dp.get("authors").addValue(author.getMap());
        assertEquals(prop.getValue(), author.getMap());

        assertEquals("Toto", prop.resolvePath("name/firstName").getValue());
        assertNull(prop.get("name").get("lastName").getValue());

        // test set(index)
        author.name.firstName = null;
        author.name.lastName = "Titi";
        dp.get("authors").setValue(0, author.getMap());
        assertEquals("Titi", prop.resolvePath("name/lastName").getValue());
        assertNull(prop.get("name").get("firstName").getValue());
        // set using xpath
        dp.setValue("authors/author[0]/name/lastName", "Tete");
        assertEquals("Tete", dp.getValue("authors/author[0]/name/lastName"));

        // test add(index)
        author.name.lastName = "Toto";
        dp.get("authors").addValue(0, author.getMap());
        assertEquals("Toto", dp.resolvePath("authors/author[0]/name/lastName").getValue());
        assertEquals("Tete", dp.resolvePath("authors/author[1]/name/lastName").getValue());
    }

    @Test
    public void testReadOnlyValue() throws Exception {
        Property prop = dp.resolvePath("file/fileName/extension");
        try {
            prop.setReadOnly(true);
            prop.setValue("test");
            fail();
        } catch (ReadOnlyPropertyException e) {
            // do nothing
        }
    }

    @Test
    public void testFlags() throws Exception {
        dp.setValue("file/fileName/extension", "ejb");
        assertTrue(dp.get("file").isDirty());
        assertTrue(dp.get("file").get("fileName").isDirty());
        assertTrue(dp.get("file").get("fileName").get("extension").isDirty());
        assertTrue(dp.get("file").get("fileName").get("name").isPhantom());
        assertEquals("ejb", dp.getValue("file/fileName/extension"));

        assertFalse(dp.get("file").isPhantom());
        assertFalse(dp.get("file").get("fileName").isPhantom());
        assertFalse(dp.get("file").get("fileName").get("extension").isPhantom());
    }

    @Test
    public void testDefaultFactories() throws Exception {
        Book book = new Book();
        Author author = new Author();
        author.name.firstName = "John";
        book.authors = new ArrayList<>();
        book.authors.add(author);
        book.title = "My Title";
        book.creationDate = Calendar.getInstance();
        book.price = 100L;
        BlobFile file = new BlobFile();
        file.fileName.extension = "xml";
        book.file = file;
        book.keywords = new String[] { "a", "b" };

        dp.setValue(book.getMap());

        assertTrue(dp.get("book:title") instanceof StringProperty);
        assertTrue(dp.get("book:price") instanceof LongProperty);
        assertTrue(dp.get("book:creationDate") instanceof DateProperty);
        assertTrue(dp.get("book:authors") instanceof ListProperty);
        assertTrue(dp.get("book:file") instanceof MapProperty);
    }

    /**
     * Compatibility test - this should be removed when ListDiff will be no more used in nuxeo.
     */
    @Test
    public void testListDiffCompatibility() throws Exception {
        Book book = new Book();
        book.authors = new ArrayList<>();
        book.authors.add(new Author(1));
        book.authors.add(new Author(2));
        book.authors.add(new Author(3));
        book.authors.add(new Author(4));
        book.authors.add(new Author(5));

        dp.setValue(book.getMap());

        Property prop = dp.get("authors");

        ListDiff ld = new ListDiff();
        ld.add(new Author(6).getMap()); // 123456
        ld.insert(4, new Author(7).getMap()); // 1234756
        ld.move(0, 1); // 2134756
        ld.remove(2); // 214756

        prop.setValue(ld);

        book.authors.add(new Author(6));
        book.authors.add(4, new Author(7));
        Author a = book.authors.get(0);
        book.authors.set(0, book.authors.get(1));
        book.authors.set(1, a);
        book.authors.remove(2);

        ArrayList<Serializable> authors = new ArrayList<>();
        for (Author author : book.authors) {
            authors.add(author.getMap());
        }

        assertEquals(authors, prop.getValue());
    }

    /**
     * Compatibility test - this should be removed when ListDiff will be no more used in nuxeo.
     */
    @Test
    public void testListDiffCompatibilityForScalarList() throws Exception {
        ArrayList<String> references = arrayList("a", "b", "c", "d", "e");
        dp.get("references").init(references);

        Property prop = dp.get("references");

        ListDiff ld = new ListDiff();
        ld.add("f"); // abcdef
        ld.insert(4, "g"); // abcdgef
        ld.move(0, 1); // bacdgef
        ld.remove(2); // badgef

        prop.setValue(ld);

        List<?> list = prop.getValue(List.class);

        assertEquals(arrayList("b", "a", "d", "g", "e", "f"), list);
    }

    @Test
    public void testScalarList() throws Exception {
        ArrayList<String> references = arrayList("a", "b", "c", "d", "e");
        dp.get("references").init(references);

        Property prop = dp.get("references");
        prop.setValue(references);

        assertEquals(references, prop.getValue(List.class));
        assertEquals(references, prop.getValue());

        // FIXME: NXP-1994: New property model makes list properties return null
        // values instead of empty list

        // setting an empty list at initialization time should not give null at
        // fetching time

        // ArrayList<String> emptyList = arrayList();
        // dp.get("references").init(emptyList);
        //
        // prop = dp.get("references");
        // prop.setValue(emptyList);
        //
        // assertEquals(emptyList, prop.getValue(List.class));
        // assertEquals(emptyList, prop.getValue());
    }

    @Test
    public void testBlob() throws Exception {
        Book book = new Book();
        BlobFile file = new BlobFile();
        file.fileName.extension = "xml";
        file.blob = Blobs.createBlob("abcdef", "plain/text", "UTF8");
        book.file = file;

        dp.setValue(book.getMap());

        Property pblob = dp.get("file").get("blob");
        assertEquals(file.blob, pblob.getValue());
        assertEquals(file.blob.getEncoding(), pblob.getValue("encoding"));
        assertEquals(file.blob.getMimeType(), pblob.getValue("mime-type"));

        Blob blob = Blobs.createBlob("xyz", "text/html", "UTF16");
        pblob.setValue(blob);
        assertEquals(blob.getEncoding(), pblob.getValue("encoding"));
        assertEquals(blob.getMimeType(), pblob.getValue("mime-type"));

        // TODO
        // Map<String, Object> map = new HashMap<String, Object>();
        // map.put("encoding", "LATIN2");
        // map.put("mime-type", "test/plain");
        // map.put("length", 123);
        // map.put("data", new ByteArrayInputStream("abc".getBytes()));
        // pblob.setValue(map);
        // assertEquals("LATIN2", pblob.getValue("encoding"));
    }

    @Test
    public void testSerialization() throws Exception {
        Book book = new Book();
        BlobFile file = new BlobFile();
        file.fileName.extension = "xml";
        file.blob = Blobs.createBlob("abcdef", "plain/text");
        book.file = file;

        dp.setValue(book.getMap());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(dp);

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        DocumentPartImpl dp2 = (DocumentPartImpl) in.readObject();

        // blobs are equals only if they are the same object so we need
        // to remove them before doing the assertion
        Blob blob1 = (Blob) dp.get("file").get("blob").remove();
        Blob blob2 = (Blob) dp2.get("file").get("blob").remove();
        // remove array also for the same reason as the blob
        Object[] keywords1 = (Object[]) dp.get("keywords").remove();
        Object[] keywords2 = (Object[]) dp2.get("keywords").remove();
        ArrayList<?> references1 = (ArrayList<?>) dp.get("references").remove();
        ArrayList<?> references2 = (ArrayList<?>) dp2.get("references").remove();

        assertEquals(dp2.getValue(), dp.getValue());

        // now check the blobs they are equals
        assertEquals(blob1.getEncoding(), blob2.getEncoding());
        assertEquals(blob1.getMimeType(), blob2.getMimeType());
        assertEquals(blob1.getLength(), blob2.getLength());
        assertEquals(blob1.getString(), blob2.getString());

        // now check arrays
        assertTrue(Arrays.equals(keywords1, keywords2));
        assertEquals(references1, references2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDirtyChildren() throws Exception {
        Iterator<Property> it = dp.getDirtyChildren();
        assertFalse(it.hasNext());

        Book book = new Book();
        Author author = new Author();
        author.name.firstName = "John";
        book.authors = new ArrayList<>();
        book.authors.add(author);
        book.title = "My Title";

        Map<String, Serializable> map = book.getMap();
        map.remove("book:price");
        map.remove("book:creationDate");
        map.remove("book:references");
        map.remove("book:keywords");
        map.remove("book:file");
        List<Serializable> list = (List<Serializable>) map.get("book:authors");
        Map<String, Serializable> amap = (Map<String, Serializable>) list.get(0);
        amap.remove("age");
        amap = (Map<String, Serializable>) amap.get("name");
        amap.remove("lastName");

        dp.setValue(map);

        it = dp.getDirtyChildren();
        List<Property> properties = new ArrayList<>();
        while (it.hasNext()) {
            properties.add(it.next());
        }
        assertTrue(properties.contains(dp.resolvePath("title")));
        assertTrue(properties.contains(dp.resolvePath("authors")));

        it = dp.get("authors").getDirtyChildren();
        Property p = it.next();
        assertFalse(it.hasNext());

        it = p.getDirtyChildren();
        p = it.next();
        assertFalse(it.hasNext());
        it = p.getDirtyChildren();
        p = it.next();
        assertEquals("John", p.getValue());
        assertFalse(it.hasNext());

        try {
            p.getDirtyChildren();
            fail("scalar property cannot have children");
        } catch (UnsupportedOperationException e) {
            // do nothing
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInit() throws Exception {
        Book book = new Book();
        Author author = new Author();
        author.name.firstName = "John";
        book.authors = new ArrayList<>();
        book.authors.add(author);
        book.title = "My Title";
        BlobFile file = new BlobFile();
        file.fileName.extension = "xml";
        book.file = file;
        book.keywords = new String[] { "a", "b" };
        book.references = arrayList("a", "b");

        HashMap<String, Serializable> map = book.getMap();
        // remove name so that it will be a phantom
        Map<String, Serializable> map2 = (Map<String, Serializable>) map.get("book:file");
        Map<String, Serializable> map3 = (Map<String, Serializable>) map2.get("fileName");
        map3.remove("name");

        dp.init(map);

        assertFalse(dp.isDirty());
        assertFalse(dp.get("file").isDirty());
        assertFalse(dp.get("file").get("fileName").isDirty());
        assertFalse(dp.get("file").get("fileName").get("extension").isDirty());
        assertFalse(dp.get("file").get("fileName").get("name").isDirty());

        assertFalse(dp.get("file").isPhantom());
        assertFalse(dp.get("file").get("fileName").isPhantom());
        assertFalse(dp.get("file").get("fileName").get("extension").isPhantom());
        assertFalse(dp.resolvePath("authors/auhtor[0]/name/firstName").isPhantom());
        assertTrue(dp.get("file").get("fileName").get("name").isPhantom());

        assertEquals("xml", dp.getValue("file/fileName/extension"));
        assertEquals("My Title", dp.getValue("title"));
        assertEquals("John", dp.getValue("authors/author[0]/name/firstName"));
        Object[] ar = (Object[]) dp.getValue("keywords");
        assertEquals("a", ar[0]);
        assertEquals("b", ar[1]);
        assertEquals("a", dp.getValue("references/reference[0]"));
        assertEquals("b", dp.getValue("references/reference[1]"));

        assertEquals(111L, dp.getValue("price"));
        assertNull(dp.getValue("authors/author[0]/name/lastName"));

        // test list size
        assertEquals(1, dp.get("authors").size());
        assertEquals(2, ar.length);
        assertEquals(2, dp.get("references").size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testExport() throws Exception {
        Book book = new Book();
        Author author = new Author();
        author.name.firstName = "John";
        book.authors = new ArrayList<>();
        book.authors.add(author);
        book.title = "My Title";
        BlobFile file = new BlobFile();
        file.fileName.extension = "xml";
        book.file = file;
        book.keywords = new String[] { "a", "b" };

        HashMap<String, Serializable> map = book.getMap();
        // remove name so that it will be a phantom
        ((Map) ((Map) map.get("book:file")).get("fileName")).remove("name");

        // remove null values - since they are related to phantom props
        clearMap(map);

        dp.init(map);

        // prefixed export
        ValueExporter ve = new ValueExporter(true);
        Map<String, Serializable> export = ve.run(dp);
        assertEquals(map, export);

        Map<String, Serializable> value = (Map<String, Serializable>) dp.getValue();
        clearMap(value);
        HashMap<String, Serializable> m = new HashMap<>(map);
        m.put("book:price", Long.valueOf(111)); // default value exported too
        assertEquals(m, value);

        // now unprefixed
        ve = new ValueExporter(false);
        export = ve.run(dp);
        assertEquals(unPrefixedMap(map), export);
    }

}
