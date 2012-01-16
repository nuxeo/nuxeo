/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.diff.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;

/**
 * Inits the repository for a document diff test case.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentDiffRepositoryInit extends DefaultRepositoryInit {

    public static final String LEFT_DOC_PATH = "/leftDoc";

    public static final String RIGHT_DOC_PATH = "/rightDoc";

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
    protected final DocumentModel createLeftDoc(CoreSession session)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc",
                "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:created", "2011-12-29T11:24:25Z");
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", "2011-12-29T11:24:25Z");
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "Administrator",
                "joe", null });
        doc.setPropertyValue("dc:subjects", new String[] { "Art",
                "Architecture" });

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:boolean", true);
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", "2011-12-28T23:00:00Z");
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        doc.setPropertyValue("st:multivalued", new String[] { "monday",
                "tuesday", "wednesday", "thursday" });

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<String, Serializable>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", true);
        complexPropValue.put("integerItem", 10);
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<String, Serializable>();
        item1ComplexPropValue.put("stringItem",
                "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", true);
        item1ComplexPropValue.put("integerItem", 12);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<Map<String, Serializable>>();
        complexListPropValue.add(item1ComplexPropValue);

        doc.setPropertyValue("ct:complexList",
                (Serializable) complexListPropValue);

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
        // TODO: uncomment
        // listOfListPropValue.add(complexItem1);

        Map<String, Serializable> complexItem2 = new HashMap<String, Serializable>();
        complexItem2.put("stringItem", "second item");
        List<String> item2SubList = new ArrayList<String>();
        item2SubList.add("Wednesday");
        item2SubList.add("Thursday");
        complexItem2.put("stringListItem", (Serializable) item2SubList);
        listOfListPropValue.add(complexItem2);

        doc.setPropertyValue("lol:listOfLists",
                (Serializable) listOfListPropValue);

        return session.createDocument(doc);
    }

    /**
     * Creates the right doc.
     * 
     * @param session the session
     * @return the document model
     * @throws ClientException the client exception
     */
    protected final DocumentModel createRightDoc(CoreSession session)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "rightDoc",
                "OtherSampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My second sample");
        doc.setPropertyValue("dc:created", "2011-12-30T12:05:02Z");
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", "2011-12-30T12:05:02Z");
        doc.setPropertyValue("dc:lastContributor", " Administrator ");
        doc.setPropertyValue("dc:contributors", new String[] {
                "anotherAdministrator", "joe", "jack" });
        doc.setPropertyValue("dc:subjects", new String[] { "Art" });

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a different string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", "2011-12-28T23:00:00Z");
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");

        // -----------------------
        // complextypes
        // -----------------------
        Map<String, Serializable> complexPropValue = new HashMap<String, Serializable>();
        complexPropValue.put("stringItem", "string of a complex type");
        complexPropValue.put("booleanItem", false);
        complexPropValue.put("dateItem", "2011-12-29T23:00:00Z");
        doc.setPropertyValue("ct:complex", (Serializable) complexPropValue);

        Map<String, Serializable> item1ComplexPropValue = new HashMap<String, Serializable>();
        item1ComplexPropValue.put("stringItem",
                "first element of a complex list");
        item1ComplexPropValue.put("booleanItem", false);
        item1ComplexPropValue.put("dateItem", "2011-12-30T23:00:00Z");

        Map<String, Serializable> item2ComplexPropValue = new HashMap<String, Serializable>();
        item2ComplexPropValue.put("stringItem",
                "second element of a complex list");
        item2ComplexPropValue.put("booleanItem", false);
        item2ComplexPropValue.put("integerItem", 20);

        List<Map<String, Serializable>> complexListPropValue = new ArrayList<Map<String, Serializable>>();
        complexListPropValue.add(item1ComplexPropValue);
        complexListPropValue.add(item2ComplexPropValue);

        doc.setPropertyValue("ct:complexList",
                (Serializable) complexListPropValue);

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

        doc.setPropertyValue("lol:listOfLists",
                (Serializable) listOfListPropValue);

        return session.createDocument(doc);
    }

}
