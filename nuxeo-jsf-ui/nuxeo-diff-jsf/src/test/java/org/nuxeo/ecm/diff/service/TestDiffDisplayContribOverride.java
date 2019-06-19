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
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml")
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml")
@Deploy("org.nuxeo.diff.jsf.test:OSGI-INF/test-diff-display-contrib.xml")
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

        List<String> expectedDiffExcludedFields = new ArrayList<>();
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

        List<DiffFieldItemDefinition> expectedIncludedItems = new ArrayList<>();
        expectedIncludedItems.add(new DiffFieldItemDefinitionImpl("dateItem"));
        expectedIncludedItems.add(new DiffFieldItemDefinitionImpl("stringItem"));
        assertEquals(expectedIncludedItems, diffComplexField.getIncludedItems());

        assertTrue(diffComplexField.getExcludedItems().isEmpty());

        // Check "complextypes:complexList" diffComplexField contrib
        diffComplexField = diffDisplayService.getDiffComplexField("complextypes", "complexList");
        assertNotNull(diffComplexField);

        List<DiffFieldItemDefinition> expectedExcludedItems = new ArrayList<>();
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

        List<String> expectedDiffDisplay = new ArrayList<>();
        expectedDiffDisplay.add("dublincore");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check overridden File diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("File");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<>();
        expectedDiffDisplay.add("files");
        expectedDiffDisplay.add("testNoFields");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check non overridden Note diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("Note");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<>();
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("note");
        expectedDiffDisplay.add("files");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check new SampleType diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("SampleType");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<>();
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

        List<DiffFieldDefinition> fields = new ArrayList<>();
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "description"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "created"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "creator"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "lastContributor"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "contributors"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "subjects"));

        Map<String, String> templates = new HashMap<>();
        templates.put(BuiltinModes.ANY, "/layouts/layout_diff_template.xhtml");

        Map<String, Map<String, Serializable>> properties = new HashMap<>();
        Map<String, Serializable> labelProperty = new HashMap<>();
        labelProperty.put("label", "label.diffBlock.dublincore");
        properties.put(BuiltinModes.ANY, labelProperty);

        DiffBlockDefinition expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("dublincore", templates, fields,
                properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden files diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("files");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<>();
        fields.add(new DiffFieldDefinitionImpl(null, "file", "content", true));
        List<DiffFieldItemDefinition> items = new ArrayList<>();
        items.add(new DiffFieldItemDefinitionImpl("file", true));
        fields.add(new DiffFieldDefinitionImpl(null, "files", "files", items));

        labelProperty.put("label", "label.diffBlock.files");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("files", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check non overridden note diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("note");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<>();
        fields.add(new DiffFieldDefinitionImpl(null, "note", "note", true));

        labelProperty.put("label", "label.diffBlock.note");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("note", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check new simpleTypes diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("simpleTypes");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<>();
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

        fields = new ArrayList<>();
        items = new ArrayList<>();
        items.add(new DiffFieldItemDefinitionImpl("stringItem", true));
        items.add(new DiffFieldItemDefinitionImpl("integerItem"));
        items.add(new DiffFieldItemDefinitionImpl("dateItem"));
        items.add(new DiffFieldItemDefinitionImpl("booleanItem"));
        fields.add(new DiffFieldDefinitionImpl(null, "complextypes", "complex", items));
        items = new ArrayList<>();
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
