/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.FetchContextDocument;
import org.nuxeo.ecm.automation.core.operations.document.CreateDocument;
import org.nuxeo.ecm.automation.core.operations.document.UpdateDocument;
import org.nuxeo.ecm.automation.core.scripting.MvelTemplate;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:complexTypeContribs.xml")
public class DocumentUpdateOperationTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    OperationChain chain;

    @Before
    public void initChain() throws Exception {
        chain = new OperationChain("testChain");
        chain.add(FetchContextDocument.ID);

        Map<String, Object> params = new HashMap<>();
        params.put("type", "File");
        params.put("name", "file");
        chain.add(CreateDocument.ID).from(params);

        params = new HashMap<>();
        params.put(
                "properties",
                new MvelTemplate(
                        "dc:title=Test\ndc:issued=@{org.nuxeo.ecm.core.schema.utils.DateParser.formatW3CDateTime(CurrentDate.date)}"));
        params.put("save", "true");
        chain.add(UpdateDocument.ID).from(params);
    }

    @After
    public void clearRepo() throws Exception {
        Framework.getService(EventService.class).waitForAsyncCompletion();
    }

    @Test
    public void shouldUpdateProperties() throws Exception {

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(session.getRootDocument());

            DocumentModel doc = (DocumentModel) service.run(ctx, chain);
            assertNotNull(doc);
            assertEquals("Test", doc.getTitle());
            assertNotNull(doc.getPropertyValue("dc:issued"));
        }
    }

    // NXP-30623
    @Test
    public void shouldAppendToList() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:subjects", (Serializable) List.of("art/architecture"));
        doc = session.createDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc.getRef());
            // Test adding a single property
            var singleParam = Map.of("properties", "dc:subjects=sciences/astronomy", //
                    "propertiesBehaviors", "dc:subjects=append_including_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, singleParam);
            assertEquals(List.of("art/architecture", "sciences/astronomy"),
                    List.of((String[]) doc.getPropertyValue("dc:subjects")));

            // Test excluding a duplicate property
            var dupeParamExclude = Map.of("properties", "dc:subjects=art/architecture", //
                    "propertiesBehaviors", "dc:subjects=append_excluding_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, dupeParamExclude);
            assertEquals(List.of("art/architecture", "sciences/astronomy"),
                    List.of((String[]) doc.getPropertyValue("dc:subjects")));

            // Test including a duplicate property
            var dupeParamInclude = Map.of("properties", "dc:subjects=art/architecture", //
                    "propertiesBehaviors", "dc:subjects=append_including_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, dupeParamInclude);
            assertEquals(List.of("art/architecture", "sciences/astronomy", "art/architecture"),
                    List.of((String[]) doc.getPropertyValue("dc:subjects")));

            // Test adding multiple properties
            var multipleParams = Map.of("properties", "dc:subjects=art/dance,society/minority,daily life/video games", //
                    "propertiesBehaviors", "dc:subjects=append_excluding_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, multipleParams);
            assertEquals(
                    List.of("art/architecture", "sciences/astronomy", "art/architecture", "art/dance",
                            "society/minority", "daily life/video games"),
                    List.of((String[]) doc.getPropertyValue("dc:subjects")));
        }

    }

    // NXP-30623
    @Test
    public void shouldReplaceList() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc.setPropertyValue("dc:subjects", (Serializable) List.of("art/architecture"));
        doc = session.createDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc.getRef());
            // no propertiesBehavior
            // => replace (default behavior)
            var noBehaviorParam = Map.of("properties", "dc:subjects=sciences/astronomy");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, noBehaviorParam);

            assertEquals(List.of("sciences/astronomy"), List.of((String[]) doc.getPropertyValue("dc:subjects")));

            // w/ propertiesBehavior to 'replace'
            // => replace
            var replaceParam = Map.of("properties", "dc:subjects=art/dance,society/minority", //
                    "propertiesBehaviors", "dc:subjects=replace");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, replaceParam);

            assertEquals(List.of("art/dance", "society/minority"),
                    List.of((String[]) doc.getPropertyValue("dc:subjects")));

            // w/ propertiesBehavior to 'unknown'
            // => replace (ignore behavior)
            var unknownParam = Map.of("properties", "dc:subjects=sciences/astronomy", //
                    "propertiesBehaviors", "dc:subjects=unknown");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, unknownParam);

            assertEquals(List.of("sciences/astronomy"), List.of((String[]) doc.getPropertyValue("dc:subjects")));
        }
    }

    // NXP-30623
    @Test
    public void shouldAppendToComplexList() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        doc.addFacet("Addresses");
        Map<String, Object> address = new HashMap<>();
        address.put("streetNumber", "1bis");
        address.put("streetName", "whatever");
        address.put("zipCode", 75020);
        doc.setPropertyValue("addr:addressesList", (Serializable) List.of(address));
        doc = session.saveDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc.getRef());

            // Test adding a single property
            var singleParam = Map.of("properties",
                    "addr:addressesList=[{ \"streetNumber\": \"2\", \"streetName\": \"Baker\",\"zipCode\": 75000 }]",
                    "propertiesBehaviors", "addr:addressesList=append_excluding_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, singleParam);

            List<Map<String, Object>> addresses = (List<Map<String, Object>>) doc.getPropertyValue(
                    "addr:addressesList");
            assertNotNull(addresses);
            assertEquals(2, addresses.size());
            assertEquals("1bis", addresses.get(0).get("streetNumber"));
            assertEquals(75020L, addresses.get(0).get("zipCode"));
            assertEquals("2", addresses.get(1).get("streetNumber"));
            assertEquals(75000L, addresses.get(1).get("zipCode"));

            // Test excluding a duplicate property
            var dupeParam = Map.of("properties",
                    "addr:addressesList=[{ \"streetNumber\": \"2\", \"streetName\": \"Baker\",\"zipCode\": 75000 },{ \"streetNumber\": \"3\", \"streetName\": \"Baker\",\"zipCode\": 75000 }]",
                    "propertiesBehaviors", "addr:addressesList=append_excluding_duplicates");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, dupeParam);

            List<Map<String, Object>> addresses2 = (List<Map<String, Object>>) doc.getPropertyValue(
                    "addr:addressesList");
            assertNotNull(addresses2);
            assertEquals(3, addresses2.size());
            assertEquals("1bis", addresses2.get(0).get("streetNumber"));
            assertEquals(75020L, addresses2.get(0).get("zipCode"));
            assertEquals("2", addresses2.get(1).get("streetNumber"));
            assertEquals(75000L, addresses2.get(1).get("zipCode"));
            assertEquals("3", addresses2.get(2).get("streetNumber"));
        }

    }

    // NXP-30623
    @Test
    public void shouldReplaceComplexList() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
        doc.addFacet("Addresses");
        Map<String, Object> address = new HashMap<>();
        address.put("streetNumber", "1bis");
        address.put("streetName", "whatever");
        address.put("zipCode", 75020);
        doc.setPropertyValue("addr:addressesList", (Serializable) List.of(address));
        doc = session.saveDocument(doc);

        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(doc.getRef());
            var singleParam = Map.of("properties",
                    "addr:addressesList=[{ \"streetNumber\": \"2\", \"streetName\": \"Baker\",\"zipCode\": 75000 }]",
                    "propertiesBehaviors", "addr:addressesList=replace");
            doc = (DocumentModel) service.run(ctx, UpdateDocument.ID, singleParam);

            List<Map<String, Object>> addresses = (List<Map<String, Object>>) doc.getPropertyValue(
                    "addr:addressesList");
            assertNotNull(addresses);
            assertEquals(1, addresses.size());
            assertEquals("2", addresses.get(0).get("streetNumber"));
            assertEquals(75000L, addresses.get(0).get("zipCode"));
        }
    }

    // NXP-30623
    @Test
    public void shouldAppendListWhenNull() throws Exception {
        DocumentModel docNull = session.createDocumentModel("/", "file", "File");
        docNull = session.createDocument(docNull);
        docNull.addFacet("Addresses");
        docNull = session.saveDocument(docNull);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(docNull.getRef());

            // Test adding a simple property to empty list
            var simpleParam = Map.of("properties", "dc:subjects=sciences/astronomy", //
                    "propertiesBehaviors", "dc:subjects=append_excluding_duplicates");
            docNull = (DocumentModel) service.run(ctx, UpdateDocument.ID, simpleParam);
            assertEquals(List.of("sciences/astronomy"), List.of((String[]) docNull.getPropertyValue("dc:subjects")));

            // Test adding a complex property to empty list
            var complexParam = Map.of("properties",
                    "addr:addressesList=[{ \"streetNumber\": \"2\", \"streetName\": \"Baker\",\"zipCode\": 75000 }]",
                    "propertiesBehaviors", "addr:addressesList=append_excluding_duplicates");
            docNull = (DocumentModel) service.run(ctx, UpdateDocument.ID, complexParam);

            List<Map<String, Object>> addresses = (List<Map<String, Object>>) docNull.getPropertyValue(
                    "addr:addressesList");
            assertNotNull(addresses);
            assertEquals(75000L, addresses.get(0).get("zipCode"));

        }

    }
}
