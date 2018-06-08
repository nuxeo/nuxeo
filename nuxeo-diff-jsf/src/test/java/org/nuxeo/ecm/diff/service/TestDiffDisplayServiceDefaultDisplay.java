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
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
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
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.io:OSGI-INF/document-xml-exporter-service.xml", //
        "org.nuxeo.diff.core", //
        "org.nuxeo.diff.test", //
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
        leftDoc.setPropertyValue("common:size", 10);
        // Set other properties
        leftDoc.setPropertyValue("dc:description", "Description of my sample type.");
        leftDoc.setPropertyValue("dc:subjects", new String[] { "Art", "Architecture" });
        leftDoc.setPropertyValue("dc:creator", "Joe");
        List<Map<String, Serializable>> files = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) Blobs.createBlob("Joe is not rich."));
        file.put("filename", "Joe.txt");
        files.add(file);
        leftDoc.setPropertyValue("files:files", (Serializable) files);
        leftDoc = session.createDocument(leftDoc);
        // Set complex properties to test the diffComplexField contribs
        Map<String, Serializable> complexProp = new HashMap<String, Serializable>();
        complexProp.put("stringItem", "My name is Joe");
        complexProp.put("booleanItem", true);
        complexProp.put("integerItem", 3);
        complexProp.put("dateItem", new GregorianCalendar(2012, Calendar.DECEMBER, 25));
        leftDoc.setPropertyValue("ct:complex", (Serializable) complexProp);
        List<Map<String, Serializable>> complexListProp = new ArrayList<Map<String, Serializable>>();
        complexListProp.add(complexProp);
        leftDoc.setPropertyValue("ct:complexList", (Serializable) complexListProp);

        DocumentModel rightDoc = session.createDocumentModel("/", "MyOtherSampleType", "OtherSampleType");
        rightDoc.setPropertyValue("dc:description", "Description of my other sample type.");
        rightDoc.setPropertyValue("dc:subjects", new String[] { "Art" });
        rightDoc.setPropertyValue("dc:creator", "Jack");
        files = new ArrayList<Map<String, Serializable>>();
        file = new HashMap<String, Serializable>();
        file.put("file", (Serializable) Blobs.createBlob("Joe is not rich, nor is Jack."));
        file.put("filename", "Jack.pdf");
        files.add(file);
        rightDoc.setPropertyValue("files:files", (Serializable) files);
        rightDoc = session.createDocument(rightDoc);
        // Set complex properties to test the diffComplexField contribs
        complexProp = new HashMap<String, Serializable>();
        complexProp.put("stringItem", "My name is Jack");
        complexProp.put("booleanItem", false);
        complexProp.put("integerItem", 50);
        complexProp.put("dateItem", new GregorianCalendar(2011, Calendar.NOVEMBER, 23));
        rightDoc.setPropertyValue("ct:complex", (Serializable) complexProp);
        complexListProp = new ArrayList<Map<String, Serializable>>();
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
                checkDiffDisplayBlockSchema(diffDisplayBlock, "files", 1, Arrays.asList("files"));
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
        List<String> remainingFieldItemNames = new ArrayList<String>(Arrays.asList(fieldItemNames));
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
}
