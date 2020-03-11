/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:OSGI-INF/dataset-type-test-contrib.xml")
public class RemoveItemFromListPropertyTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession coreSession;

    protected DocumentModel testFolder;

    protected DocumentModel doc;

    protected OperationContext ctx;

    @Before
    public void initRepo() throws Exception {
        coreSession.removeChildren(coreSession.getRootDocument().getRef());
        coreSession.save();

        testFolder = coreSession.createDocumentModel("/", "Folder", "Folder");
        testFolder.setPropertyValue("dc:title", "Folder");
        testFolder = coreSession.createDocument(testFolder);
        coreSession.save();
        testFolder = coreSession.getDocument(testFolder.getRef());

        // creates a document of custom type DataSet
        doc = coreSession.createDocumentModel("/Folder", "TestDoc", "DataSet");
        doc.setPropertyValue("dc:title", "TestDoc");
        doc = coreSession.createDocument(doc);
        coreSession.save();
        doc = coreSession.getDocument(doc.getRef());

        // Check there is no value already.
        List<?> fields = (List<?>) doc.getPropertyValue("ds:fields");
        assertNotNull(fields);
        assertTrue(fields.isEmpty());

        // Get new fields from json file to String
        File fieldsAsJsonFile = FileUtils.getResourceFileFromContext("newFields.json");
        assertNotNull(fieldsAsJsonFile);
        String fieldsDataAsJSon = org.apache.commons.io.FileUtils.readFileToString(fieldsAsJsonFile, UTF_8);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");

        // ADD new fields
        try (OperationContext ctx = new OperationContext(coreSession)) {
            ctx.setInput(doc);
            OperationChain chain = new OperationChain("testChain");
            chain.add(AddItemToListProperty.ID).set("xpath", "ds:fields").set("complexJsonProperties", fieldsDataAsJSon);

            doc = (DocumentModel) service.run(ctx, chain);
            List<?> dbFields = (List<?>) doc.getPropertyValue("ds:fields");
            assertEquals(2, dbFields.size());
        }

        ctx = new OperationContext(coreSession);
    }

    @After
    public void clearRepo() {
        ctx.close();
        coreSession.removeChildren(coreSession.getRootDocument().getRef());
        coreSession.save();
    }

    @Test
    public void removeAllItemFromListPropertyTest() throws Exception {
        // Remove all the fields
        DocumentModel resultDoc = removeItemsFromListProperty("ds:fields", null);
        List<?> dbFields = (List<?>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(0, dbFields.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeFirstItemFromListPropertyTest() throws Exception {
        // remove the first item
        DocumentModel resultDoc = removeItemsFromListProperty("ds:fields", 0);

        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals("unicTypeAdded2", properties.get("fieldType"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeLastItemFromListPropertyTest() throws Exception {
        // Remove the last item
        DocumentModel resultDoc = removeItemsFromListProperty("ds:fields", 1);
        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals("unicTypeAdded", properties.get("fieldType"));
    }

    @Test(expected = OperationException.class)
    @SuppressWarnings("unchecked")
    public void removeNonExistentItemFromListPropertyTest() throws Exception {
        // Not possible to remove the index:2, because the list only have two items
        DocumentModel resultDoc = removeItemsFromListProperty("ds:fields", 2);
        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals("unicTypeAdded", properties.get("fieldType"));
    }

    protected DocumentModel removeItemsFromListProperty(String xpath, Integer index) throws OperationException {
        ctx.setInput(doc);

        Map<String, Object> params = new HashMap<>();
        params.put("xpath", xpath);
        if (index != null) {
            params.put("index", index);
        }
        return (DocumentModel) service.run(ctx, RemoveItemFromListProperty.ID, params);
    }

    @Test
    public void removeFromArrayProperty() throws OperationException {
        doc.setPropertyValue("dc:subjects", new String[] { "sub1", "sub2", "sub3", "sub4" });
        doc = coreSession.saveDocument(doc);
        String[] subjects = (String[]) doc.getPropertyValue("dc:subjects");
        assertNotNull(subjects);
        assertEquals(4, subjects.length);

        // remove first item
        DocumentModel res = removeItemsFromListProperty("dc:subjects", 0);
        subjects = (String[]) res.getPropertyValue("dc:subjects");
        assertNotNull(subjects);
        assertArrayEquals(new String[] { "sub2", "sub3", "sub4" }, subjects);

        // remove second item
        res = removeItemsFromListProperty("dc:subjects", 1);
        subjects = (String[]) res.getPropertyValue("dc:subjects");
        assertNotNull(subjects);
        assertArrayEquals(new String[] { "sub2", "sub4" }, subjects);

        // clear remaining items
        res = removeItemsFromListProperty("dc:subjects", null);
        subjects = (String[]) res.getPropertyValue("dc:subjects");
        assertNull(subjects);
    }

}
