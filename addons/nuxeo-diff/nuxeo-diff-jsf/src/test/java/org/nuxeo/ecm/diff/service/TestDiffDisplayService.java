/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.diff.model.impl.PropertyDiffDisplayImpl;
import org.nuxeo.ecm.diff.test.DocumentDiffRepositoryInit;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link DiffDisplayService}.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DocumentDiffRepositoryInit.class)
@Deploy("org.nuxeo.diff.core")
@Deploy("org.nuxeo.diff.test")
@Deploy("org.nuxeo.ecm.platform.forms.layout.client")
@Deploy("org.nuxeo.ecm.platform.forms.layout.core:OSGI-INF/layouts-core-framework.xml")
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-framework.xml")
@Deploy("org.nuxeo.diff.content:OSGI-INF/content-diff-adapter-contrib.xml")
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml")
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml")
@Deploy("org.nuxeo.diff.jsf.test:OSGI-INF/test-diff-display-contrib.xml")
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-widgets-contrib.xml")
@Deploy("org.nuxeo.diff.jsf.test:OSGI-INF/test-diff-widgets-contrib.xml")
public class TestDiffDisplayService extends DiffDisplayServiceTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests diff display block schemas.
     */
    @Test
    public void testDiffDisplayBlockSchemas() throws ParseException {

        // --------------------------------------------------------------------
        // Check diff display for 2 documents of a different type: Note / File
        // with no diffDisplay contrib defined for a common super type
        // => must fall back on the default diff display.
        // --------------------------------------------------------------------

        // Create left and right docs
        DocumentModel leftDoc = session.createDocumentModel("/", "myNote", "Note");
        leftDoc.setPropertyValue("dc:description", "Description of my note");
        leftDoc.setPropertyValue("dc:creator", "Joe");
        leftDoc.setPropertyValue("note:note", "The content of my note.");
        leftDoc = session.createDocument(leftDoc);

        DocumentModel rightDoc = session.createDocumentModel("/", "myFile", "File");
        leftDoc.setPropertyValue("dc:description", "Description of my file");
        rightDoc.setPropertyValue("dc:creator", "Jack");
        rightDoc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("Joe is not rich, nor is Jack."));
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Sort them
        diffDisplayBlocks.sort(Comparator.comparing(DiffDisplayBlock::getLabel));

        // Check diff display blocks
        DiffDisplayBlock diffDisplayBlockCreator = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "dublincore", 2, Arrays.asList("description", "creator"));
        DiffDisplayBlock diffDisplayBlockMinorVersion = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlockMinorVersion, "label.diffBlock.uid", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockMinorVersion, "uid", 1, Collections.singletonList("minor_version"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type: OtherSampleType
        // with no diffDisplay contrib defined for this type or one of its
        // super types => must fall back on the default diff display.
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("/", "myFirstOtherSampleType", "OtherSampleType");
        leftDoc.setPropertyValue("dc:description", "Description of my first other sample type");
        leftDoc.setPropertyValue("st:boolean", true);
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("/", "mySecondOtherSampleType", "OtherSampleType");
        rightDoc.setPropertyValue("dc:description", "Description of my second other sample type");
        rightDoc.setPropertyValue("st:boolean", false);
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlockCreator = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "dublincore", 1, Arrays.asList("description"));

        diffDisplayBlockCreator = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.simpletypes", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "simpletypes", 1, Arrays.asList("boolean"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type: Note with a
        // diffDisplay contrib defined for this type.
        // => must use it!
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("/", "myFirstNote", "Note");
        leftDoc.setPropertyValue("dc:description", "Description of my first note");
        leftDoc.setPropertyValue("dc:creator", "Joe");
        leftDoc.setPropertyValue("note:note", "The content of my first note is short.");
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("/", "mySecondNote", "Note");
        rightDoc.setPropertyValue("dc:description", "Description of my second note");
        rightDoc.setPropertyValue("dc:creator", "Jack");
        rightDoc.setPropertyValue("note:note", "The content of my second note written by Jack is still short.");
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlockCreator = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "dublincore", 2, Arrays.asList("description", "creator"));

        diffDisplayBlockCreator = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.note", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "note", 2, Arrays.asList("note"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of the same type: ExtendedNote
        // with a diffDisplay contrib defined for one of its super type: Note =>
        // must use it!
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("/", "myExtendedNote", "ExtendedNote");
        leftDoc.setPropertyValue("dc:description", "Description of my extended note");
        leftDoc.setPropertyValue("dc:creator", "Jack");
        leftDoc.setPropertyValue("note:note", "Joe is not rich, nor is Jack.");
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("/", "mySecondExtendedNote", "ExtendedNote");
        rightDoc.setPropertyValue("dc:description", "Description of my second extended note");
        rightDoc.setPropertyValue("dc:creator", "Joe");
        rightDoc.setPropertyValue("note:note", "Joe is much richer than Jack.");
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlockCreator = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "dublincore", 2, Arrays.asList("description", "creator"));

        diffDisplayBlockCreator = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.note", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "note", 2, Arrays.asList("note"));

        // -----------------------------------------------------------------
        // Check diff display for 2 documents of a different type:
        // ExtendedNote/ExtendedExtendedNote with a diffDisplay contrib defined
        // for a common super type: Note => must use it!
        // -----------------------------------------------------------------

        // Create left and right docs
        leftDoc = session.createDocumentModel("/", "myExtendedNote", "ExtendedNote");
        leftDoc.setPropertyValue("dc:description", "Description of my extended note");
        leftDoc.setPropertyValue("dc:creator", "Jack");
        leftDoc.setPropertyValue("note:note", "Joe is not rich, nor is Jack.");
        leftDoc = session.createDocument(leftDoc);

        rightDoc = session.createDocumentModel("/", "myExtendedExtendedNote", "ExtendedExtendedNote");
        rightDoc.setPropertyValue("dc:description", "Description of my extended extended note");
        rightDoc.setPropertyValue("dc:creator", "Joe");
        rightDoc.setPropertyValue("note:note", "Joe is much richer than Jack.");
        rightDoc = session.createDocument(rightDoc);

        // Do doc diff
        docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(2, diffDisplayBlocks.size());

        // Check diff display blocks
        diffDisplayBlockCreator = diffDisplayBlocks.get(0);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.dublincore", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "dublincore", 2, Arrays.asList("description", "creator"));

        diffDisplayBlockCreator = diffDisplayBlocks.get(1);
        checkDiffDisplayBlock(diffDisplayBlockCreator, "label.diffBlock.note", 1);
        checkDiffDisplayBlockSchema(diffDisplayBlockCreator, "note", 2, Arrays.asList("note"));
    }

    /**
     * Tests diff display blocks.
     */
    @Test
    public void testDiffDisplayBlocks() {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getLeftDocPath()));
        DocumentModel rightDoc = session.getDocument(new PathRef(DocumentDiffRepositoryInit.getRightDocPath()));

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);

        // Get diff display blocks
        List<DiffDisplayBlock> diffDisplayBlocks = diffDisplayService.getDiffDisplayBlocks(docDiff, leftDoc, rightDoc);
        assertNotNull(diffDisplayBlocks);
        assertEquals(4, diffDisplayBlocks.size());

        // -------------------------------------------
        // Check first diff display block: dublincore
        // -------------------------------------------
        DiffDisplayBlock diffDisplayBlock = diffDisplayBlocks.get(0);

        // Check label
        assertEquals("label.diffBlock.dublincore", diffDisplayBlock.getLabel());

        // Check left value
        Map<String, Map<String, PropertyDiffDisplay>> expectedValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        Map<String, PropertyDiffDisplay> expectedFields = new HashMap<String, PropertyDiffDisplay>();
        // description
        expectedFields.put("description",
                new PropertyDiffDisplayImpl("description", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        // subjects
        List<Map<String, Serializable>> subjects = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> subject = new HashMap<String, Serializable>();
        subject.put("index", 2);
        subject.put("value", new PropertyDiffDisplayImpl("Architecture", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        subjects.add(subject);
        expectedFields.put("subjects", new PropertyDiffDisplayImpl((Serializable) subjects));
        // no diff display for dc:created since it is < 1 minute
        // modified
        Calendar cal = DocumentDiffRepositoryInit.getCalendarNoMillis(2011, Calendar.DECEMBER, 29, 11, 24, 25);
        expectedFields.put("modified",
                new PropertyDiffDisplayImpl(cal.getTime(), PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        expectedValue.put("dublincore", expectedFields);
        // contributors
        List<Map<String, Serializable>> contributors = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> contributor1 = new HashMap<String, Serializable>();
        contributor1.put("index", 1);
        contributor1.put("value",
                new PropertyDiffDisplayImpl("Administrator", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        contributors.add(contributor1);
        Map<String, Serializable> contributor2 = new HashMap<String, Serializable>();
        contributor2.put("index", 3);
        contributor2.put("value", new PropertyDiffDisplayImpl(null, PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        contributors.add(contributor2);
        expectedFields.put("contributors", new PropertyDiffDisplayImpl((Serializable) contributors));
        // TODO: test a complex field
        assertEquals(expectedValue, diffDisplayBlock.getLeftValue());

        // Check right value
        expectedValue = new HashMap<String, Map<String, PropertyDiffDisplay>>();
        expectedFields = new HashMap<String, PropertyDiffDisplay>();
        // description
        expectedFields.put("description", new PropertyDiffDisplayImpl(null, PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        // subjects
        subjects = new ArrayList<Map<String, Serializable>>();
        subject = new HashMap<String, Serializable>();
        subject.put("index", 2);
        subject.put("value", new PropertyDiffDisplayImpl(null, PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        subjects.add(subject);
        expectedFields.put("subjects", new PropertyDiffDisplayImpl((Serializable) subjects));
        // no diff display for dc:created since it is < 1 minute
        // modified
        cal = DocumentDiffRepositoryInit.getCalendarNoMillis(2011, Calendar.DECEMBER, 30, 12, 05, 02);
        expectedFields.put("modified",
                new PropertyDiffDisplayImpl(cal.getTime(), PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        expectedValue.put("dublincore", expectedFields);
        // contributors
        contributors = new ArrayList<Map<String, Serializable>>();
        contributor1 = new HashMap<String, Serializable>();
        contributor1.put("index", 1);
        contributor1.put("value",
                new PropertyDiffDisplayImpl("anotherAdministrator", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        contributors.add(contributor1);
        contributor2 = new HashMap<String, Serializable>();
        contributor2.put("index", 3);
        contributor2.put("value", new PropertyDiffDisplayImpl("jack", PropertyDiffDisplay.DEFAULT_STYLE_CLASS));
        contributors.add(contributor2);
        expectedFields.put("contributors", new PropertyDiffDisplayImpl((Serializable) contributors));
        // TODO: test a complex field
        assertEquals(expectedValue, diffDisplayBlock.getRightValue());

        // TODO: check contentDiff

        // Check layout definition
        LayoutDefinition layoutDef = diffDisplayBlock.getLayoutDefinition();
        assertEquals("dublincore", layoutDef.getName());

        // Check layout row definitions
        LayoutRowDefinition[] layoutRowDefinitions = layoutDef.getRows();
        assertEquals(4, layoutRowDefinitions.length);

        LayoutRowDefinition layoutRowDef = layoutRowDefinitions[0];
        assertEquals("dublincore:description", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        WidgetReference[] widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        WidgetReference widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:description", widgetRef.getName());

        layoutRowDef = layoutRowDefinitions[1];
        assertEquals("dublincore:modified", layoutRowDef.getName());
        assertEquals(1, layoutRowDef.getSize());
        widgetRefs = layoutRowDef.getWidgetReferences();
        assertEquals(1, widgetRefs.length);
        widgetRef = widgetRefs[0];
        assertEquals("diff", widgetRef.getCategory());
        assertEquals("dublincore:modified", widgetRef.getName());

        // Check layout widget definitions
        WidgetDefinition wDef = layoutDef.getWidgetDefinition("dublincore:description");
        assertNotNull(wDef);
        assertEquals("dublincore:description", wDef.getName());
        assertEquals("template", wDef.getType());
        assertEquals("label.dublincore.description", wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        FieldDefinition[] fieldDefs = wDef.getFieldDefinitions();
        assertEquals(2, fieldDefs.length);
        FieldDefinition fieldDef = fieldDefs[0];
        assertEquals("dublincore:description/value", fieldDef.getPropertyName());
        fieldDef = fieldDefs[1];
        assertEquals("dublincore:description/styleClass", fieldDef.getPropertyName());

        // no widget defintion for dc:created since it is < 1 minute
        wDef = layoutDef.getWidgetDefinition("dublincore:created");
        assertNull(wDef);

        wDef = layoutDef.getWidgetDefinition("dublincore:modified");
        assertNotNull(wDef);
        assertEquals("dublincore:modified", wDef.getName());
        assertEquals("datetime", wDef.getType());
        assertEquals("label.dublincore.modified", wDef.getLabel(BuiltinModes.ANY));
        assertTrue(wDef.isTranslated());
        fieldDefs = wDef.getFieldDefinitions();
        assertEquals(2, fieldDefs.length);
        fieldDef = fieldDefs[0];
        assertEquals("dublincore:modified/value", fieldDef.getPropertyName());
        fieldDef = fieldDefs[1];
        assertEquals("dublincore:modified/styleClass", fieldDef.getPropertyName());

        // TODO: check other widget definitions

        // TODO: check props?
    }
}
