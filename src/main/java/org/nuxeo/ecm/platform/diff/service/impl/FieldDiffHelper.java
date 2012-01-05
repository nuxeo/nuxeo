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
package org.nuxeo.ecm.platform.diff.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.NodeDetail;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyType;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.PropertyHierarchyNode;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper for computing a field diff.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class FieldDiffHelper {

    private static final Log LOGGER = LogFactory.getLog(FieldDiffHelper.class);

    private static final String SYSTEM_ELEMENT = "system";

    private static final String SCHEMA_ELEMENT = "schema";

    private static final String NAME_ATTRIBUTE = "name";

    /**
     * Computes a field diff.
     * <p>
     * First gets all needed elements to compute the field diff:
     * <ul>
     * <li>
     * propertyHierarchy: list holding the property hierarchy
     * 
     * <pre>
     * Every time we encounter a list or complex node going up in the DOM tree
     * from the property node to the prefixed field node, we add it to the property
     * hierarchy.
     * If it is a list item node, we set its index in the hierarchy.
     * If it is a complex item node, we set its name in the hierarchy.
     * 
     * Example: complex list
     * 
     * The "true" property's hierarchy is:
     * [{list,"0"},{complex, "complexBoolean"}]
     * 
     * The "jack" property's hierarchy is:
     * [{list,"1"},{complex, "complexString"}]
     * 
     * <list>
     *   <complexItem>
     *      <complexString>joe</complexString>
     *      <complexBoolean>true</complexBoolean>
     *   </complexItem>
     *   <complexItem>
     *      <complexString>jack</complexString>
     *      <complexBoolean>false</complexBoolean>
     *   </complexItem>
     * </list>
     * </pre>
     * 
     * </li>
     * <li>
     * isChildNodeNotFoundOnTestSide: boolean to check if we are in a
     * "CHILD_NODE_NOT_FOUND on the test side" case, which, I don't really
     * understand why, is marked as a TEXT_VALUE difference...
     * 
     * For example in this case we will have a TEXT_VALUE difference
     * {green/red}.
     * 
     * <pre>
     * [----- control ------]   [----- test ------]
     * 
     * <field>                  <field>
     * <item>red</item>         <item>red</item>
     * <item>green</item>     </field>
     * </field>
     * 
     * </pre>
     * 
     * </li>
     * </ul>
     * 
     * @param docDiff the doc diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @param fieldDifferenceCount the field difference count
     * @param difference the difference
     * @throws ClientException the client exception
     */
    public static void computeFieldDiff(DocumentDiff docDiff,
            NodeDetail controlNodeDetail, NodeDetail testNodeDetail,
            Integer fieldDifferenceCount, Difference difference)
            throws ClientException {

        // Use control node or if null test node to detect schema and
        // field elements
        Node currentNode = controlNodeDetail.getNode();
        Node otherCurrentNode = null;
        if (currentNode == null) {
            currentNode = testNodeDetail.getNode();
        } else {
            otherCurrentNode = testNodeDetail.getNode();
        }
        if (currentNode != null) {

            String field = null;
            String currentNodeName = currentNode.getNodeName();
            List<PropertyHierarchyNode> propertyHierarchy = new ArrayList<PropertyHierarchyNode>();
            boolean isChildNodeNotFoundOnTestSide = false;

            // Detect a schema element,
            // for instance: <schema name="dublincore" xmlns:dc="...">,
            // or the <system> element.
            Node parentNode = currentNode.getParentNode();
            Node otherParentNode = null;
            if (otherCurrentNode != null) {
                otherParentNode = otherCurrentNode.getParentNode();
            }
            while (parentNode != null
                    && !SCHEMA_ELEMENT.equals(currentNodeName)
                    && !SYSTEM_ELEMENT.equals(currentNodeName)) {

                // Get property type
                PropertyType propertyType = getPropertyType(currentNode,
                        otherCurrentNode);
                PropertyType parentPropertyType = getPropertyType(parentNode,
                        otherParentNode);

                // Fill in property hierarchy
                if (PropertyType.list.equals(parentPropertyType)) {
                    int currentNodePosition = getNodePosition(currentNode);

                    if (otherCurrentNode != null) {
                        int otherCurrentNodePosition = getNodePosition(otherCurrentNode);
                        if (otherCurrentNodePosition < currentNodePosition) {
                            if (PropertyType.complex.equals(propertyType)) {
                                currentNodePosition--;
                            }
                            isChildNodeNotFoundOnTestSide = true;
                        }
                    }

                    propertyHierarchy.add(new PropertyHierarchyNode(
                            parentPropertyType,
                            String.valueOf(currentNodePosition)));
                } else if (PropertyType.complex.equals(parentPropertyType)) {
                    propertyHierarchy.add(new PropertyHierarchyNode(
                            parentPropertyType, currentNodeName));
                }

                // Detect a field element, ie. an element that has a
                // prefix, for instance: <dc:title>,
                // or an element nested in <system>.
                if (SCHEMA_ELEMENT.equals(parentNode.getNodeName())
                        || SYSTEM_ELEMENT.equals(parentNode.getNodeName())) {
                    field = currentNode.getLocalName();
                    if (PropertyType.simple.equals(propertyType)) {
                        propertyHierarchy.add(new PropertyHierarchyNode(
                                propertyType, null));
                    } else if (PropertyType.list.equals(propertyType)
                            && propertyHierarchy.isEmpty()) {
                        propertyHierarchy.add(new PropertyHierarchyNode(
                                propertyType, null));
                    } else if (PropertyType.complex.equals(propertyType)
                            && propertyHierarchy.isEmpty()) {
                        propertyHierarchy.add(new PropertyHierarchyNode(
                                propertyType, null));
                    }

                }
                currentNode = parentNode;
                currentNodeName = currentNode.getNodeName();
                parentNode = parentNode.getParentNode();

                if (otherParentNode != null) {
                    otherCurrentNode = otherParentNode;
                    otherParentNode = otherParentNode.getParentNode();
                }
            }

            // If we found a schema or system element (ie. we did not
            // reached the root element, ie. parentNode != null) and a
            // nested field element, we can compute the diff for this
            // field.
            if (parentNode != null && field != null
                    && !propertyHierarchy.isEmpty()) {
                String schema = currentNodeName;
                // Get schema name if not system
                if (!SYSTEM_ELEMENT.equals(schema)) {
                    NamedNodeMap attr = currentNode.getAttributes();
                    if (attr != null && attr.getLength() > 0) {
                        Node nameAttr = attr.getNamedItem(NAME_ATTRIBUTE);
                        if (nameAttr != null) {
                            schema = nameAttr.getNodeValue();
                        }
                    }
                }

                // Reverse property hierarchy
                Collections.reverse(propertyHierarchy);

                // Increment field differences count and pretty
                // log field difference
                fieldDifferenceCount++;
                LOGGER.info(String.format(
                        "Found field difference #%d on [%s]/[%s] with hierarchy %s: [%s (%s)] {%s --> %s}",
                        fieldDifferenceCount, schema, field, propertyHierarchy,
                        difference.getDescription(), difference.getId(),
                        controlNodeDetail.getValue(), testNodeDetail.getValue()));

                // Compute field diff
                computeFieldDiff(docDiff, schema, field, propertyHierarchy,
                        difference.getId(), controlNodeDetail, testNodeDetail,
                        isChildNodeNotFoundOnTestSide);
            } else {// Non-field difference
                LOGGER.debug(String.format(
                        "Found non-field difference: [%s (%s)] {%s --> %s}",
                        difference.getDescription(), difference.getId(),
                        controlNodeDetail.getValue(), testNodeDetail.getValue()));
            }
        }
    }

    /**
     * Gets the node property type.
     * 
     * @param node the node
     * @return the property diff type
     */
    public static PropertyType getPropertyType(Node node, Node testNode) {

        // Default: simple type
        PropertyType propertyType = PropertyType.simple;

        // Particular case of system and schema elements
        if (SYSTEM_ELEMENT.equals(node.getNodeName())
                || SCHEMA_ELEMENT.equals(node.getNodeName())) {
            return propertyType;
        }

        NodeList childNodes = node.getChildNodes();
        int childNodesLength = childNodes.getLength();

        if (testNode != null) {
            NodeList testChildNodes = testNode.getChildNodes();
            int testChildNodesLength = testChildNodes.getLength();
            if (childNodesLength == 0 && testChildNodesLength > 0) {
                childNodes = testChildNodes;
                childNodesLength = testChildNodesLength;
            }
        }

        // Only one child node
        if (childNodesLength == 1) {
            Node childNode = childNodes.item(0);
            // If the only child node is an element node, but not a list
            // element
            // => list type with one item (could also be a complex type with
            // one
            // item but we have to chose and this makes less sense)
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (!(PropertyType.list.equals(getPropertyType(childNode, null)))) {
                    propertyType = PropertyType.list;
                }
            }
            // Else the first child node is a text node or a list or complex
            // element => keep simple type
        }

        // At least 2 child nodes => list or complex type
        if (childNodesLength > 1) {

            // Default: list type
            propertyType = PropertyType.list;

            for (int i = 1; i < childNodes.getLength(); i++) {
                String firstChildNodeName = childNodes.item(0).getNodeName();
                if (!firstChildNodeName.equals(childNodes.item(i).getNodeName())) {

                    // All child nodes don't have the same name => complex type
                    propertyType = PropertyType.complex;
                    break;
                }
            }
        }

        return propertyType;
    }

    /**
     * Gets the node position.
     * 
     * @param node the node
     * @return the node position
     */
    private static int getNodePosition(Node node) {

        int nodePos = 0;
        Node previousSibling = node.getPreviousSibling();
        while (previousSibling != null) {
            nodePos++;
            previousSibling = previousSibling.getPreviousSibling();
        }
        return nodePos;
    }

    /**
     * Sets the property diff hierarchy.
     * 
     * @param firstPropertyDiff the first property diff
     * @param propertyHierarchy the property hierarchy
     * @return the property diff
     * @throws ClientException the client exception
     */
    public static PropertyDiff applyPropertyHierarchyToDiff(
            PropertyDiff firstPropertyDiff,
            List<PropertyHierarchyNode> propertyHierarchy)
            throws ClientException {

        if (propertyHierarchy.isEmpty()) {
            throw new ClientException("Empty property hierarchy.");
        }

        // Get first property hierarchy node
        PropertyHierarchyNode propertyHierarchyNode = propertyHierarchy.get(0);
        PropertyType firstPropertyType = propertyHierarchyNode.getNodeType();
        String firstPropertyValue = propertyHierarchyNode.getNodeValue();

        if (PropertyType.simple.equals(firstPropertyType)
                && propertyHierarchy.size() > 1) {
            throw new ClientException(String.format(
                    "Inconsistant property hierarchy %s.", propertyHierarchy));
        }

        // Go through the property hierarchy
        PropertyDiff propertyDiff = firstPropertyDiff;
        PropertyType propertyType = firstPropertyType;
        String propertyValue = firstPropertyValue;
        for (int i = 1; i < propertyHierarchy.size(); i++) {

            PropertyDiff childPropertyDiff = null;
            PropertyHierarchyNode childPropertyHierarchyNode = propertyHierarchy.get(i);
            PropertyType childPropertyType = childPropertyHierarchyNode.getNodeType();
            String childPropertyValue = childPropertyHierarchyNode.getNodeValue();

            switch (propertyType) {
            default: // simple type
                // Nothing to do here (should never happen)
                break;
            case list:
                int propertyIndex = Integer.parseInt(propertyValue);
                int listPropertyDiffSize = ((ListPropertyDiff) propertyDiff).size();
                // Check that index is not greater than list size
                if (propertyIndex > listPropertyDiffSize) {
                    throw new ClientException(
                            "First property hierarchy node should not have an index > than the property diff list size.");
                }
                // Get list diff if index within list size, otherwise initialize
                // it
                if (propertyIndex < listPropertyDiffSize) {
                    childPropertyDiff = ((ListPropertyDiff) propertyDiff).getDiff(propertyIndex);
                } else {
                    childPropertyDiff = newPropertyDiff(childPropertyType);
                    ((ListPropertyDiff) propertyDiff).addDiff(propertyIndex,
                            childPropertyDiff);
                }
                propertyDiff = childPropertyDiff;
                break;
            case complex:
                // Get complex diff, initialize it if null
                childPropertyDiff = ((ComplexPropertyDiff) propertyDiff).getDiff(propertyValue);
                if (childPropertyDiff == null) {
                    childPropertyDiff = newPropertyDiff(childPropertyType);
                    ((ComplexPropertyDiff) propertyDiff).putDiff(propertyValue,
                            childPropertyDiff);
                }
                propertyDiff = childPropertyDiff;
                break;
            }

            propertyType = childPropertyType;
            propertyValue = childPropertyValue;
        }
        return propertyDiff;
    }

    /**
     * Computes a field diff.
     * 
     * @param docDiff the doc diff
     * @param schema the schema
     * @param field the field
     * @param propertyHierarchy the property hierarchy
     * @param differenceId the difference id
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @throws ClientException the client exception
     */
    private static void computeFieldDiff(DocumentDiff docDiff, String schema,
            String field, List<PropertyHierarchyNode> propertyHierarchy,
            int differenceId, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail, boolean isChildNodeNotFoundOnTestSide)
            throws ClientException {

        if (propertyHierarchy.isEmpty()) {
            throw new ClientException("Empty property hierarchy.");
        }

        // Get first property hierarchy node
        PropertyHierarchyNode propertyHierarchyNode = propertyHierarchy.get(0);
        PropertyType firstPropertyType = propertyHierarchyNode.getNodeType();

        // Get schema diff, initialize it if null
        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        if (schemaDiff == null) {
            schemaDiff = docDiff.initSchemaDiff(schema);
        }

        // Get field diff, initialize it if null
        PropertyDiff fieldDiff = schemaDiff.getFieldDiff(field);
        if (fieldDiff == null) {
            fieldDiff = newPropertyDiff(firstPropertyType);
        }

        PropertyDiff endPropertyDiff = fieldDiff;
        // Apply property hierarchy to diff if first property type in hierarchy
        // is list or complex
        if (!(PropertyType.simple.equals(firstPropertyType))) {
            endPropertyDiff = applyPropertyHierarchyToDiff(fieldDiff,
                    propertyHierarchy);
        }

        // Compute field diff depending on difference type.
        switch (differenceId) {
        default:// In most cases: TEXT_VALUE_ID
            computeTextValueDiff(endPropertyDiff, controlNodeDetail,
                    testNodeDetail, propertyHierarchy,
                    isChildNodeNotFoundOnTestSide);
            break;
        case DifferenceConstants.CHILD_NODE_NOT_FOUND_ID:
            computeChildNodeNotFoundDiff(endPropertyDiff, controlNodeDetail,
                    testNodeDetail);
            break;
        case DifferenceConstants.HAS_CHILD_NODES_ID:
            computeHasChildNodesDiff(endPropertyDiff, controlNodeDetail,
                    testNodeDetail);
            break;
        }

        schemaDiff.putFieldDiff(field, fieldDiff);
    }

    /**
     * New property diff.
     * 
     * @param propertyType the property type
     * @return the property diff
     */
    private static PropertyDiff newPropertyDiff(PropertyType propertyType) {

        switch (propertyType) {
        default: // simple type
            return new SimplePropertyDiff();
        case list:
            return new ListPropertyDiff();
        case complex:
            return new ComplexPropertyDiff();
        }
    }

    /**
     * Computes a TEXT_VALUE diff.
     * 
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @param propertyHierarchy the property hierarchy
     */
    private static void computeTextValueDiff(PropertyDiff fieldDiff,
            NodeDetail controlNodeDetail, NodeDetail testNodeDetail,
            List<PropertyHierarchyNode> propertyHierarchy,
            boolean isChildNodeNotFoundOnTestSide) {

        // SimplePropertyDiff propertyDiff = checkChildNodeNotFoundOnTestSide(
        // controlNodeDetail, testNodeDetail, propertyHierarchy);
        // String leftValue = propertyDiff.getLeftValue();
        // String rightValue = propertyDiff.getRightValue();

        String leftValue = controlNodeDetail.getValue();
        String rightValue = testNodeDetail.getValue();
        if (isChildNodeNotFoundOnTestSide) {
            rightValue = null;
        }

        switch (fieldDiff.getPropertyType()) {
        default: // simple type
            ((SimplePropertyDiff) fieldDiff).setLeftValue(leftValue);
            ((SimplePropertyDiff) fieldDiff).setRightValue(rightValue);
            break;
        case list:
            ((ListPropertyDiff) fieldDiff).addDiff(new SimplePropertyDiff(
                    leftValue, rightValue));
            break;
        case complex:
            Node controlNode = controlNodeDetail.getNode();
            if (controlNode != null) {
                Node controlParentNode = controlNode.getParentNode();
                if (controlParentNode != null) {
                    ((ComplexPropertyDiff) fieldDiff).putDiff(
                            controlParentNode.getNodeName(),
                            new SimplePropertyDiff(leftValue, rightValue));
                }
            }
            break;
        }
    }

    /**
     * Computes a CHILD_NODE_NOT_FOUND diff.
     * 
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @throws ClientException the client exception
     */
    private static void computeChildNodeNotFoundDiff(PropertyDiff fieldDiff,
            NodeDetail controlNodeDetail, NodeDetail testNodeDetail)
            throws ClientException {

        Node childNode;
        boolean isTestNodeNotFound = "null".equals(testNodeDetail.getValue());
        if (!isTestNodeNotFound) {
            childNode = testNodeDetail.getNode();
        }
        // Should never happen as then it would be marked as a
        // TEXT_VALUE difference.
        else {
            childNode = controlNodeDetail.getNode();
        }

        if (childNode != null) {

            switch (fieldDiff.getPropertyType()) {
            default: // simple type
                // Should never happen as then it would be marked as a
                // HAS_CHILD_NODES difference.
                throw new ClientException(
                        "A CHILD_NODE_NOT_FOUND difference should never be found within a simple type.");
            case list:
                PropertyDiff childNodeDiff = getChildNodePropertyDiff(
                        childNode, isTestNodeNotFound);
                ((ListPropertyDiff) fieldDiff).addDiff(childNodeDiff);
                break;
            case complex:
                throw new ClientException(
                        "A CHILD_NODE_NOT_FOUND difference should never be found within a complex type.");

            }
        }
    }

    /**
     * Computes a HAS_CHILD_NODES diff.
     * 
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @throws ClientException the client exception
     */
    private static void computeHasChildNodesDiff(PropertyDiff fieldDiff,
            NodeDetail controlNodeDetail, NodeDetail testNodeDetail)
            throws ClientException {

        Node nodeWithChilds;
        boolean hasControlNodeChildNodes = Boolean.valueOf(controlNodeDetail.getValue());
        if (hasControlNodeChildNodes) {
            nodeWithChilds = controlNodeDetail.getNode();
        } else {
            nodeWithChilds = testNodeDetail.getNode();
        }

        if (nodeWithChilds != null) {
            switch (fieldDiff.getPropertyType()) {
            default: // simple type
                setSimplePropertyDiff((SimplePropertyDiff) fieldDiff,
                        nodeWithChilds, hasControlNodeChildNodes);
                break;
            case list:
                NodeList childNodes = nodeWithChilds.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    ((ListPropertyDiff) fieldDiff).addDiff(getChildNodePropertyDiff(
                            childNodes.item(i), hasControlNodeChildNodes));
                }
                break;
            case complex:
                PropertyDiff childNodeDiff = getChildNodePropertyDiff(
                        nodeWithChilds, hasControlNodeChildNodes);
                if (PropertyType.complex.equals(getPropertyType(nodeWithChilds,
                        null))) {
                    ((ComplexPropertyDiff) fieldDiff).putAll((ComplexPropertyDiff) childNodeDiff);
                } else {
                    ((ComplexPropertyDiff) fieldDiff).putDiff(
                            nodeWithChilds.getNodeName(), childNodeDiff);
                }
                break;
            }
        }
    }

    /**
     * Gets the child node property diff.
     * 
     * @param node the node
     * @param hasControlNodeChildNodes the test node was not found
     * @throws ClientException the client exception
     */
    private static PropertyDiff getChildNodePropertyDiff(Node node,
            boolean hasControlNodeChildNodes) throws ClientException {

        PropertyDiff propertyDiff;

        // Get first child node
        Node firstChildNode = node.getFirstChild();

        // Check empty list item
        boolean isEmptyListItem = false;
        Node parentNode = node.getParentNode();
        if (parentNode != null) {
            PropertyType parentNodeType = getPropertyType(parentNode, null);
            if (PropertyType.list.equals(parentNodeType)) {
                isEmptyListItem = true;
            } else if (PropertyType.simple.equals(parentNodeType)) {
                parentNode = parentNode.getParentNode();
                if (parentNode != null
                        && PropertyType.list.equals(getPropertyType(parentNode,
                                null))) {
                    isEmptyListItem = true;
                }
            }
        }
        isEmptyListItem = isEmptyListItem && firstChildNode == null;
        if (isEmptyListItem) {
            throw new ClientException(
                    "Found an empty list item (<item/>), this should never happen.");
        }

        PropertyType nodePropertyType = getPropertyType(node, null);

        // Manage the specific case of a list child node
        if (firstChildNode != null) {
            PropertyType firstChildNodePropertyType = getPropertyType(
                    firstChildNode, null);

            if (PropertyType.list.equals(firstChildNodePropertyType)) {
                nodePropertyType = getPropertyType(firstChildNode, null);
                node = firstChildNode;
            }
        }

        switch (nodePropertyType) {
        default: // simple type
            propertyDiff = new SimplePropertyDiff();
            setSimplePropertyDiff((SimplePropertyDiff) propertyDiff, node,
                    hasControlNodeChildNodes);
            break;
        case list:
            propertyDiff = new ListPropertyDiff();
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                ((ListPropertyDiff) propertyDiff).addDiff(getChildNodePropertyDiff(
                        childNodes.item(i), hasControlNodeChildNodes));
            }
            break;
        case complex:
            propertyDiff = new ComplexPropertyDiff();
            childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                ((ComplexPropertyDiff) propertyDiff).putDiff(
                        childNode.getNodeName(),
                        getChildNodePropertyDiff(childNode,
                                hasControlNodeChildNodes));
            }
            break;
        }
        return propertyDiff;
    }

    /**
     * Sets the text content of textNode on fieldDiff.
     * 
     * @param fieldDiff the field diff
     * @param textNode the text node
     * @param hasControlNodeContent the has control node content
     */
    private static void setSimplePropertyDiff(SimplePropertyDiff fieldDiff,
            Node textNode, boolean hasControlNodeContent) {

        String textNodeValue = textNode.getTextContent();

        String leftValue = hasControlNodeContent ? textNodeValue : null;
        String rightValue = hasControlNodeContent ? null : textNodeValue;

        fieldDiff.setLeftValue(leftValue);
        fieldDiff.setRightValue(rightValue);
    }

}
