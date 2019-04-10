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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffComplexFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests overrding the contribution to the {@link DiffDisplayService}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml",
        "org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml",
        "org.nuxeo.diff.jsf.test:OSGI-INF/test-diff-display-contrib.xml" })
public class TestDiffDisplayContribOverride {

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests the diff default display contribution.
     */
    @Test
    public void testDiffDefaultDisplayContrib() {

        // Check diffExcludedFields contribs
        Map<String, List<String>> diffExcludedSchemas = diffDisplayService.getDiffExcludedSchemas();
        assertNotNull(diffExcludedSchemas);
        assertEquals(2, diffExcludedSchemas.size());
        assertTrue(diffExcludedSchemas.containsKey("common"));
        assertTrue(diffExcludedSchemas.containsKey("dublincore"));

        // Check non overriden "common" diffExcludedFields contrib
        List<String> diffExcludedFields = diffDisplayService.getDiffExcludedFields("common");
        assertNotNull(diffExcludedFields);
        assertTrue(diffExcludedFields.isEmpty());

        // Check new "dublincore" diffExcludedFields contrib
        diffExcludedFields = diffDisplayService.getDiffExcludedFields("dublincore");
        assertNotNull(diffExcludedFields);

        List<String> expectedDiffExcludedFields = new ArrayList<String>();
        expectedDiffExcludedFields.add("subjects");
        expectedDiffExcludedFields.add("modified");
        assertEquals(expectedDiffExcludedFields, diffExcludedFields);

        // Check diffComplexField contribs
        List<DiffComplexFieldDefinition> diffComplexFields = diffDisplayService.getDiffComplexFields();
        assertNotNull(diffComplexFields);
        assertEquals(2, diffComplexFields.size());

        // Check "complextypes:complex" diffComplexField contrib
        DiffComplexFieldDefinition diffComplexField = diffDisplayService.getDiffComplexField("complextypes", "complex");
        assertNotNull(diffComplexField);

        List<DiffFieldItemDefinition> expectedIncludedItems = new ArrayList<DiffFieldItemDefinition>();
        expectedIncludedItems.add(new DiffFieldItemDefinitionImpl("dateItem"));
        expectedIncludedItems.add(new DiffFieldItemDefinitionImpl("stringItem"));
        assertEquals(expectedIncludedItems, diffComplexField.getIncludedItems());

        assertTrue(diffComplexField.getExcludedItems().isEmpty());

        // Check "complextypes:complexList" diffComplexField contrib
        diffComplexField = diffDisplayService.getDiffComplexField("complextypes", "complexList");
        assertNotNull(diffComplexField);

        List<DiffFieldItemDefinition> expectedExcludedItems = new ArrayList<DiffFieldItemDefinition>();
        expectedExcludedItems.add(new DiffFieldItemDefinitionImpl("stringItem"));
        expectedExcludedItems.add(new DiffFieldItemDefinitionImpl("dateItem"));
        assertEquals(expectedExcludedItems, diffComplexField.getExcludedItems());

        assertTrue(diffComplexField.getIncludedItems().isEmpty());
    }

    /**
     * Tests the diff display contribution.
     */
    @Test
    public void testDiffDisplayContrib() {

        // Check diffDisplay contribs
        Map<String, List<String>> diffDisplays = diffDisplayService.getDiffDisplays();
        assertNotNull(diffDisplays);
        assertEquals(4, diffDisplays.size());
        assertTrue(diffDisplays.containsKey("Folder"));
        assertTrue(diffDisplays.containsKey("File"));
        assertTrue(diffDisplays.containsKey("Note"));
        assertTrue(diffDisplays.containsKey("SampleType"));

        // Check non overridden Folder diffDisplay contrib
        List<String> diffDisplay = diffDisplayService.getDiffDisplay("Folder");
        assertNotNull(diffDisplay);

        List<String> expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check overridden File diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("File");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("files");
        expectedDiffDisplay.add("testNoFields");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check non overridden Note diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("Note");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("note");
        expectedDiffDisplay.add("files");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check new SampleType diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("SampleType");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("files");
        expectedDiffDisplay.add("simpleTypes");
        expectedDiffDisplay.add("complexTypesAndListOfLists");
        assertEquals(expectedDiffDisplay, diffDisplay);
    }

