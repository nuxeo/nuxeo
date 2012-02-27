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

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.DocumentDiffRepositoryInit;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffDisplayListItem;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.impl.DiffDisplayListItemImpl;
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
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DocumentDiffRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({
        "org.nuxeo.ecm.platform.forms.layout.core:OSGI-INF/layouts-core-framework.xml",
        "org.nuxeo.diff", "org.nuxeo.diff.test" })
public class TestDiffDisplayService extends TestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DiffDisplayService diffDisplayService;

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Test diff display service.
     * 
     * @throws ClientException the client exception
     * @throws ParseException
     */
    @Test
    public void testDiffDisplayService() throws ClientException, ParseException {

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of different types
        // => must fall back on the default (Document) diffDisplay contrib
        // -----------------------------------------------------------------

        // Create left and right docs
        DocumentModel leftDoc = session.createDocumentModel("Note");
        leftDoc.setPropertyValue("dc:title", "My note");
        leftDoc.setPropertyValue("dc:subjects", new String[] { "Art",
                "Architecture" });
        leftDoc = session.createDocument(leftDoc);

        DocumentModel rightDoc = session.createDocumentModel("File");
        rightDoc.setPropertyValue("dc:title", "My file");
        rightDoc.setPropertyValue("dc:subjects", new String[] { "Art" });
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(1, diffDisplayBlocks.size());

        // Check diff display block
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.header", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 2,
                Arrays.asList("title", "subjects"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type but with no
        // diffDisplay contrib defined for this type
        // => must fall back on the default (Document) diffDisplay contrib
        // -----------------------------------------------------------------

        // Create left and right docs
        rightDoc = session.createDocumentModel("Note");
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff,
                leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(1, diffDisplayBlocks.size());

        // Check diff display block
        diffDisplayBlock = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlock, "label.diffBlock.header", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlock, "dublincore", 2,
                Arrays.asList("title", "subjects"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type with a
        // diffDisplay contrib defined for this type
        // => must use it!
        // -----------------------------------------------------------------

        // Get left and right docs
        leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));
        rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.RIGHT_DOC_PATH));

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff,
                leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // ------------------------------
        // Check first diff display block
        // ------------------------------
        diffDisplayBlock = diffDisplayBlocks.get(0);

        // Check label
        assertEquals("label.diffBlock.header", diffDisplayBlock.getLabel());

        // Check left value
        Map<String, Map<String, Serializable>> expectedValue = new HashMap<String, Map<String, Serializable>>();
        Map<String, Serializable> expectedFields = new HashMap<String, Serializable>();
        expectedFields.put("title", "My first sample");
        Calendar cal = DocumentDiffRepositoryInit.getCalendarUTCNoMillis(2011,
                Calendar.DECEMBER, 29, 11, 24, 25);
        expectedFields.put("modified", cal.getTime());
        List<Serializable> listField = new ArrayList<Serializable>();
        DiffDisplayListItem item1 = new DiffDisplayListItemImpl(1,
                "Architecture");
        listField.add((Serializable) item1);
        // TODO: test list / complex field
        expectedFields.put("subjects", (Serializable) listField);
        expectedValue.put("dublincore", expectedFields);
        assertEquals(expectedValue, diffDisplayBlock.getLeftValue());

        // Check right value
        expectedValue = new HashMap<String, Map<String, Serializable>>();
        expectedFields = new HashMap<String, Serializable>();
        expectedFields.put("title", "My second sample");
        cal = DocumentDiffRepositoryInit.getCalendarUTCNoMillis(2011,
                Calendar.DECEMBER, 30, 12, 05, 02);
        expectedFields.put("modified", cal.getTime());
        listField = new ArrayList<Serializable>();
        item1 = new DiffDisplayListItemImpl(1, "N/A");
        listField.add((Serializable) item1);
        // TODO: test list / complex field
        expectedFields.put("subjects", (Serializable) listField);
        expectedValue.put("dublincore", expectedFields);
        assertEquals(expectedValue, diffDisplayBlock.getRightValue());

        // TODO: check detailedDiff

        // Check layout definition
        LayoutDefinition layoutDef = diffDisplayBlock.getLayoutDefinition();
        assertEquals("header", layoutDef.getName());

        // Check layout row definitions
        LayoutRowDefinition[] layoutRowDefinitions = layoutDef.getRows();
        assertEquals(3, layoutRowDefinitions.length);

        LayoutRowDefinition layoutRowDef = layoutRowDefinitions[0];
        assertEquals("dublincore:title", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        WidgetReference[] widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        WidgetReference widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:title", widgetRef.getName());

        layoutRowDef = layoutRowDefinitions[1];
        assertEquals("dublincore:modified", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:modified", widgetRef.getName());

        layoutRowDef = layoutRowDefinitions[2];
        assertEquals("dublincore:subjects", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:subjects", widgetRef.getName());

        // Check layout widget definitions
        WidgetDefinition wDef = layoutDef.getWidgetDefinition("dublincore:title");
        assertNotNull(wDef);
        assertEquals("dublincore:title", wDef.getName());
        assertEquals("text", wDef.getType());
        assertEquals("label.diff.widget.dublincore.title",
                wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        FieldDefinition[] fieldDefs = wDef.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        FieldDefinition fieldDef = fieldDefs[0];
        assertEquals("dublincore:title", fieldDef.getPropertyName());

        wDef = layoutDef.getWidgetDefinition("dublincore:modified");
        assertNotNull(wDef);
        assertEquals("dublincore:modified", wDef.getName());
        assertEquals("dateTime", wDef.getType());
        assertEquals("label.diff.widget.dublincore.modified",
                wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        fieldDefs = wDef.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        fieldDef = fieldDefs[0];
        assertEquals("dublincore:modified", fieldDef.getPropertyName());

        wDef = layoutDef.getWidgetDefinition("dublincore:subjects");
        assertNotNull(wDef);
        assertEquals("dublincore:subjects", wDef.getName());
        assertEquals("list", wDef.getType());
        assertEquals("label.diff.widget.dublincore.subjects",
                wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        fieldDefs = wDef.getFieldDefinitions();
        assertEquals(1, fieldDefs.length);
        fieldDef = fieldDefs[0];
        assertEquals("dublincore:subjects", fieldDef.getPropertyName());

        // TODO: check props ?
    }

    /**
     * @param diffDisplayBlock
     */
    protected final void checkDiffDisplayBlock(
            DiffDisplayBlock diffDisplayBlock, String label, int schemaCount) {

        // Check label
        assertEquals(label, diffDisplayBlock.getLabel());

        // Check schema count on left value
        Map<String, Map<String, Serializable>> value = diffDisplayBlock.getLeftValue();
        assertNotNull(value);
        assertEquals(schemaCount, value.size());

        // Check schema count on right value
        value = diffDisplayBlock.getRightValue();
        assertNotNull(value);
        assertEquals(schemaCount, value.size());

        // TODO: manage detailedDiff
    }

    protected final void checkDiffDisplayBlockSchema(
            DiffDisplayBlock diffDisplayBlock, String schemaName,
            int fieldCount, List<String> fieldNames) {

        // Check fields on left value
        Map<String, Serializable> fields = diffDisplayBlock.getLeftValue().get(
                schemaName);
        assertNotNull(fields);
        assertEquals(fieldCount, fields.size());
        for (String fieldName : fieldNames) {
            assertTrue(fields.containsKey(fieldName));
        }

        // Check fields on right value
        fields = diffDisplayBlock.getRightValue().get(schemaName);
        assertNotNull(fields);
        assertEquals(fieldCount, fields.size());
        for (String fieldName : fieldNames) {
            assertTrue(fields.containsKey(fieldName));
        }

        // TODO: manage detailedDiff
    }
}
