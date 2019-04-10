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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.DocumentDiffRepositoryInit;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.diff.model.impl.PropertyDiffDisplayImpl;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the {@link DiffDisplayService}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", init = DocumentDiffRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.ecm.platform.forms.layout.core:OSGI-INF/layouts-core-framework.xml",
        "org.nuxeo.diff", "org.nuxeo.diff.test" })
public class TestDiffDisplayService extends DiffDisplayServiceTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests diff display block schemas.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testDiffDisplayBlockSchemas() throws ClientException,
            ParseException {

        // --------------------------------------------------------------------
        // Check diff display for 2 documents of a different type: Note / File
        // => must fall back on the default diff display.
        // --------------------------------------------------------------------

        // Create left and right docs
        DocumentModel leftDoc = session.createDocumentModel("Note");
        leftDoc.setPropertyValue("dc:title", "My note");
        leftDoc.setPropertyValue("dc:creator", "Joe");
        leftDoc.setPropertyValue("note:note", "The content of my note.");
        leftDoc = session.createDocument(leftDoc);

        DocumentModel rightDoc = session.createDocumentModel("File");
        rightDoc.setPropertyValue("dc:title", "My file");
        rightDoc.setPropertyValue("dc:creator", "Jack");
        rightDoc.setPropertyValue("file:content", new StringBlob(
                "Joe is not rich, nor is Jack."));
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(1, diffDisplayBlocks.size());

        // Check diff display blocks
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 2,
                Arrays.asList("title", "creator"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type but with no
        // diffDisplay contrib defined for this type: OtherSampleType
        // => must fall back on the default diff display.
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("OtherSampleType");
        leftDoc.setPropertyValue("dc:title", "My first other sample type");
        leftDoc.setPropertyValue("st:boolean", true);
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("OtherSampleType");
        rightDoc.setPropertyValue("dc:title", "My second other sample type");
        rightDoc.setPropertyValue("st:boolean", false);
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff,
                leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 1,
                Arrays.asList("title"));

        diffDisplayBlock = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.simpletypes",
                1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "simpletypes", 1,
                Arrays.asList("boolean"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type with a
        // diffDisplay contrib defined for this type: Note
        // => must use it!
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("Note");
        leftDoc.setPropertyValue("dc:title", "My first note");
        leftDoc.setPropertyValue("dc:creator", "Joe");
        leftDoc.setPropertyValue("note:note",
                "The content of my first note is short.");
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("Note");
        rightDoc.setPropertyValue("dc:title", "My second note");
        rightDoc.setPropertyValue("dc:creator", "Jack");
        rightDoc.setPropertyValue("note:note",
                "The content of my second note written by Jack is still short.");
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff,
                leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(3, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.heading", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 1,
                Arrays.asList("title"));

        diffDisplayBlock = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 1,
                Arrays.asList("creator"));

        diffDisplayBlock = diffDisplayBlocks.get(2);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.note", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "note", 2,
                Arrays.asList("note"));
    }

    /**
     * Tests diff display blocks.
     *
     * @throws ClientException the client exception
     */
    @Test
    public void testDiffDisplayBlocks() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.getLeftDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.getRightDocPath()));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(5, diffDisplayBlocks.size());

        // ----------------------------------------
        // Check first diff display block: heading
        // ----------------------------------------
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);

        // Check label
        assertEquals("label.diffBlock.heading", diffDisplayBlock.getLabel());

        // Check left value
        Map<String, Map<String, PropertyDiffDisplay>> expectedValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, PropertyDiffDisplay> expectedFields = new HashMap<String, PropertyDiffDisplay>();
        expectedFields.put("title", new PropertyDiffDisplayImpl(
                "My first sample", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        expectedFields.put("description", new PropertyDiffDisplayImpl(
                "description", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        // Calendar cal =
        // DocumentDiffRepositoryInit.getCalendarUTCNoMillis(2011,
        // Calendar.DECEMBER, 29, 11, 24, 25);
        // expectedFields.put("modified", cal.getTime());
        // List<Serializable> listField = new ArrayList<Serializable>();
        // Map<String, Serializable> item1 = new HashMap<String,
        // Serializable>();
        // item1.put("index", 1);
        // item1.put("value", "Architecture");
        // listField.add((Serializable) item1);
        // // TODO: test list / complex field
        // expectedFields.put("subjects", (Serializable) listField);
        expectedValue.put("dublincore", expectedFields);
        assertEquals(expectedValue, diffDisplayBlock.getLeftValue());

        // Check right value
        expectedValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        expectedFields = new HashMap<String, PropertyDiffDisplay>();
        expectedFields.put("title", new PropertyDiffDisplayImpl(
                "My second sample", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        expectedFields.put("description", new PropertyDiffDisplayImpl(null,
                PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        // cal = DocumentDiffRepositoryInit.getCalendarUTCNoMillis(2011,
        // Calendar.DECEMBER, 30, 12, 05, 02);
        // expectedFields.put("modified", cal.getTime());
        // listField = new ArrayList<Serializable>();
        // item1 = new HashMap<String, Serializable>();
        // item1.put("index", 1);
        // item1.put("value", "N/A");
        // listField.add((Serializable) item1);
        // // TODO: test list / complex field
        // expectedFields.put("subjects", (Serializable) listField);
        expectedValue.put("dublincore", expectedFields);
        assertEquals(expectedValue, diffDisplayBlock.getRightValue());

        // TODO: check contentDiff

        // Check layout definition
        LayoutDefinition layoutDef = diffDisplayBlock.getLayoutDefinition();
        assertEquals("heading", layoutDef.getName());

        // Check layout row definitions
        LayoutRowDefinition[] layoutRowDefinitions = layoutDef.getRows();
        assertEquals(2, layoutRowDefinitions.length);

        LayoutRowDefinition layoutRowDef = layoutRowDefinitions[0];
        assertEquals("dublincore:title", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        WidgetReference[] widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        WidgetReference widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:title", widgetRef.getName());

        layoutRowDef = layoutRowDefinitions[1];
        assertEquals("dublincore:description", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:description", widgetRef.getName());

        // layoutRowDef = layoutRowDefinitions[1];
        // assertEquals("dublincore:modified", layoutRowDef.getName());
        // assertEquals(1, layoutRowDef.getSize());
        // widgetRefs = layoutRowDef.getWidgetReferences();
        // assertEquals(1, widgetRefs.length);
        // widgetRef = widgetRefs[0];
        // assertEquals("diff", widgetRef.getCategory());
        // assertEquals("dublincore:modified", widgetRef.getName());

        // layoutRowDef = layoutRowDefinitions[2];
        // assertEquals("dublincore:subjects", layoutRowDef.getName());
        // assertEquals(1, layoutRowDef.getSize());
        // widgetRefs = layoutRowDef.getWidgetReferences();
        // assertEquals(1, widgetRefs.length);
        // widgetRef = widgetRefs[0];
        // assertEquals("diff", widgetRef.getCategory());
        // assertEquals("dublincore:subjects", widgetRef.getName());

        // Check layout widget definitions
        WidgetDefinition wDef = layoutDef.getWidgetDefinition("dublincore:title");
        assertNotNull(wDef);
        assertEquals("dublincore:title", wDef.getName());
        assertEquals("template", wDef.getType());
        assertEquals("label.dublincore.title", wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        FieldDefinition[] fieldDefs = wDef.getFieldDefinitions();
        assertEquals(2, fieldDefs.length);
        FieldDefinition fieldDef = fieldDefs[0];
        assertEquals("dublincore:title/value", fieldDef.getPropertyName());
        fieldDef = fieldDefs[1];
        assertEquals("dublincore:title/styleClass", fieldDef.getPropertyName());

        wDef = layoutDef.getWidgetDefinition("dublincore:description");
        assertNotNull(wDef);
        assertEquals("dublincore:description", wDef.getName());
        assertEquals("template", wDef.getType());
        assertEquals("label.dublincore.description",
                wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        fieldDefs = wDef.getFieldDefinitions();
        assertEquals(2, fieldDefs.length);
        fieldDef = fieldDefs[0];
        assertEquals("dublincore:description/value", fieldDef.getPropertyName());
        fieldDef = fieldDefs[1];
        assertEquals("dublincore:description/styleClass",
                fieldDef.getPropertyName());

        // wDef = layoutDef.getWidgetDefinition("dublincore:modified");
        // assertNotNull(wDef);
        // assertEquals("dublincore:modified", wDef.getName());
        // assertEquals("dateTime", wDef.getType());
        // assertEquals("label.diff.widget.dublincore.modified",
        // wDef.getLabel(BuiltinModes.ANY));
        // assertTrue(wDef.isTranslated());
        // fieldDefs = wDef.getFieldDefinitions();
        // assertEquals(1, fieldDefs.length);
        // fieldDef = fieldDefs[0];
        // assertEquals("dublincore:modified", fieldDef.getPropertyName());
        //
        // wDef = layoutDef.getWidgetDefinition("dublincore:subjects");
        // assertNotNull(wDef);
        // assertEquals("dublincore:subjects", wDef.getName());
        // assertEquals("list", wDef.getType());
        // assertEquals("label.diff.widget.dublincore.subjects",
        // wDef.getLabel(BuiltinModes.ANY));
        // assertTrue(wDef.isTranslated());
        // fieldDefs = wDef.getFieldDefinitions();
        // assertEquals(1, fieldDefs.length);
        // fieldDef = fieldDefs[0];
        // assertEquals("dublincore:subjects", fieldDef.getPropertyName());

        // TODO: check props ?

    }
}
