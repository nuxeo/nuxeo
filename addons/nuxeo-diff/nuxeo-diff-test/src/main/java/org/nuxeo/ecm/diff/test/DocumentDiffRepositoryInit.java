/*
 * (C) Copyright 2012-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Inits the repository for a document diff test case with 2 documents of the same type.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffRepositoryInit extends DefaultRepositoryInit {

    public static final String LEFT_DOC_PATH = "/leftDoc";

    public static final String RIGHT_DOC_PATH = "/rightDoc";

    @Override
    public void populate(CoreSession session) {

        createLeftDoc(session);
        createRightDoc(session);

        TransactionHelper.commitOrRollbackTransaction();
        Framework.getService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();
    }

    /**
     * Creates the left doc.
     *
     * @param session the session
     * @return the document model
     */
    protected DocumentModel createLeftDoc(CoreSession session) {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:created", getCalendarNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 25));
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", getCalendarNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 25));
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "Administrator", "joe", null });
        doc.setPropertyValue("dc:subjects", new String[] { "Art", "Architecture" });

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:content", createBlob("Joe is rich.", "Joe.txt"));

        // -----------------------
        // files
        // -----------------------
        List<Map<String, Serializable>> files = List.of(
                createFileMetadata("Content of the first blob", "first_attachement.txt"),
                createFileMetadata("Content of the second blob", "second_attachement.txt"),
                createFileMetadata("Content of the third blob", "third_attachement.txt"),
                createFileMetadata("Content of the fourth blob", "fourth_attachement.txt"));
        doc.setPropertyValue("files:files", (Serializable) files);

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:boolean", true);
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", getCalendarNoMillis(2011, Calendar.DECEMBER, 28, 23, 00, 00));
        doc.setPropertyValue("st:htmlText",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;");
        doc.setPropertyValue("st:multivalued", new String[] { "monday", "tuesday", "wednesday", "thursday" });

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", true);
        complexPropValue.put("integerItem", 10);
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<>();
        item1ComplexPropValue.put("stringItem", "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", true);
        item1ComplexPropValue.put("integerItem", 12);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<>();
        complexListPropValue.add(item1ComplexPropValue);

        doc.setPropertyValue("ct:complexList", (Serializable) complexListPropValue);

        // -----------------------
        // listoflists
        // -----------------------
        List<Map<String, Serializable>> listOfListPropValue = new ArrayList<>();

        Map<String, Serializable> complexItem1 = new HashMap<>();
        complexItem1.put("stringItem", "first item");
        List<String> item1SubList = new ArrayList<>();
        item1SubList.add("Monday");
        item1SubList.add("Tuesday");
        complexItem1.put("stringListItem", (Serializable) item1SubList);
        listOfListPropValue.add(complexItem1);

        Map<String, Serializable> complexItem2 = new HashMap<>();
        complexItem2.put("stringItem", "second item");
        List<String> item2SubList = new ArrayList<>();
        item2SubList.add("Wednesday");
        item2SubList.add("Thursday");
        complexItem2.put("stringListItem", (Serializable) item2SubList);
        listOfListPropValue.add(complexItem2);

        doc.setPropertyValue("lol:listOfLists", (Serializable) listOfListPropValue);

        return session.createDocument(doc);
    }

    /**
     * Creates the right doc.
     *
     * @param session the session
     * @return the document model
     */
    protected DocumentModel createRightDoc(CoreSession session) {

        DocumentModel doc = session.createDocumentModel("/", "rightDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My second sample");
        doc.setPropertyValue("dc:created", getCalendarNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 50));
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", getCalendarNoMillis(2011, Calendar.DECEMBER, 30, 12, 05, 02));
        doc.setPropertyValue("dc:lastContributor", " Administrator ");
        doc.setPropertyValue("dc:contributors", new String[] { "anotherAdministrator", "joe", "jack" });
        doc.setPropertyValue("dc:subjects", new String[] { "Art" });

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:content", createBlob("Joe is rich, Jack is not.", "Jack.txt"));

        // -----------------------
        // files
        // -----------------------
        List<Map<String, Serializable>> files = List.of(
                createFileMetadata("Content of the first blob", "first_attachement.txt"),
                createFileMetadata("Content of the second blob", "the_file_name_is_different.txt"),
                createFileMetadata("Different content of the third blob", "third_attachement.txt"));
        doc.setPropertyValue("files:files", (Serializable) files);

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a different string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", getCalendarNoMillis(2011, Calendar.DECEMBER, 28, 23, 00, 00));
        doc.setPropertyValue("st:htmlText",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", false);
        complexPropValue.put("dateItem", getCalendarNoMillis(2011, Calendar.DECEMBER, 29, 23, 00, 00));
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<>();
        item1ComplexPropValue.put("stringItem", "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", false);
        item1ComplexPropValue.put("dateItem", getCalendarNoMillis(2011, Calendar.DECEMBER, 30, 23, 00, 00));

        Map<String, Serializable> item2ComplexPropValue = new HashMap<>();
        item2ComplexPropValue.put("stringItem", "second element of a complex list");
        item2ComplexPropValue.put("booleanItem", false);
        item2ComplexPropValue.put("integerItem", 20);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<>();
        complexListPropValue.add(item1ComplexPropValue);
        complexListPropValue.add(item2ComplexPropValue);

        doc.setPropertyValue("ct:complexList", (Serializable) complexListPropValue);

        // -----------------------
        // listoflists
        // -----------------------
        List<Map<String, Serializable>> listOfListPropValue = new ArrayList<>();

        Map<String, Serializable> complexItem1 = new HashMap<>();
        complexItem1.put("stringItem", "first item");
        List<String> item1SubList = new ArrayList<>();
        item1SubList.add("Monday");
        item1SubList.add("Tuesday");
        complexItem1.put("stringListItem", (Serializable) item1SubList);
        listOfListPropValue.add(complexItem1);

        Map<String, Serializable> complexItem2 = new HashMap<>();
        complexItem2.put("stringItem", "second item is different");
        List<String> item2SubList = new ArrayList<>();
        item2SubList.add("Wednesday");
        item2SubList.add("Friday");
        item2SubList.add("Saturday");
        complexItem2.put("stringListItem", (Serializable) item2SubList);
        listOfListPropValue.add(complexItem2);

        Map<String, Serializable> complexItem3 = new HashMap<>();
        complexItem3.put("stringItem", "third item");
        List<String> item3SubList = new ArrayList<>();
        item2SubList.add("July");
        item2SubList.add("August");
        complexItem3.put("stringListItem", (Serializable) item3SubList);
        listOfListPropValue.add(complexItem3);

        doc.setPropertyValue("lol:listOfLists", (Serializable) listOfListPropValue);

        return session.createDocument(doc);
    }

    /**
     * Gets a calendar instance with 0 milliseconds.
     *
     * @param year the year
     * @param month the month
     * @param day the day
     * @param hourOfDay the hour of day
     * @param minute the minute
     * @param second the second
     * @return the calendar
     */
    public static Calendar getCalendarNoMillis(int year, int month, int day, int hourOfDay, int minute, int second) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hourOfDay, minute, second);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    protected Map<String, Serializable> createFileMetadata(String content, String filename) {
        Blob blob = createBlob(content, filename);
        return Map.of("file", (Serializable) blob);
    }

    @SuppressWarnings("unchecked")
    protected <B extends Blob & Serializable> B createBlob(String content, String filename) {
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(filename);
        return (B) blob;
    }

}
