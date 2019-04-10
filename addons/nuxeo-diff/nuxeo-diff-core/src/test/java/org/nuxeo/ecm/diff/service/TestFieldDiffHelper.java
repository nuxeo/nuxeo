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

import java.util.ArrayList;
import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.diff.DiffTestCase;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.PropertyHierarchyNode;
import org.nuxeo.ecm.diff.service.impl.FieldDiffHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tests the {@link FieldDiffHelper}.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class TestFieldDiffHelper extends DiffTestCase {

    /**
     * Tests {@link FieldDiffHelper#getPropertyType(org.w3c.dom.Node)}.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testGetPropertyType() throws ClientException {

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

        // Content type
        xml = "<file type=\"content\"><encoding/><filename>test_file.doc</filename></file>";
        node = getNode(xml, "/file");
        propertyDiffType = FieldDiffHelper.getPropertyType(node);
        assertEquals(PropertyType.CONTENT, propertyDiffType);
    }

    /**
     * Tests
     * {@link FieldDiffHelper#applyPropertyHierarchyToDiff(PropertyDiff, List)}.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testApplyPropertyHierarchyToDiff() throws ClientException {

        // Complex list
        PropertyDiff propertyDiff = new ListPropertyDiff(
                PropertyType.COMPLEX_LIST);
        List<PropertyHierarchyNode> propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(
                PropertyType.COMPLEX_LIST, "0"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX,
                "stringItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        PropertyDiff expectedPropertyDiff = new ListPropertyDiff(
                PropertyType.COMPLEX_LIST);
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        ((ListPropertyDiff) expectedPropertyDiff).putDiff(0,
                expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff,
                (ListPropertyDiff) expectedPropertyDiff);

        // Complex with nested list
        propertyDiff = new ComplexPropertyDiff(PropertyType.COMPLEX);
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX,
                "listItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(
                PropertyType.SCALAR_LIST, "1"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff(PropertyType.COMPLEX);
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("listItem",
                new ListPropertyDiff(PropertyType.SCALAR_LIST));
        checkComplexFieldDiff(propertyDiff,
                (ComplexPropertyDiff) expectedPropertyDiff);

        // Complex with nested complex
        propertyDiff = new ComplexPropertyDiff(PropertyType.COMPLEX);
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX,
                "complexItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.COMPLEX,
                "subComplexItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff(PropertyType.COMPLEX);
        expectedComplexPropDiff = new ComplexPropertyDiff(PropertyType.COMPLEX);
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("complexItem",
                expectedComplexPropDiff);
        checkComplexFieldDiff(propertyDiff,
                (ComplexPropertyDiff) expectedPropertyDiff);

    }

    /**
     * Gets the first node matching the xPath expression in the XML doc.
     * 
     * @param xml the XML
     * @param xPath the x path
     * @throws ClientException the client exception
     */
    protected final Node getNode(String xml, String xPath)
            throws ClientException {

        NodeList matchingNodes;
        try {
            Document xmlDoc = XMLUnit.buildControlDocument(xml);
            matchingNodes = xPathEngine.getMatchingNodes(xPath, xmlDoc);
        } catch (Exception e) {
            throw ClientException.wrap(e);
        }

        if (!(matchingNodes.getLength() > 0)) {
            throw new ClientException(String.format(
                    "No node matches the xPath expression [%s] in the XML doc",
                    xPath));
        }
        return matchingNodes.item(0);
    }

}