    /**
     * Tests the diff block contribution.
     */
    @Test
    public void testDiffBlockContrib() {

        // Check diffBlock contribs
        Map<String, DiffBlockDefinition> contribs = diffDisplayService.getDiffBlockDefinitions();
        assertNotNull(contribs);
        assertEquals(5, contribs.size());
        assertTrue(contribs.containsKey("dublincore"));
        assertTrue(contribs.containsKey("files"));
        assertTrue(contribs.containsKey("note"));
        assertTrue(contribs.containsKey("simpleTypes"));
        assertTrue(contribs.containsKey("complexTypesAndListOfLists"));

        // Check a diffBlock contrib with no fields
        DiffBlockDefinition diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("testNoFields");
        assertNull(diffBlockDefinition);

        // Check overridden dublincore diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("dublincore");
        assertNotNull(diffBlockDefinition);

        List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "description"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "created"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "creator"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "lastContributor"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "contributors"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "subjects"));

        Map<String, String> templates = new HashMap<String, String>();
        templates.put(BuiltinModes.ANY, "/layouts/layout_diff_template.xhtml");

        Map<String, Map<String, Serializable>> properties = new HashMap<String, Map<String, Serializable>>();
        Map<String, Serializable> labelProperty = new HashMap<String, Serializable>();
        labelProperty.put("label", "label.diffBlock.dublincore");
        properties.put(BuiltinModes.ANY, labelProperty);

        DiffBlockDefinition expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("dublincore", templates, fields,
                properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden files diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("files");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "file", "content", true));
        List<DiffFieldItemDefinition> items = new ArrayList<DiffFieldItemDefinition>();
        items.add(new DiffFieldItemDefinitionImpl("file", true));
        fields.add(new DiffFieldDefinitionImpl(null, "files", "files", items));

        labelProperty.put("label", "label.diffBlock.files");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("files", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden note diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("note");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "note", "note", true));

        labelProperty.put("label", "label.diffBlock.note");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("note", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check new simpleTypes diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("simpleTypes");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "string"));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "textarea", true));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "boolean"));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "integer"));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "date"));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "htmlText", true));
        fields.add(new DiffFieldDefinitionImpl(null, "simpletypes", "multivalued"));

        labelProperty.put("label", "label.diffBlock.simpleTypes");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("simpleTypes", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check new complexTypesAndListOfLists diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("complexTypesAndListOfLists");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        items = new ArrayList<DiffFieldItemDefinition>();
        items.add(new DiffFieldItemDefinitionImpl("stringItem", true));
        items.add(new DiffFieldItemDefinitionImpl("integerItem"));
        items.add(new DiffFieldItemDefinitionImpl("dateItem"));
        items.add(new DiffFieldItemDefinitionImpl("booleanItem"));
        fields.add(new DiffFieldDefinitionImpl(null, "complextypes", "complex", items));
        items = new ArrayList<DiffFieldItemDefinition>();
        items.add(new DiffFieldItemDefinitionImpl("stringItem", true));
        items.add(new DiffFieldItemDefinitionImpl("dateItem"));
        items.add(new DiffFieldItemDefinitionImpl("integerItem"));
        fields.add(new DiffFieldDefinitionImpl(null, "complextypes", "complexList", items));
        fields.add(new DiffFieldDefinitionImpl(null, "listoflists", "listOfLists"));

        labelProperty.put("label", "label.diffBlock.complexTypesAndListOfLists");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("complexTypesAndListOfLists", templates, fields,
                properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);
    }
}
