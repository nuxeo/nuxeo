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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.diff.DiffTestCase;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.PropertyHierarchyNode;
import org.nuxeo.ecm.diff.service.impl.FieldDiffHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tests the {@link FieldDiffHelper}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class TestFieldDiffHelper extends DiffTestCase {

    /**
     * Tests {@link FieldDiffHelper#getPropertyType(org.w3c.dom.Node)}.
     *
     */
    @Test
    public void testGetPropertyType() {

        // Simple type
        String xml = "<title type=\"string\">joe</title>";
        Node node = getNode(xml, "/title");
        String propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        // Simple type "schema" with a nested simple type
        xml = "<schema><title type=\"string\">joe</title></schema>";
        node = getNode(xml, "/schema/title");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        // Simple type "schema" with a nested list that has no item
        xml = "<schema><list type=\"scalarList\"/></schema>";
        node = getNode(xml, "/schema/list");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.SCALAR_LIST, propertyDiffType);

        // List type with no item
        xml = "<contributors type=\"scalarList\"/>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.SCALAR_LIST, propertyDiffType);

        // List type with two items
        xml = "<contributors type=\"scalarList\"><item type=\"string\">joe</item><item type=\"string\">jack</item></contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.SCALAR_LIST, propertyDiffType);

        node = getNode(xml, "/contributors/item");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        // List type with a nested complex item
        xml = "<contributors type=\"complexList\">"
                + "<complexItem type=\"complex\"><firstname type=\"string\">Antoine</firstname><lastname type=\"string\">Taillefer</lastname></complexItem>"
                + "</contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX_LIST, propertyDiffType);

        node = getNode(xml, "/contributors/complexItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/contributors/complexItem/firstname");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        // Complex type
        xml = "<complex type=\"complex\"><stringItem type=\"string\">joe</stringItem><booleanItem type=\"boolean\">true</booleanItem></complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        node = getNode(xml, "/complex/booleanItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.BOOLEAN, propertyDiffType);

        // Complex type with a nested list item
        xml = "<complex type=\"complex\"><stringItem type=\"string\">joe</stringItem>"
                + "<listItem type=\"scalarList\"><item type=\"string\">Hey</item><item type=\"string\">Joe</item></listItem>"
                + "</complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        node = getNode(xml, "/complex/listItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.SCALAR_LIST, propertyDiffType);

        node = getNode(xml, "/complex/listItem/item");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        // Complex type with a nested complex item
        xml = "<complex type=\"complex\"><stringItem type=\"string\">joe</stringItem>"
                + "<complexItem type=\"complex\"><booleanItem type=\"boolean\">false</booleanItem><integerItem type=\"integer\">10</integerItem></complexItem>"
                + "</complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.STRING, propertyDiffType);

        node = getNode(xml, "/complex/complexItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/complex/complexItem/booleanItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.BOOLEAN, propertyDiffType);

        node = getNode(xml, "/complex/complexItem/integerItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.INTEGER, propertyDiffType);

        // Complex type with a nested content item
        xml = "<complex type=\"complex\">"
                + "<contentItem type=\"content\"><encoding>UTF-8</encoding><mime-type>text/plain</mime-type><filename>Joe.txt</filename><digest>5dafdabf966043c8c8cef20011e939a2</digest></contentItem>"
                + "</complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.COMPLEX, propertyDiffType);

        node = getNode(xml, "/complex/contentItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.CONTENT, propertyDiffType);

        // Content type
        xml = "<file type=\"content\"><encoding/><mime-type/><filename>test_file.doc</filename><digest/></file>";
        node = getNode(xml, "/file");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.CONTENT, propertyDiffType);
    }

    /**
     * Tests {@link FieldDiffHelper#applyPropertyHierarchyToDiff(PropertyDiff, List)}.
     *
     */
    @Test
    public void testApplyPropertyHierarchyToDiff() {

        // Complex list
        PropertyDiff propertyDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        List<PropertyHierarchyNode> propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX_LIST, "0"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX, "stringItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff, propertyHierarchy);

        PropertyDiff expectedPropertyDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        ((ListPropertyDiff) expectedPropertyDiff).putDiff(0, expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff, (ListPropertyDiff) expectedPropertyDiff);

        // Complex with nested list
        propertyDiff = new ComplexPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX, "listItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.SCALAR_LIST, "1"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff, propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff();
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("listItem", new ListPropertyDiff(PropertyType.SCALAR_LIST));
        checkComplexFieldDiff(propertyDiff, (ComplexPropertyDiff) expectedPropertyDiff);

        // Complex with nested complex
        propertyDiff = new ComplexPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX, "complexItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX, "subComplexItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff, propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("complexItem", expectedComplexPropDiff);
        checkComplexFieldDiff(propertyDiff, (ComplexPropertyDiff) expectedPropertyDiff);

        // Complex with nested content
        propertyDiff = new ComplexPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX, "complexItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.CONTENT, "subContentItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff, propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff();
        ContentPropertyDiff expectedContentPropDiff = new ContentPropertyDiff();
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("complexItem", expectedContentPropDiff);
        checkComplexFieldDiff(propertyDiff, (ComplexPropertyDiff) expectedPropertyDiff);

        // Content
        propertyDiff = new ContentPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.CONTENT, "encoding"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff, propertyHierarchy);

        expectedPropertyDiff = new ContentPropertyDiff();
        checkContentFieldDiff(propertyDiff, (ContentPropertyDiff) expectedPropertyDiff);
    }

    /**
     * Gets the first node matching the xPath expression in the XML doc.
     *
     * @param xml the XML
     * @param xPath the x path
     */
    protected final Node getNode(String xml, String xPath) {

        NodeList matchingNodes;
        try {
            Document xmlDoc = XMLUnit.buildControlDocument(xml);
            matchingNodes = xPathEngine.getMatchingNodes(xPath, xmlDoc);
        } catch (SAXException | IOException | XpathException e) {
            throw new NuxeoException(e);
        }

        if (!(matchingNodes.getLength() > 0)) {
            throw new NuxeoException(String.format("No node matches the xPath expression [%s] in the XML doc", xPath));
        }
        return matchingNodes.item(0);
    }

}
