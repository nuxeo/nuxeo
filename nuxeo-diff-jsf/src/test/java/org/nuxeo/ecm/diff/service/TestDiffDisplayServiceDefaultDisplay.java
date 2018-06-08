/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link DiffDisplayService} for the default diff display.
 */
@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.diff.core", //
        "org.nuxeo.diff.test", //
        "org.nuxeo.diff.test:OSGI-INF/test-diff-constraint-types-contrib.xml", //
        "org.nuxeo.ecm.platform.forms.layout.client", //
        "org.nuxeo.ecm.platform.forms.layout.core:OSGI-INF/layouts-core-framework.xml", //
        "org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml", //
        "org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml", //
        "org.nuxeo.diff.jsf:OSGI-INF/diff-widgets-contrib.xml", //
        "org.nuxeo.diff.jsf.test:OSGI-INF/test-diff-display-contrib.xml" //
})
public class TestDiffDisplayServiceDefaultDisplay extends DiffDisplayServiceTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests the default diff display on 2 documents of a different type.
     */
    @Test
    public void testDefaultDiffDisplay() {

        // Create left and right docs
        DocumentModel leftDoc = session.createDocumentModel("/", "MySampleType", "SampleType");
        // Set properties from the "common" schema. They should be ignored in
        // the diff display since this schema is excluded.
        leftDoc.setPropertyValue("common:icon", "icons/note.gif");
        // Set other properties
        leftDoc.setPropertyValue("dc:description", "Description of my sample type.");
        leftDoc.setPropertyValue("dc:subjects", new String[] { "Art", "Architecture" });
        leftDoc.setPropertyValue("dc:creator", "Joe");
        List<Map<String, Serializable>> files = new ArrayList<>();
        Map<String, Serializable> file = new HashMap<>();
        file.put("file", (Serializable) Blobs.createBlob("Joe is not rich.", "text/plain", "UTF-8", "Joe.txt"));
        files.add(file);
        leftDoc.setPropertyValue("files:files", (Serializable) files);
        leftDoc = session.createDocument(leftDoc);
        // Set complex properties to test the diffComplexField contribs
        Map<String, Serializable> complexProp = new HashMap<>();
        complexProp.put("stringItem", "My name is Joe");
        complexProp.put("booleanItem", true);
        complexProp.put("integerItem", 3);
        complexProp.put("dateItem", new GregorianCalendar(2012, Calendar.DECEMBER, 25));
        leftDoc.setPropertyValue("ct:complex", (Serializable) complexProp);
        List<Map<String, Serializable>> complexListProp = new ArrayList<>();
        complexListProp.add(complexProp);
        leftDoc.setPropertyValue("ct:complexList", (Serializable) complexListProp);

        DocumentModel rightDoc = session.createDocumentModel("/", "MyOtherSampleType", "OtherSampleType");
        rightDoc.setPropertyValue("dc:description", "Description of my other sample type.");
        rightDoc.setPropertyValue("dc:subjects", new String[] { "Art" });
        rightDoc.setPropertyValue("dc:creator", "Jack");
        files = new ArrayList<>();
        file = new HashMap<>();
        file.put("file",
                (Serializable) Blobs.createBlob("Joe is not rich, nor is Jack.", "text/plain", "UTF-8", "Jack.pdf"));
        files.add(file);
        rightDoc.setPropertyValue("files:files", (Serializable) files);
        rightDoc = session.createDocument(rightDoc);
        // Set complex properties to test the diffComplexField contribs
        complexProp = new HashMap<>();
        complexProp.put("stringItem", "My name is Jack");
        complexProp.put("booleanItem", false);
        complexProp.put("integerItem", 50);
        complexProp.put("dateItem", new GregorianCalendar(2011, Calendar.NOVEMBER, 23));
        rightDoc.setPropertyValue("ct:complex", (Serializable) complexProp);
        complexListProp = new ArrayList<>();
        complexListProp.add(complexProp);
        rightDoc.setPropertyValue("ct:complexList", (Serializable) complexListProp);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(3, diffDisplayBlocks.size());

        // Check diff display blocks
        for (DiffDisplayBlock diffDisplayBlock : diffDisplayBlocks) {

            if (checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.dublincore", 1)) {
                checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 2, Arrays.asList("description", "creator"));
            } else if (checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.files", 1)) {
                checkDiffDisplayBlockSchema(diffDisplayBlock, "files", 1, Collections.singletonList("files"));
            } else if (checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.complextypes", 1)) {
                checkDiffDisplayBlockSchema(diffDisplayBlock, "complextypes", 2,
                        Arrays.asList("complex", "complexList"));
                checkDiffDisplayBlockFieldWidgets(diffDisplayBlock, "complextypes:complex",
                        new String[] { "dateItem", "stringItem" }, true);
                checkDiffDisplayBlockFieldWidgets(diffDisplayBlock, "complextypes:complexList",
                        new String[] { "index", "integerItem", "booleanItem" }, false);
            } else {
                fail("Unmatching diff display block.");
            }
        }
    }

    /**
     * @since 10.2
     */
    @Test
    public void testConstraints() {
        // Create left doc and set constrained properties
        DocumentModel leftDoc = session.createDocumentModel("/", "leftSampleType", "SampleType");
        leftDoc = session.createDocument(leftDoc);
        leftDoc.setPropertyValue("constraints:string", "foo");
        leftDoc.setPropertyValue("constraints:multivaluedString", new String[] { "foo", "joe" });
        leftDoc.setPropertyValue("constraints:multivaluedDirectory", new String[] { "article", "acknowledgement" });

        // Create right doc and set constrained properties
        DocumentModel rightDoc = session.createDocumentModel("/", "rightSampleType", "OtherSampleType");
        rightDoc = session.createDocument(rightDoc);
        rightDoc.setPropertyValue("constraints:string", "bar");
        rightDoc.setPropertyValue("constraints:multivaluedString", new String[] { "bar", "jack" });
        rightDoc.setPropertyValue("constraints:multivaluedDirectory", new String[] { "certificate" });

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);

        // Check diff display blocks
        assertEquals(1, diffDisplayBlocks.size());
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.constraints", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "constraints", 3,
                Arrays.asList("string", "multivaluedString", "multivaluedDirectory"));

        LayoutDefinition layoutDefinition = diffDisplayBlock.getLayoutDefinition();
        checkWidgetType(layoutDefinition, "constraints:string", "text");
        checkSubWidgetType(layoutDefinition, "constraints:multivaluedString", 1, "text");
        checkSubWidgetType(layoutDefinition, "constraints:multivaluedDirectory", 1, "text");
    }

    @Override
    protected boolean checkDiffDisplayBlock(DiffDisplayBlock diffDisplayBlock, String label, int schemaCount) {

        // Check label
        if (!label.equals(diffDisplayBlock.getLabel())) {
            return false;
        }

        // Check schema count on left value
        Map<String, Map<String, PropertyDiffDisplay>> value = diffDisplayBlock.getLeftValue();
        if (value == null || schemaCount != value.size()) {
            return false;
        }

        // Check schema count on right value
        value = diffDisplayBlock.getRightValue();
        if (value == null || schemaCount != value.size()) {
            return false;
        }

        // TODO: manage contentDiff

        return true;
    }

    @Override
    protected void checkDiffDisplayBlockSchema(DiffDisplayBlock diffDisplayBlock, String schemaName, int fieldCount,
            List<String> fieldNames) {

        // Check fields on left value
        checkDiffDisplayBlockFields(diffDisplayBlock, true, schemaName, fieldCount, fieldNames);

        // Check fields on right value
        checkDiffDisplayBlockFields(diffDisplayBlock, false, schemaName, fieldCount, fieldNames);
    }

    protected void checkDiffDisplayBlockFields(DiffDisplayBlock diffDisplayBlock, boolean isLeftValue,
            String schemaName, int fieldCount, List<String> fieldNames) {

        Map<String, Map<String, PropertyDiffDisplay>> diffDisplayBlockValue = diffDisplayBlock.getLeftValue();
        if (!isLeftValue) {
            diffDisplayBlockValue = diffDisplayBlock.getRightValue();
        }
        Map<String, PropertyDiffDisplay> fields = diffDisplayBlockValue.get(schemaName);
        assertNotNull(fields);
        assertEquals(fieldCount, fields.size());

        for (String fieldName : fieldNames) {
            assertTrue(fields.containsKey(fieldName));
        }
        // TODO: manage contentDiff
    }

    protected void checkDiffDisplayBlockFieldWidgets(DiffDisplayBlock diffDisplayBlock, String widgetName,
            String[] fieldItemNames, boolean order) {

        LayoutDefinition layoutDef = diffDisplayBlock.getLayoutDefinition();
        assertNotNull(layoutDef);
        WidgetDefinition widgetDef = layoutDef.getWidgetDefinition(widgetName);
        assertNotNull(widgetDef);
        WidgetDefinition[] subWidgetDefs = widgetDef.getSubWidgetDefinitions();
        assertNotNull(subWidgetDefs);
        assertEquals(fieldItemNames.length, subWidgetDefs.length);
        List<String> remainingFieldItemNames = new ArrayList<>(Arrays.asList(fieldItemNames));
        for (int i = 0; i < subWidgetDefs.length; i++) {
            WidgetDefinition subWidgetDef = subWidgetDefs[i];
            FieldDefinition[] fieldDefs = subWidgetDef.getFieldDefinitions();
            assertNotNull(fieldDefs);
            if ("index".equals(fieldItemNames[i])) {
                assertEquals(1, fieldDefs.length);
            } else {
                assertEquals(2, fieldDefs.length);
            }

            FieldDefinition fieldDef = fieldDefs[0];
            String fieldDefName = fieldDef.getFieldName();
            if (order) {
                assertTrue(fieldDefName.startsWith(fieldItemNames[i]));
            } else {
                boolean isFieldDef = false;
                Iterator<String> remainingFieldItemNamesIt = remainingFieldItemNames.iterator();
                while (remainingFieldItemNamesIt.hasNext()) {
                    String fieldItemName = remainingFieldItemNamesIt.next();
                    if (fieldDefName.startsWith(fieldItemName)) {
                        isFieldDef = true;
                        remainingFieldItemNamesIt.remove();
                        break;
                    }
                }
                if (!isFieldDef) {
                    fail(String.format("Item %s doesn't match any of the expected field items %s", fieldDefName,
                            fieldItemNames));
                }
            }
        }
    }

    protected void checkWidgetType(LayoutDefinition layoutDefinition, String xpath, String type) {
        assertEquals(type, getWidgetTypeProperty(layoutDefinition.getWidgetDefinition(xpath)));
    }

    protected void checkSubWidgetType(LayoutDefinition layoutDefinition, String xpath, int index, String type) {
        assertEquals(type,
                getWidgetTypeProperty(layoutDefinition.getWidgetDefinition(xpath).getSubWidgetDefinitions()[index]));
    }

    protected String getWidgetTypeProperty(WidgetDefinition widgetDefinition) {
        return (String) widgetDefinition.getProperties().get("any").get("widgetType");
    }
}
