/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.diff.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Inits the repository for a document diff test case with 2 documents of the same type.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffRepositoryInit extends DefaultRepositoryInit {

    public static String getLeftDocPath() {
        return "/leftDoc";
    }

    public static String getRightDocPath() {
        return "/rightDoc";
    }

    @Override
    public void populate(CoreSession session) throws ClientException {

        createLeftDoc(session);
        createRightDoc(session);
    }

    /**
     * Creates the left doc.
     *
     * @param session the session
     * @return the document model
     * @throws ClientException the client exception
     */
    protected DocumentModel createLeftDoc(CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:created", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 25));
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 25));
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "Administrator", "joe", null });
        doc.setPropertyValue("dc:subjects", new String[] { "Art", "Architecture" });

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:filename", "Joe.txt");
        Blob blob = Blobs.createBlob("Joe is rich.");
        blob.setFilename("Joe.txt");
        doc.setPropertyValue("file:content", (Serializable) blob);

        // -----------------------
        // files
        // -----------------------
        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("filename", "first_attachement.txt");
        blob = Blobs.createBlob("Content of the first blob");
        blob.setFilename("first_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        file = new HashMap<String, Serializable>();
        file.put("filename", "second_attachement.txt");
        blob = Blobs.createBlob("Content of the second blob");
        blob.setFilename("second_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        file = new HashMap<String, Serializable>();
        file.put("filename", "third_attachement.txt");
        blob = Blobs.createBlob("Content of the third blob");
        blob.setFilename("third_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        file = new HashMap<String, Serializable>();
        file.put("filename", "fourth_attachement.txt");
        blob = Blobs.createBlob("Content of the fourth blob");
        blob.setFilename("fourth_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        doc.setPropertyValue("files:files", (Serializable) files);

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:boolean", true);
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 28, 23, 00, 00));
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;");
        doc.setPropertyValue("st:multivalued", new String[] { "monday", "tuesday", "wednesday", "thursday" });

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<String, Serializable>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", true);
        complexPropValue.put("integerItem", 10);
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<String, Serializable>();
        item1ComplexPropValue.put("stringItem", "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", true);
        item1ComplexPropValue.put("integerItem", 12);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<Map<String, Serializable>>();
        complexListPropValue.add(item1ComplexPropValue);

        doc.setPropertyValue("ct:complexList", (Serializable) complexListPropValue);

        // -----------------------
        // listoflists
        // -----------------------
        List<Map<String, Serializable>> listOfListPropValue = new ArrayList<Map<String, Serializable>>();

        Map<String, Serializable> complexItem1 = new HashMap<String, Serializable>();
        complexItem1.put("stringItem", "first item");
        List<String> item1SubList = new ArrayList<String>();
        item1SubList.add("Monday");
        item1SubList.add("Tuesday");
        complexItem1.put("stringListItem", (Serializable) item1SubList);
        listOfListPropValue.add(complexItem1);

        Map<String, Serializable> complexItem2 = new HashMap<String, Serializable>();
        complexItem2.put("stringItem", "second item");
        List<String> item2SubList = new ArrayList<String>();
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
     * @throws ClientException the client exception
     */
    protected DocumentModel createRightDoc(CoreSession session) throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "rightDoc", "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My second sample");
        doc.setPropertyValue("dc:created", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 50));
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 30, 12, 05, 02));
        doc.setPropertyValue("dc:lastContributor", " Administrator ");
        doc.setPropertyValue("dc:contributors", new String[] { "anotherAdministrator", "joe", "jack" });
        doc.setPropertyValue("dc:subjects", new String[] { "Art" });

        // -----------------------
        // file
        // -----------------------
        doc.setPropertyValue("file:filename", "Jack.txt");
        Blob blob = Blobs.createBlob("Joe is rich, Jack is not.");
        blob.setFilename("Jack.txt");
        doc.setPropertyValue("file:content", (Serializable) blob);

        // -----------------------
        // files
        // -----------------------
        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();

        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("filename", "first_attachement.txt");
        blob = Blobs.createBlob("Content of the first blob");
        blob.setFilename("first_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        file = new HashMap<String, Serializable>();
        file.put("filename", "the_file_name_is_different.txt");
        blob = Blobs.createBlob("Content of the second blob");
        blob.setFilename("the_file_name_is_different.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        file = new HashMap<String, Serializable>();
        file.put("filename", "third_attachement.txt");
        blob = Blobs.createBlob("Different content of the third blob");
        blob.setFilename("third_attachement.txt");
        file.put("file", (Serializable) blob);
        files.add(file);

        doc.setPropertyValue("files:files", (Serializable) files);

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a different string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 28, 23, 00, 00));
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<String, Serializable>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", false);
        complexPropValue.put("dateItem", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 29, 23, 00, 00));
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<String, Serializable>();
        item1ComplexPropValue.put("stringItem", "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", false);
        item1ComplexPropValue.put("dateItem", getCalendarUTCNoMillis(2011, Calendar.DECEMBER, 30, 23, 00, 00));

        Map<String, Serializable> item2ComplexPropValue = new HashMap<String, Serializable>();
        item2ComplexPropValue.put("stringItem", "second element of a complex list");
        item2ComplexPropValue.put("booleanItem", false);
        item2ComplexPropValue.put("integerItem", 20);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<Map<String, Serializable>>();
        complexListPropValue.add(item1ComplexPropValue);
        complexListPropValue.add(item2ComplexPropValue);

        doc.setPropertyValue("ct:complexList", (Serializable) complexListPropValue);

        // -----------------------
        // listoflists
        // -----------------------
        List<Map<String, Serializable>> listOfListPropValue = new ArrayList<Map<String, Serializable>>();

        Map<String, Serializable> complexItem1 = new HashMap<String, Serializable>();
        complexItem1.put("stringItem", "first item");
        List<String> item1SubList = new ArrayList<String>();
        item1SubList.add("Monday");
        item1SubList.add("Tuesday");
        complexItem1.put("stringListItem", (Serializable) item1SubList);
        listOfListPropValue.add(complexItem1);

        Map<String, Serializable> complexItem2 = new HashMap<String, Serializable>();
        complexItem2.put("stringItem", "second item is different");
        List<String> item2SubList = new ArrayList<String>();
        item2SubList.add("Wednesday");
        item2SubList.add("Friday");
        item2SubList.add("Saturday");
        complexItem2.put("stringListItem", (Serializable) item2SubList);
        listOfListPropValue.add(complexItem2);

        Map<String, Serializable> complexItem3 = new HashMap<String, Serializable>();
        complexItem3.put("stringItem", "third item");
        List<String> item3SubList = new ArrayList<String>();
        item2SubList.add("July");
        item2SubList.add("August");
        complexItem3.put("stringListItem", (Serializable) item3SubList);
        listOfListPropValue.add(complexItem3);

        doc.setPropertyValue("lol:listOfLists", (Serializable) listOfListPropValue);

        return session.createDocument(doc);
    }

    /**
     * Gets a calendar set on the UTC time zone with 0 milliseconds.
     *
     * @param year the year
     * @param month the month
     * @param day the day
     * @param hourOfDay the hour of day
     * @param minute the minute
     * @param second the second
     * @return the calendar
     */
    public static Calendar getCalendarUTCNoMillis(int year, int month, int day, int hourOfDay, int minute, int second) {

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hourOfDay, minute, second);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

}
