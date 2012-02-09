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

import java.text.ParseException;
import java.util.ArrayList;
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
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffDisplayField;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexDiffDisplayField;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.ListDiffDisplayField;
import org.nuxeo.ecm.diff.model.impl.SimpleDiffDisplayField;
import org.nuxeo.ecm.diff.service.impl.DiffDisplayDescriptor;
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
     * Test diff display contrib.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testDiffDisplayContrib() throws ClientException {

        // Check service
        assertNotNull(diffDisplayService);

        // Check diffDisplay contribs
        Map<String, DiffDisplayDescriptor> contribs = diffDisplayService.getContributions();
        assertNotNull(contribs);
        assertEquals(1, contribs.size());
        assertTrue(contribs.containsKey("default"));

        // Check a non existing diffDisplay contrib
        List<DiffBlockDefinition> diffBlockDefinitions = diffDisplayService.getDiffBlockDefinitions("test");
        assertNull(diffBlockDefinitions);

        // Check default diffDisplay contrib
        diffBlockDefinitions = diffDisplayService.getDefaultDiffBlockDefinitions();
        assertNotNull(diffBlockDefinitions);

        List<DiffBlockDefinition> expectedDiffBlockDefinitions = new ArrayList<DiffBlockDefinition>();

        List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("dublincore", "title"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "subjects"));
        expectedDiffBlockDefinitions.add(new DiffBlockDefinitionImpl("general",
                fields));

        fields = new ArrayList<DiffFieldDefinition>();
        List<String> items = new ArrayList<String>();
        items.add("stringItem");
        items.add("dateItem");
        items.add("integerItem");
        fields.add(new DiffFieldDefinitionImpl("complextypes", "complexList",
                items));
        expectedDiffBlockDefinitions.add(new DiffBlockDefinitionImpl(
                "complexTypes", fields));

        assertEquals(expectedDiffBlockDefinitions, diffBlockDefinitions);

        // Check that order is taken into account
        DiffBlockDefinition diffDisplayBlock = expectedDiffBlockDefinitions.get(0);
        expectedDiffBlockDefinitions.remove(0);
        expectedDiffBlockDefinitions.add(diffDisplayBlock);

        assertFalse(expectedDiffBlockDefinitions.equals(diffBlockDefinitions));

    }

    /**
     * Test diff display service.
     * 
     * @throws ClientException the client exception
     * @throws ParseException
     */
    @Test
    public void testDiffDisplayService() throws ClientException, ParseException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.RIGHT_DOC_PATH));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Check a non existing diffDisplay contrib
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(
                "test", docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        // TODO: should not be empty, with random schema fields...
        assertTrue(diffDisplayBlocks.isEmpty());

        // Check default diffDisplay contrib
        diffDisplayBlocks = diffDisplayService.getDefaultDiffDisplayBlocks(
                docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // ------------------------------
        // Check first diffDisplay block
        // ------------------------------
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);

        // Check label
        assertEquals("label.diffBlock.general", diffDisplayBlock.getLabel());

        // Check value
        Map<String, DiffDisplayField> expectedValue = new HashMap<String, DiffDisplayField>();
        expectedValue.put("dublincore:title", new SimpleDiffDisplayField(
                "My first sample", "My second sample"));
        Calendar leftCal = DocumentDiffRepositoryInit.getCalendarUTCNoMillis(
                2011, Calendar.DECEMBER, 29, 11, 24, 25);
        Calendar rightCal = DocumentDiffRepositoryInit.getCalendarUTCNoMillis(
                2011, Calendar.DECEMBER, 30, 12, 05, 02);
        expectedValue.put("dublincore:modified", new SimpleDiffDisplayField(
                leftCal, rightCal));
        ListDiffDisplayField listField = new ListDiffDisplayField();
        ComplexDiffDisplayField item1 = new ComplexDiffDisplayField();
        item1.put("index", new SimpleDiffDisplayField("1", "1"));
        item1.put("value", new SimpleDiffDisplayField("Architecture", "N/A"));
        listField.add(item1);
        // expectedValue.put("dublincore:subjects", listField);
        expectedValue.put("dublincore:subjects", null);
        assertEquals(expectedValue, diffDisplayBlock.getValue());

        // Check layout definition
        LayoutDefinition layoutDef = diffDisplayBlock.getLayoutDefinition();
        assertEquals("general", layoutDef.getName());

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
     * Test apply complex items order.
     */
    // @Test
    // public void testApplyComplexItemsOrder() {
    //
    // List<String> complexItems = new ArrayList<String>();
    // complexItems.add("stringItem");
    // complexItems.add("booleanItem");
    // complexItems.add("integerItem");
    // complexItems.add("dateItem");
    //
    // docDiffDisplayService.applyComplexItemsOrder("complextypes", "complex",
    // complexItems);
    //
    // List<String> expectedComplexItems = new ArrayList<String>();
    // expectedComplexItems.add("integerItem");
    // expectedComplexItems.add("dateItem");
    // expectedComplexItems.add("stringItem");
    // expectedComplexItems.add("booleanItem");
    //
    // assertEquals(expectedComplexItems, complexItems);
    // }

}
