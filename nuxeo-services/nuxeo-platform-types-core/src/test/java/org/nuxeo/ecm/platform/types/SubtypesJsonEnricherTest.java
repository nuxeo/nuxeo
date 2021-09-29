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
 *     Gabriel Barata <gbarata@nuxeo.com>
 *     Charles Boidot <cboidot@nuxeo.com>
 */

package org.nuxeo.ecm.platform.types;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.types.localconfiguration.UITypesConfigurationConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 8.4
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.core.io:OSGI-INF/doc-type-contrib.xml")
public class SubtypesJsonEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    @Inject
    private CoreSession session;

    public SubtypesJsonEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Before
    public void setup() {
        DocumentModel document = session.createDocumentModel("/", "folder_root", "MyFolderRoot");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "doc1", "MyFolder");
        document = session.createDocument(document);
        document = session.createDocumentModel("/", "doc2", "MyFolder2");
        document = session.createDocument(document);
    }

    /**
     * @since 11.5
     */
    @Test
    public void testSubtypes() throws Exception {
        DocumentModel folderRoot = session.getDocument(new PathRef("/folder_root"));
        JsonAssert subTypes = assertSubtypes(folderRoot, 2, "MyFolder", "MyFolder2");
        JsonAssert sub = subTypes.has(0);
        sub = sub.has("facets");
        sub.length(1);
        sub.contains("Folderish");
        sub = subTypes.has(1);
        sub = sub.has("facets");
        sub.length(2);
        sub.contains("Folderish", "HiddenInCreation");

        DocumentModel doc1 = session.getDocument(new PathRef("/doc1"));
        assertSubtypes(doc1, 3, "RefDoc", "File", "CSDoc");

        // doctype of doc2 extends doctype of doc1, but subtypes should not be inherited
        DocumentModel doc2 = session.getDocument(new PathRef("/doc2"));
        assertSubtypes(doc2, 1, "DummyDoc");
    }

    @Test
    public void testLocalConfigurationAllowedSubtypes() throws Exception {
        DocumentModel folderRoot = session.getDocument(new PathRef("/folder_root"));
        folderRoot.addFacet(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET);
        folderRoot.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY,
                (Serializable) Arrays.asList("MyFolder"));
        folderRoot = session.saveDocument(folderRoot);

        assertSubtypes(folderRoot, 1, "MyFolder");
    }

    @Test
    public void testLocalConfigurationDeniedSubtypes() throws Exception {
        DocumentModel folderRoot = session.getDocument(new PathRef("/folder_root"));
        folderRoot.addFacet(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET);
        folderRoot.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DENIED_TYPES_PROPERTY,
                (Serializable) Arrays.asList("MyFolder"));
        folderRoot = session.saveDocument(folderRoot);

        assertSubtypes(folderRoot, 1, "MyFolder2");
    }

    @Test
    public void testLocalConfigurationDenyAllSubtypes() throws Exception {
        DocumentModel folderRoot = session.getDocument(new PathRef("/folder_root"));
        folderRoot.addFacet(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET);
        folderRoot.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DENY_ALL_TYPES_PROPERTY,
                Boolean.TRUE);
        folderRoot = session.saveDocument(folderRoot);

        assertSubtypes(folderRoot, 0);
    }

    @Test
    public void testLocalConfigurationSubtypes() throws Exception {
        DocumentModel folderRoot = session.getDocument(new PathRef("/folder_root"));
        folderRoot.addFacet(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET);
        folderRoot.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY,
                (Serializable) Arrays.asList("MyFolder"));
        folderRoot.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_DENIED_TYPES_PROPERTY,
                (Serializable) Arrays.asList("MyFolder2"));
        folderRoot = session.saveDocument(folderRoot);

        assertSubtypes(folderRoot, 1, "MyFolder");
    }

    // NXP-30128
    @Test
    public void testLocalConfigurationSubtypesInheritance() throws IOException {
        DocumentModel ws = session.createDocumentModel("/", "ws", "Workspace");
        ws = session.createDocument(ws);
        DocumentModel subWS = session.createDocumentModel("/ws", "subws", "Workspace");
        subWS = session.createDocument(subWS);

        // more than one subtypes
        assertSubtypes(subWS, -1);

        ws.refresh();
        ws.addFacet(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_FACET);
        ws.setPropertyValue(UITypesConfigurationConstants.UI_TYPES_CONFIGURATION_ALLOWED_TYPES_PROPERTY,
                (Serializable) Arrays.asList("Workspace"));
        session.saveDocument(ws);

        // only one subtype
        assertSubtypes(subWS, 1, "Workspace");
    }

    /**
     * Checks the document subtypes.
     * <p>
     * If {@code length == -1}, the method only checks that there are more than 1 subtypes.
     *
     * @param length the subtypes array length
     * @param subtypes the subtypes to check, not checked if {@code length} is < 1
     * @return the subtypes JsonAssert object
     */
    protected JsonAssert assertSubtypes(DocumentModel doc, int length, String... subtypes) throws IOException {
        RenderingContext ctx = RenderingContext.CtxBuilder.enrichDoc("subtypes").get();
        JsonAssert json = jsonAssert(doc, ctx);
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json = json.has("subtypes").isArray();
        if (length == -1) {
            // just check that there are more than 1 subtypes
            assertTrue(json.getNode().size() > 1);
        } else {
            json = json.length(length);
            if (length > 0) {
                json.childrenContains("type", subtypes);
            }
        }
        return json;
    }
}
