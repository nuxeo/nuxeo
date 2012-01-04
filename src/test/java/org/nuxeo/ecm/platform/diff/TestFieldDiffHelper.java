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
package org.nuxeo.ecm.platform.diff;

import java.util.ArrayList;
import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.diff.helpers.DiffTestCase;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyType;
import org.nuxeo.ecm.platform.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.PropertyHierarchyNode;
import org.nuxeo.ecm.platform.diff.service.impl.FieldDiffHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tests the FieldDiffHelper class.
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
        String xml = "<title>joe</title>";
        Node node = getNode(xml, "/title/text()");
        PropertyType propertyDiffType = FieldDiffHelper.getPropertyType(node,
                null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Simple type "schema" with a nested simple type
        xml = "<schema><title>joe</title></schema>";
        node = getNode(xml, "/schema");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        node = getNode(xml, "/schema/title");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Simple type "schema" with a nested list that has no item
        xml = "<schema><list/></schema>";
        String testXml = "<schema><list><item>joe</item></list></schema>";
        node = getNode(xml, "/schema");
        Node testNode = getNode(testXml, "/schema");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, testNode);
        assertEquals(PropertyType.simple, propertyDiffType);

        node = getNode(xml, "/schema/list");
        testNode = getNode(testXml, "/schema/list");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, testNode);
        assertEquals(PropertyType.list, propertyDiffType);

        // Simple type "schema" with a nested list that has only one item
        xml = "<schema><list><item>joe</item></list></schema>";
        node = getNode(xml, "/schema");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Simple type "schema" with a nested list that has two items
        xml = "<schema><list><item>joe</item><item>jack</item></list></schema>";
        node = getNode(xml, "/schema");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Simple type "schema" with a nested complex type
        xml = "<schema><complex><stringItem>joe</stringItem><booleanItem>true</booleanItem></complex></schema>";
        node = getNode(xml, "/schema");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // List type with no item
        xml = "<contributors/>";
        testXml = "<contributors><item>joe</item></contributors>";
        node = getNode(xml, "/contributors");
        testNode = getNode(testXml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, testNode);
        assertEquals(PropertyType.list, propertyDiffType);

        // List type with only one item
        xml = "<contributors><item>joe</item></contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        // List type with two items
        xml = "<contributors><item>joe</item><item>jack</item></contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        // List type with a nested complex item
        xml = "<contributors>"
                + "<complexItem><firstname>Antoine</firstname><lastname>Taillefer</lastname></complexItem>"
                + "</contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        node = getNode(xml, "/contributors/complexItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.complex, propertyDiffType);

        node = getNode(xml, "/contributors/complexItem/firstname");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // List type with a nested list item
        xml = "<contributors>"
                + "<item><listItem><subItem>subItem1</subItem><subItem>subItem2</subItem></listItem></item>"
                + "</contributors>";
        node = getNode(xml, "/contributors");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        node = getNode(xml, "/contributors/item");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        node = getNode(xml, "/contributors/item/listItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        node = getNode(xml, "/contributors/item/listItem/subItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Complex type
        xml = "<complex><stringItem>joe</stringItem><booleanItem>true</booleanItem></complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.complex, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Complex type with a nested list item
        xml = "<complex><stringItem>joe</stringItem>"
                + "<listItem><item>Hey</item><item>Joe</item></listItem>"
                + "</complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.complex, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        node = getNode(xml, "/complex/listItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.list, propertyDiffType);

        node = getNode(xml, "/complex/listItem/item");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        // Complex type with a nested complex item
        xml = "<complex><stringItem>joe</stringItem>"
                + "<complexItem><booleanItem>Hey</booleanItem><integerItem>10</integerItem></complexItem>"
                + "</complex>";
        node = getNode(xml, "/complex");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.complex, propertyDiffType);

        node = getNode(xml, "/complex/stringItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

        node = getNode(xml, "/complex/complexItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.complex, propertyDiffType);

        node = getNode(xml, "/complex/complexItem/booleanItem");
        propertyDiffType = FieldDiffHelper.getPropertyType(node, null);
        assertEquals(PropertyType.simple, propertyDiffType);

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
        PropertyDiff propertyDiff = new ListPropertyDiff();
        List<PropertyHierarchyNode> propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.list, "0"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.complex,
                "stringItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        PropertyDiff expectedPropertyDiff = new ListPropertyDiff();
        ComplexPropertyDiff expectedComplexPropDiff = new ComplexPropertyDiff();
        ((ListPropertyDiff) expectedPropertyDiff).addDiff(expectedComplexPropDiff);
        checkListFieldDiff(propertyDiff,
                (ListPropertyDiff) expectedPropertyDiff);

        // List of list
        propertyDiff = new ListPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.list, "0"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.list, "1"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        expectedPropertyDiff = new ListPropertyDiff();
        ((ListPropertyDiff) expectedPropertyDiff).addDiff(new ListPropertyDiff());
        checkListFieldDiff(propertyDiff,
                (ListPropertyDiff) expectedPropertyDiff);

        // Complex with nested list
        propertyDiff = new ComplexPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.complex,
                "listItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.list, "1"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff();
        ((ComplexPropertyDiff) expectedPropertyDiff).putDiff("listItem",
                new ListPropertyDiff());
        checkComplexFieldDiff(propertyDiff,
                (ComplexPropertyDiff) expectedPropertyDiff);

        // Complex with nested complex
        propertyDiff = new ComplexPropertyDiff();
        propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.complex,
                "complexItem"));
        propertyHierarchy.add(new PropertyHierarchyNode(PropertyType.complex,
                "subComplexItem"));
        FieldDiffHelper.applyPropertyHierarchyToDiff(propertyDiff,
                propertyHierarchy);

        expectedPropertyDiff = new ComplexPropertyDiff();
        expectedComplexPropDiff = new ComplexPropertyDiff();
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
