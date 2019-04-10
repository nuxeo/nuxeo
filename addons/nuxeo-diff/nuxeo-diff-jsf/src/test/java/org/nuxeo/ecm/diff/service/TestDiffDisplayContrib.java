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
import static org.junit.Assert.assertFalse;
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
 * Tests the contribution to the {@link DiffDisplayService}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-service.xml")
@Deploy("org.nuxeo.diff.jsf:OSGI-INF/diff-display-contrib.xml")
public class TestDiffDisplayContrib {

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
        assertEquals(1, diffExcludedSchemas.size());
        assertTrue(diffExcludedSchemas.containsKey("common"));

        // Check a non existing diffExcludedFields contrib
        List<String> diffExcludedFields = diffDisplayService.getDiffExcludedFields("test");
        assertNull(diffExcludedFields);

        // Check "common" diffExcludedFields contrib
        diffExcludedFields = diffDisplayService.getDiffExcludedFields("common");
        assertNotNull(diffExcludedFields);
        assertTrue(diffExcludedFields.isEmpty());
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
        assertTrue(diffDisplays.containsKey("Folder"));
        assertTrue(diffDisplays.containsKey("File"));
        assertTrue(diffDisplays.containsKey("Note"));

        // Check a non existing diffDisplay contrib
        List<String> diffDisplay = diffDisplayService.getDiffDisplay("Test");
        assertNull(diffDisplay);

        // Check Folder diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("Folder");
        assertNotNull(diffDisplay);

        List<String> expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check File diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("File");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("files");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check that order is taken into account
        String diffBlockRef = expectedDiffDisplay.get(0);
        expectedDiffDisplay.remove(0);
        expectedDiffDisplay.add(diffBlockRef);
        assertFalse(expectedDiffDisplay.equals(diffDisplay));

        // Check Note diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("Note");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("note");
        expectedDiffDisplay.add("files");
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
        assertEquals(3, contribs.size());
        assertTrue(contribs.containsKey("dublincore"));
        assertTrue(contribs.containsKey("files"));
        assertTrue(contribs.containsKey("note"));

        // Check a non existing diffBlock contrib
        DiffBlockDefinition diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("test");
        assertNull(diffBlockDefinition);

        // Check dublincore diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("dublincore");
        assertNotNull(diffBlockDefinition);

        List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "description"));
        fields.add(new DiffFieldDefinitionImpl(null, "dam_common", "author"));
        fields.add(new DiffFieldDefinitionImpl(null, "dam_common", "authoringDate"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "nature"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "subjects"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "rights"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "source"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "coverage"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "created"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "format"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "language"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "expired"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "creator"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "contributors"));
        fields.add(new DiffFieldDefinitionImpl(null, "dublincore", "lastContributor"));

        Map<String, String> templates = new HashMap<String, String>();
        templates.put(BuiltinModes.ANY, "/layouts/layout_diff_template.xhtml");

        Map<String, Map<String, Serializable>> properties = new HashMap<String, Map<String, Serializable>>();
        Map<String, Serializable> labelProperty = new HashMap<String, Serializable>();
        labelProperty.put("label", "label.diffBlock.dublincore");
        properties.put(BuiltinModes.ANY, labelProperty);

        DiffBlockDefinition expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("dublincore", templates, fields,
                properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check that order is taken into account
        DiffFieldDefinition diffFieldDefinition = expectedDiffBlockDefinition.getFields().get(0);
        expectedDiffBlockDefinition.getFields().remove(0);
        expectedDiffBlockDefinition.getFields().add(diffFieldDefinition);
        assertFalse(expectedDiffBlockDefinition.equals(diffBlockDefinition));

        // Check files diffDisplay contrib
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

        // Check note diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("note");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl(null, "note", "note", true));

        labelProperty.put("label", "label.diffBlock.note");
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("note", templates, fields, properties);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);
    }
}
