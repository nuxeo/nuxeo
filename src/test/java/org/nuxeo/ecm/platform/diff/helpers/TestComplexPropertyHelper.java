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
package org.nuxeo.ecm.platform.diff.helpers;

import java.io.Serializable;
import java.util.List;

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
import org.nuxeo.ecm.platform.diff.DocumentDiffRepositoryInit;
import org.nuxeo.ecm.platform.diff.model.PropertyType;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Tests the ComplexPropertyHelper class.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DocumentDiffRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.platform.diff.test" })
public class TestComplexPropertyHelper extends TestCase {

    @Inject
    protected CoreSession session;

    @Test
    public void testGetSimplePropertyValue() throws Exception {

        DocumentModel doc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        Serializable value = ComplexPropertyHelper.getSimplePropertyValue(doc,
                "system", "type");
        assertEquals("SampleType", value);

        value = ComplexPropertyHelper.getSimplePropertyValue(doc, "system",
                "path");
        assertEquals("/leftDoc", value);

        value = ComplexPropertyHelper.getSimplePropertyValue(doc, "system",
                "lifecycle-state");
        assertEquals("undefined", value);

        value = ComplexPropertyHelper.getSimplePropertyValue(doc, "dublincore",
                "title");
        assertEquals("My first sample", value);

        value = ComplexPropertyHelper.getSimplePropertyValue(doc,
                "simpletypes", "integer");
        assertEquals(10L, value);
    }

    @Test
    public void testGetComplexItemNames() throws Exception {

        List<String> complexItemNames = ComplexPropertyHelper.getComplexItemNames(
                "complextypes", "complex");

        assertNotNull(complexItemNames);
        assertEquals(4, complexItemNames.size());

        assertTrue(complexItemNames.contains("stringItem"));
        assertTrue(complexItemNames.contains("booleanItem"));
        assertTrue(complexItemNames.contains("integerItem"));
        assertTrue(complexItemNames.contains("dateItem"));

    }

    @Test
    public void testGetComplexItemValue() throws ClientException {

        DocumentModel doc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        Serializable value = ComplexPropertyHelper.getComplexItemValue(doc,
                "complextypes", "complex", "stringItem");
        assertEquals("string of a complex type", value);

        value = ComplexPropertyHelper.getComplexItemValue(doc, "complextypes",
                "complex", "booleanItem");
        assertEquals(true, value);

        value = ComplexPropertyHelper.getComplexItemValue(doc, "complextypes",
                "complex", "integerItem");
        assertEquals(10L, value);

        value = ComplexPropertyHelper.getComplexItemValue(doc, "complextypes",
                "complex", "dateItem");
        assertNull(value);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetListItemIndexes() throws ClientException {

        ListPropertyDiff listPropertyDiff = new ListPropertyDiff(
                PropertyType.SCALAR_LIST);
        listPropertyDiff.putDiff(1, null);
        listPropertyDiff.putDiff(3, null);
        listPropertyDiff.putDiff(5, null);

        List<Integer> listItemIndexes = ComplexPropertyHelper.getListItemIndexes(listPropertyDiff);
        List<Integer> expectedListItemIndexes = Arrays.asList(new Integer[] {
                1, 3, 5 });
        assertEquals(expectedListItemIndexes, listItemIndexes);
    }

    @Test
    public void testGetListItemValue() throws ClientException {

        DocumentModel doc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        Serializable value = ComplexPropertyHelper.getListItemValue(doc,
                "simpletypes", "multivalued", 0);
        assertEquals("monday", value);

        value = ComplexPropertyHelper.getListItemValue(doc, "simpletypes",
                "multivalued", 1);
        assertEquals("tuesday", value);

        value = ComplexPropertyHelper.getListItemValue(doc, "simpletypes",
                "multivalued", 2);
        assertEquals("wednesday", value);

        value = ComplexPropertyHelper.getListItemValue(doc, "simpletypes",
                "multivalued", 3);
        assertEquals("thursday", value);
    }

    @Test
    public void testGetComplexListItemNames() throws Exception {

        List<String> complexItemNames = ComplexPropertyHelper.getComplexListItemNames(
                "complextypes", "complexList");

        assertNotNull(complexItemNames);
        assertEquals(4, complexItemNames.size());

        assertTrue(complexItemNames.contains("stringItem"));
        assertTrue(complexItemNames.contains("booleanItem"));
        assertTrue(complexItemNames.contains("integerItem"));
        assertTrue(complexItemNames.contains("dateItem"));

    }

    @Test
    public void testGetComplexListItemValue() throws ClientException {

        DocumentModel doc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        Serializable value = ComplexPropertyHelper.getComplexListItemValue(doc,
                "complextypes", "complexList", 0, "stringItem");
        assertEquals("first element of a complex list", value);

        value = ComplexPropertyHelper.getComplexListItemValue(doc,
                "complextypes", "complexList", 0, "booleanItem");
        assertEquals(true, value);

        value = ComplexPropertyHelper.getComplexListItemValue(doc,
                "complextypes", "complexList", 0, "integerItem");
        assertEquals(12L, value);

        value = ComplexPropertyHelper.getComplexListItemValue(doc,
                "complextypes", "complexList", 0, "dateItem");
        assertNull(value);

        value = ComplexPropertyHelper.getComplexListItemValue(doc,
                "complextypes", "complexList", 1, "stringItem");
        assertNull(value);
    }

    @Test
    public void testPropertyType() throws ClientException {

        DocumentModel doc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        assertTrue(ComplexPropertyHelper.isSimpleProperty(doc.getProperty(
                "simpletypes", "string")));

        assertTrue(ComplexPropertyHelper.isListProperty(doc.getProperty(
                "simpletypes", "multivalued")));

        assertTrue(ComplexPropertyHelper.isComplexProperty(doc.getProperty(
                "complextypes", "complex")));

        assertTrue(ComplexPropertyHelper.isListProperty(doc.getProperty(
                "complextypes", "complexList")));

    }
}
