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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

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
    }

    /**
     * Tests the diff display contribution.
     */
    @Test
    public void testDiffDisplayContrib() {

        // Check diffDisplay contribs
        Map<String, List<String>> diffDisplays = diffDisplayService.getDiffDisplays();
        assertNotNull(diffDisplays);
        assertEquals(3, diffDisplays.size());
        assertTrue(diffDisplays.containsKey("File"));
        assertTrue(diffDisplays.containsKey("Note"));
        assertTrue(diffDisplays.containsKey("SampleType"));

        // Check overridden File diffDisplay contrib
        List<String> diffDisplay = diffDisplayService.getDiffDisplay("File");
        assertNotNull(diffDisplay);

        List<String> expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("heading");
        expectedDiffDisplay.add("files");
        expectedDiffDisplay.add("testNoFields");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check non overridden Note diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("Note");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("heading");
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("note");
        expectedDiffDisplay.add("files");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check new SampleType diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("SampleType");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("heading");
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
        assertEquals(6, contribs.size());
        assertTrue(contribs.containsKey("heading"));
        assertTrue(contribs.containsKey("dublincore"));
        assertTrue(contribs.containsKey("files"));
        assertTrue(contribs.containsKey("note"));
        assertTrue(contribs.containsKey("simpleTypes"));
        assertTrue(contribs.containsKey("complexTypesAndListOfLists"));

        // Check a diffBlock contrib with no fields
        DiffBlockDefinition diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("testNoFields");
        assertNull(diffBlockDefinition);

        // Check non overridden heading diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("heading");
        assertNotNull(diffBlockDefinition);

        List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("dublincore", "title"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "description"));

        Map<String, String> templates = new HashMap<String, String>();
        templates.put(BuiltinModes.ANY, "/layouts/layout_diff_template.xhtml");

        Map<String, Map<String, Serializable>> properties = new HashMap<String, Map<String, Serializable>>();
        Map<String, Serializable> labelProperty = new HashMap<String, Serializable>();
        labelProperty.put("label", "label.diffBlock.heading");
        properties.put(BuiltinModes.ANY, labelProperty);

        DiffBlockDefinition expectedDiffBlockDefinition = new DiffBlockDefinitionImpl(
                "heading", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check overridden dublincore diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("dublincore");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("dublincore", "created"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "creator"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "lastContributor"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "contributors"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "subjects"));

        labelProperty.put("label", "label.diffBlock.dublincore");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("dublincore",
                templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden files diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("files");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("file", "content", true));
        List<DiffFieldItemDefinition> items = new ArrayList<DiffFieldItemDefinition>();
        items.add(new DiffFieldItemDefinitionImpl("file", true));
        fields.add(new DiffFieldDefinitionImpl("files", "files", items));

        labelProperty.put("label", "label.diffBlock.files");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("files",
                templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden note diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("note");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("note", "note", true));

        labelProperty.put("label", "label.diffBlock.note");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("note",
                templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check new simpleTypes diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("simpleTypes");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "string"));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "textarea", true));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "boolean"));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "integer"));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "date"));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "htmlText", true));
        fields.add(new DiffFieldDefinitionImpl("simpletypes", "multivalued"));

        labelProperty.put("label", "label.diffBlock.simpleTypes");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl(
                "simpleTypes", templates, fields, properties);
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
        fields.add(new DiffFieldDefinitionImpl("complextypes", "complex", items));
        items = new ArrayList<DiffFieldItemDefinition>();
        items.add(new DiffFieldItemDefinitionImpl("stringItem", true));
        items.add(new DiffFieldItemDefinitionImpl("dateItem"));
        items.add(new DiffFieldItemDefinitionImpl("integerItem"));
        fields.add(new DiffFieldDefinitionImpl("complextypes", "complexList",
                items));
        fields.add(new DiffFieldDefinitionImpl("listoflists", "listOfLists"));

        labelProperty.put("label", "label.diffBlock.complexTypesAndListOfLists");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl(
                "complexTypesAndListOfLists", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);
    }
}
