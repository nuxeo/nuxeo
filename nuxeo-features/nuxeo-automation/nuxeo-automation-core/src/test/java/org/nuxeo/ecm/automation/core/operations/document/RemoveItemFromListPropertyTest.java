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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.automation.core")
@LocalDeploy("org.nuxeo.ecm.automation.core:OSGI-INF/dataset-type-test-contrib.xml")
public class RemoveItemFromListPropertyTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession coreSession;

    protected DocumentModel testFolder;

    protected DocumentModel doc;

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
        assertNotNull(doc.getPropertyValue("ds:fields"));
        assertEquals(((Collection) doc.getPropertyValue("ds:fields")).size(), 0);

        // Get new fields from json file to String
        File fieldsAsJsonFile = FileUtils.getResourceFileFromContext("newFields.json");
        assertNotNull(fieldsAsJsonFile);
        String fieldsDataAsJSon = org.apache.commons.io.FileUtils.readFileToString(fieldsAsJsonFile, UTF_8);
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\n", "");
        fieldsDataAsJSon = fieldsDataAsJSon.replaceAll("\r", "");

        // ADD new fields
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(doc);
        OperationChain chain = new OperationChain("testChain");
        chain.add(AddItemToListProperty.ID).set("xpath", "ds:fields").set("complexJsonProperties", fieldsDataAsJSon);

        doc = (DocumentModel) service.run(ctx, chain);
        List dbFields = (List) doc.getPropertyValue("ds:fields");
        assertEquals(2, dbFields.size());
    }

    @After
    public void clearRepo() {
        coreSession.removeChildren(coreSession.getRootDocument().getRef());
        coreSession.save();
    }

    @Test
    public void removeAllItemFromListPropertyTest() throws Exception {
        // Remove all the fields
        DocumentModel resultDoc = removeItemsFromListProperty(null);
        List dbFields = (List) resultDoc.getPropertyValue("ds:fields");
        assertEquals(0, dbFields.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeFirstItemFromListPropertyTest() throws Exception {
        //remove the first item
        DocumentModel resultDoc = removeItemsFromListProperty(0);

        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals(properties.get("fieldType"), "unicTypeAdded2");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void removeLastItemFromListPropertyTest() throws Exception {
        // Remove the last item
        DocumentModel resultDoc = removeItemsFromListProperty(1);
        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals(properties.get("fieldType"), "unicTypeAdded");
    }

    @Test(expected = OperationException.class)
    @SuppressWarnings("unchecked")
    public void removeNonExistentItemFromListPropertyTest() throws Exception {
        // Not possible to remove the index:2, because the list only have two items
        DocumentModel resultDoc = removeItemsFromListProperty(2);
        List<Map<String, String>> dbFields = (List<Map<String, String>>) resultDoc.getPropertyValue("ds:fields");
        assertEquals(1, dbFields.size());

        Map<String, String> properties = dbFields.get(0);
        assertEquals(properties.get("fieldType"), "unicTypeAdded");
    }

    protected DocumentModel removeItemsFromListProperty(Integer index) throws OperationException {
        // Remove all the fields
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(doc);
        OperationChain chain = new OperationChain("testRemoveItemFromPropertyChain");

        String xpath = "ds:fields";
        OperationParameters parameters = chain.add(RemoveItemFromListProperty.ID).set("xpath", xpath);

        if (index != null) parameters.set("index", index);

        return (DocumentModel) service.run(ctx, chain);
    }

}
