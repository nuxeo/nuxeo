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
package org.nuxeo.ecm.diff.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.NodeDetail;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentProperty;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.PropertyHierarchyNode;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper for computing a field diff.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public final class FieldDiffHelper {

    private static final Log LOGGER = LogFactory.getLog(FieldDiffHelper.class);

    public static final String FACET_ELEMENT = "facet";

    public static final String SCHEMA_ELEMENT = "schema";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String TYPE_ATTRIBUTE = "type";

    /**
     * Computes a field diff.
     * <p>
     * First gets all needed elements to compute the field diff:
     * <ul>
     * <li>propertyHierarchy: list holding the property hierarchy
     *
     * <pre>
     * Every time we encounter a list, a complex or a content node going up in the DOM tree
     * from the property node to the prefixed field node, we add it to the property
     * hierarchy.
     * If it is a list item node, we set its index in the hierarchy.
     * If it is a complex item node, we set its name in the hierarchy.
     * If it is a content item node (ie. "encoding", "mime-type", "filename" or "digest"),
     * we set its name in the hierarchy.
     *
     * Example: complex list
     *
     * The "true" property's hierarchy is:
     * [{list,"0"},{complex, "complexBoolean"}]
     *
     * The "jack" property's hierarchy is:
     * [{list,"1"},{complex, "complexString"}]
     *
     * The "UTF-8" property's hierarchy is:
     * [{list,"0"},{complex, "complexString"},{content, "encoding"}]
     *
     * <list type="complexList">
     *   <complexItem type="complex">
     *     <complexString type="string">joe</complexString>
     *     <complexBoolean type="boolean">true</complexBoolean>
     *     <complexContent type="content">
     *      <encoding>UTF-8</encoding>
     *      <mime-type>text/plain</mime-type>
     *      <filename>My_file.txt</filename>
     *      <digest>5dafdabf966043c8c8cef20011e939a2</digest>
     *     </complexContent>
     *   </complexItem>
     *   <complexItem type="complex">
     *     <complexString type="string">jack</complexString>
     *     <complexBoolean type="boolean">false</complexBoolean>
     *   </complexItem>
     * </list>
     * </pre>
     *
     * </li>
     * </ul>
     *
     * @param docDiff the doc diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     * @param fieldDifferenceCount the field difference countadd
     * @param difference the difference
     * @return true if a field diff has been found
     */
    public static boolean computeFieldDiff(DocumentDiff docDiff, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail, int fieldDifferenceCount, Difference difference) {

        // Use control node or if null test node to detect schema and
        // field elements
        Node currentNode = controlNodeDetail.getNode();
        if (currentNode == null) {
            currentNode = testNodeDetail.getNode();
        }
        if (currentNode != null) {

            String field = null;
            String currentNodeName = currentNode.getNodeName();
            List<PropertyHierarchyNode> propertyHierarchy = new ArrayList<PropertyHierarchyNode>();

            // Detect a schema element,
            // for instance: <schema name="dublincore" xmlns:dc="...">.
            // Otherwise build the property hierarchy.
            // For a content type property (blob) don't take into account a
            // difference on the "data" field, since what makes the difference
            // between 2 blobs is either the filename or the digest.
            Node parentNode = currentNode.getParentNode();
            while (parentNode != null && !SCHEMA_ELEMENT.equals(currentNodeName)
                    && !ExportConstants.BLOB_DATA.equals(parentNode.getNodeName())) {

                // Get property type
                String propertyType = getPropertyType(currentNode);
                String parentPropertyType = getPropertyType(parentNode);

                // Fill in property hierarchy
                if (PropertyType.isListType(parentPropertyType)) {
                    int currentNodePosition = getNodePosition(currentNode);
                    propertyHierarchy.add(new PropertyHierarchyNode(parentPropertyType,
                            String.valueOf(currentNodePosition)));
                } else if (PropertyType.isComplexType(parentPropertyType)
                        || PropertyType.isContentType(parentPropertyType)) {
                    propertyHierarchy.add(new PropertyHierarchyNode(parentPropertyType, currentNodeName));
                }

                // Detect a field element, ie. an element that has a
                // prefix, for instance: <dc:title>.
                if (SCHEMA_ELEMENT.equals(parentNode.getNodeName())) {
                    String currentNodeLocalName = currentNode.getLocalName();
                    // TODO: manage better the facet case
                    if (!FACET_ELEMENT.equals(currentNodeLocalName)) {
                        field = currentNodeLocalName;
                        if (PropertyType.isSimpleType(propertyType) || PropertyType.isListType(propertyType)
                                && propertyHierarchy.isEmpty() || PropertyType.isComplexType(propertyType)
                                && propertyHierarchy.isEmpty() || PropertyType.isContentType(propertyType)
                                && propertyHierarchy.isEmpty()) {
                            propertyHierarchy.add(new PropertyHierarchyNode(propertyType, null));
                        }
                    }
                }
                currentNode = parentNode;
                currentNodeName = currentNode.getNodeName();
                parentNode = parentNode.getParentNode();
            }

            // If we found a schema element (ie. we did not
            // reached the root element, ie. parentNode != null) and a
            // nested field element, we can compute the diff for this
            // field.
            if (parentNode != null && field != null && !propertyHierarchy.isEmpty()) {
                String schema = currentNodeName;
                // Get schema name
                NamedNodeMap attr = currentNode.getAttributes();
                if (attr != null && attr.getLength() > 0) {
                    Node nameAttr = attr.getNamedItem(NAME_ATTRIBUTE);
                    if (nameAttr != null) {
                        schema = nameAttr.getNodeValue();
                    }
                }

                // Reverse property hierarchy
                Collections.reverse(propertyHierarchy);

                // Pretty log field difference
                LOGGER.debug(String.format(
                        "Found field difference #%d on [%s]/[%s] with hierarchy %s: [%s (%s)] {%s --> %s}",
                        fieldDifferenceCount + 1, schema, field, propertyHierarchy, difference.getDescription(),
                        difference.getId(), controlNodeDetail.getValue(), testNodeDetail.getValue()));

                // Compute field diff
                computeFieldDiff(docDiff, schema, field, propertyHierarchy, difference.getId(), controlNodeDetail,
                        testNodeDetail);
                // Return true since a field diff has been found
                return true;

            } else {// Non-field difference
                LOGGER.debug(String.format("Found non-field difference: [%s (%s)] {%s --> %s}",
                        difference.getDescription(), difference.getId(), controlNodeDetail.getValue(),
                        testNodeDetail.getValue()));
            }
        }
        return false;
    }

    /**
     * Gets the node property type.
     *
     * @param node the node
     * @return the property diff type
     */
    public static String getPropertyType(Node node) {

        // Default: undefined
        String propertyType = PropertyType.UNDEFINED;

        NamedNodeMap nodeAttr = node.getAttributes();
        if (nodeAttr != null) {
            Node type = nodeAttr.getNamedItem(TYPE_ATTRIBUTE);
            if (type != null) {
                propertyType = type.getNodeValue();
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
     */
    public static PropertyDiff applyPropertyHierarchyToDiff(PropertyDiff firstPropertyDiff,
            List<PropertyHierarchyNode> propertyHierarchy) {

        if (propertyHierarchy.isEmpty()) {
            throw new NuxeoException("Empty property hierarchy.");
        }

        // Get first property hierarchy node
        PropertyHierarchyNode propertyHierarchyNode = propertyHierarchy.get(0);
        String firstPropertyType = propertyHierarchyNode.getNodeType();
        String firstPropertyValue = propertyHierarchyNode.getNodeValue();

        if ((PropertyType.isSimpleType(firstPropertyType) || PropertyType.isContentType(firstPropertyType))
                && propertyHierarchy.size() > 1) {
            throw new NuxeoException(String.format("Inconsistant property hierarchy %s.", propertyHierarchy));
        }

        // Go through the property hierarchy
        PropertyDiff propertyDiff = firstPropertyDiff;
        String propertyType = firstPropertyType;
        String propertyValue = firstPropertyValue;
        for (int i = 1; i < propertyHierarchy.size(); i++) {

            PropertyDiff childPropertyDiff = null;
            PropertyHierarchyNode childPropertyHierarchyNode = propertyHierarchy.get(i);
            String childPropertyType = childPropertyHierarchyNode.getNodeType();
            String childPropertyValue = childPropertyHierarchyNode.getNodeValue();

            // Simple or content type
            if (PropertyType.isSimpleType(propertyType) || PropertyType.isContentType(propertyType)) {
                // Nothing to do here (should never happen)
            }
            // List type
            else if (PropertyType.isListType(propertyType)) {
                int propertyIndex = Integer.parseInt(propertyValue);
                // Get list diff, if null create a new one
                childPropertyDiff = ((ListPropertyDiff) propertyDiff).getDiff(propertyIndex);
                if (childPropertyDiff == null) {
                    childPropertyDiff = newPropertyDiff(childPropertyType);
                    ((ListPropertyDiff) propertyDiff).putDiff(propertyIndex, childPropertyDiff);
                }
                propertyDiff = childPropertyDiff;
            }
            // Complex type
            else {
                // Get complex diff, initialize it if null
                childPropertyDiff = ((ComplexPropertyDiff) propertyDiff).getDiff(propertyValue);
                if (childPropertyDiff == null) {
                    childPropertyDiff = newPropertyDiff(childPropertyType);
                    ((ComplexPropertyDiff) propertyDiff).putDiff(propertyValue, childPropertyDiff);
                }
                propertyDiff = childPropertyDiff;
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
     */
    private static void computeFieldDiff(DocumentDiff docDiff, String schema, String field,
            List<PropertyHierarchyNode> propertyHierarchy, int differenceId, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        if (propertyHierarchy.isEmpty()) {
            throw new NuxeoException("Empty property hierarchy.");
        }

        // Get first property hierarchy node
        PropertyHierarchyNode propertyHierarchyNode = propertyHierarchy.get(0);
        String firstPropertyType = propertyHierarchyNode.getNodeType();

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
        if (!(PropertyType.isSimpleType(firstPropertyType))) {
            endPropertyDiff = applyPropertyHierarchyToDiff(fieldDiff, propertyHierarchy);
        }

        // Compute field diff depending on difference type.
        switch (differenceId) {
        case DifferenceConstants.TEXT_VALUE_ID:
            computeTextValueDiff(endPropertyDiff, controlNodeDetail, testNodeDetail);
            break;
        case DifferenceConstants.CHILD_NODE_NOT_FOUND_ID:
            computeChildNodeNotFoundDiff(endPropertyDiff, controlNodeDetail, testNodeDetail);
            break;
        case DifferenceConstants.HAS_CHILD_NODES_ID:
            computeHasChildNodesDiff(endPropertyDiff, controlNodeDetail, testNodeDetail);
            break;
        default:
            computeTextValueDiff(endPropertyDiff, controlNodeDetail, testNodeDetail);
        }

        schemaDiff.putFieldDiff(field, fieldDiff);
    }

    /**
     * New property diff.
     *
     * @param propertyType the property type
     * @return the property diff
     */
    private static PropertyDiff newPropertyDiff(String propertyType) {

        if (PropertyType.isSimpleType(propertyType)) {
            return new SimplePropertyDiff(propertyType);
        } else if (PropertyType.isListType(propertyType)) {
            return new ListPropertyDiff(propertyType);
        } else if (PropertyType.isComplexType(propertyType)) {
            return new ComplexPropertyDiff();
        } else { // Content type
            return new ContentPropertyDiff();
        }
    }

    /**
     * Computes a TEXT_VALUE diff.
     *
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeTextValueDiff(PropertyDiff fieldDiff, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        String leftValue = controlNodeDetail.getValue();
        String rightValue = testNodeDetail.getValue();

        Node controlNode = controlNodeDetail.getNode();
        if (controlNode == null) {
            throw new NuxeoException("Control node should never be null.");
        }

        Node controlParentNode = controlNode.getParentNode();
        if (controlParentNode == null) {
            throw new NuxeoException("Control parent node should never be null.");
        }

        String controlParentNodePropertyType = getPropertyType(controlParentNode);
        String fieldDiffPropertyType = fieldDiff.getPropertyType();
        // Simple type
        if (PropertyType.isSimpleType(fieldDiffPropertyType)) {
            ((SimplePropertyDiff) fieldDiff).setLeftValue(leftValue);
            ((SimplePropertyDiff) fieldDiff).setRightValue(rightValue);
        }
        // List type
        else if (PropertyType.isListType(fieldDiffPropertyType)) {
            ((ListPropertyDiff) fieldDiff).putDiff(getNodePosition(controlParentNode), new SimplePropertyDiff(
                    controlParentNodePropertyType, leftValue, rightValue));
        }
        // Complex type
        else if (PropertyType.isComplexType(fieldDiffPropertyType)) {
            ((ComplexPropertyDiff) fieldDiff).putDiff(controlParentNode.getNodeName(), new SimplePropertyDiff(
                    controlParentNodePropertyType, leftValue, rightValue));
        }
        // Content type
        else {
            ContentPropertyDiff contentPropertyDiff = ((ContentPropertyDiff) fieldDiff);
            setContentSubPropertyDiff(contentPropertyDiff, controlParentNode.getNodeName(), leftValue, rightValue);
        }
    }

    /**
     * Computes a CHILD_NODE_NOT_FOUND diff.
     *
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeChildNodeNotFoundDiff(PropertyDiff fieldDiff, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        Node childNode;
        boolean isTestNodeNotFound = "null".equals(testNodeDetail.getValue());
        if (!isTestNodeNotFound) {
            childNode = testNodeDetail.getNode();
        } else {
            childNode = controlNodeDetail.getNode();
        }

        if (childNode == null) {
            throw new NuxeoException("Child node should never be null.");
        }

        String propertyType = fieldDiff.getPropertyType();
        // Simple type
        if (PropertyType.isSimpleType(propertyType)) {
            // Should never happen as then it would be marked as a
            // HAS_CHILD_NODES difference.
            throw new NuxeoException("A CHILD_NODE_NOT_FOUND difference should never be found within a simple type.");
        }
        // List type
        else if (PropertyType.isListType(propertyType)) {
            PropertyDiff childNodeDiff = getChildNodePropertyDiff(childNode, isTestNodeNotFound);
            ((ListPropertyDiff) fieldDiff).putDiff(getNodePosition(childNode), childNodeDiff);
        }
        // Complex type
        else if (PropertyType.isComplexType(propertyType)) { // Complex type
            throw new NuxeoException("A CHILD_NODE_NOT_FOUND difference should never be found within a complex type.");
        }
        // Content type
        else {
            throw new NuxeoException("A CHILD_NODE_NOT_FOUND difference should never be found within a content type.");
        }
    }

    /**
     * Computes a HAS_CHILD_NODES diff.
     *
     * @param fieldDiff the field diff
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeHasChildNodesDiff(PropertyDiff fieldDiff, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        Node nodeWithChildren;
        boolean hasControlNodeChildNodes = Boolean.valueOf(controlNodeDetail.getValue());
        if (hasControlNodeChildNodes) {
            nodeWithChildren = controlNodeDetail.getNode();
        } else {
            nodeWithChildren = testNodeDetail.getNode();
        }

        if (nodeWithChildren == null) {
            throw new NuxeoException("Node with children should never be null.");
        }

        String propertyType = fieldDiff.getPropertyType();
        // Simple type
        if (PropertyType.isSimpleType(propertyType)) {
            setSimplePropertyDiff((SimplePropertyDiff) fieldDiff, nodeWithChildren, hasControlNodeChildNodes);
        }
        // List type
        else if (PropertyType.isListType(propertyType)) {
            PropertyDiff childNodeDiff = getChildNodePropertyDiff(nodeWithChildren, hasControlNodeChildNodes);
            if (PropertyType.isListType(getPropertyType(nodeWithChildren))) {
                ((ListPropertyDiff) fieldDiff).putAllDiff((ListPropertyDiff) childNodeDiff);
            } else {
                ((ListPropertyDiff) fieldDiff).putDiff(getNodePosition(nodeWithChildren), childNodeDiff);
            }
        }
        // Complex type
        else if (PropertyType.isComplexType(propertyType)) {
            PropertyDiff childNodeDiff = getChildNodePropertyDiff(nodeWithChildren, hasControlNodeChildNodes);
            if (PropertyType.isComplexType(getPropertyType(nodeWithChildren))) {
                ((ComplexPropertyDiff) fieldDiff).putAllDiff((ComplexPropertyDiff) childNodeDiff);
            } else {
                ((ComplexPropertyDiff) fieldDiff).putDiff(nodeWithChildren.getNodeName(), childNodeDiff);
            }
        }
        // Content type
        else {
            if (PropertyType.isContentType(getPropertyType(nodeWithChildren))) {
                PropertyDiff childNodeDiff = getChildNodePropertyDiff(nodeWithChildren, hasControlNodeChildNodes);
                ((ContentPropertyDiff) fieldDiff).setLeftContent(((ContentPropertyDiff) childNodeDiff).getLeftContent());
                ((ContentPropertyDiff) fieldDiff).setRightContent(((ContentPropertyDiff) childNodeDiff).getRightContent());
            } else {
                setContentPropertyDiff((ContentPropertyDiff) fieldDiff, nodeWithChildren, hasControlNodeChildNodes);
            }
        }
    }

    /**
     * Gets the child node property diff.
     *
     * @param node the node
     * @param hasControlNodeChildNodes the test node was not found
     */
    private static PropertyDiff getChildNodePropertyDiff(Node node, boolean hasControlNodeChildNodes)
            {

        PropertyDiff propertyDiff;

        String nodePropertyType = getPropertyType(node);

        // Simple type
        if (PropertyType.isSimpleType(nodePropertyType)) {
            propertyDiff = new SimplePropertyDiff(nodePropertyType);
            setSimplePropertyDiff((SimplePropertyDiff) propertyDiff, node, hasControlNodeChildNodes);
        }
        // List type
        else if (PropertyType.isListType(nodePropertyType)) {
            propertyDiff = new ListPropertyDiff(nodePropertyType);
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                ((ListPropertyDiff) propertyDiff).putDiff(i,
                        getChildNodePropertyDiff(childNodes.item(i), hasControlNodeChildNodes));
            }
        }
        // Complex type
        else if (PropertyType.isComplexType(nodePropertyType)) {
            propertyDiff = new ComplexPropertyDiff();
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                ((ComplexPropertyDiff) propertyDiff).putDiff(childNode.getNodeName(),
                        getChildNodePropertyDiff(childNode, hasControlNodeChildNodes));
            }
        }
        // Content type
        else {
            propertyDiff = new ContentPropertyDiff();
            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                setContentPropertyDiff((ContentPropertyDiff) propertyDiff, childNode, hasControlNodeChildNodes);
            }
        }
        return propertyDiff;
    }

    /**
     * Sets the text content of textNode on {@link SimplePropertyDiff} field diff.
     *
     * @param fieldDiff the field diff
     * @param textNode the text node
     * @param hasControlNodeContent the has control node content
     */
    private static void setSimplePropertyDiff(SimplePropertyDiff fieldDiff, Node textNode, boolean hasControlNodeContent) {

        String textNodeValue = textNode.getTextContent();

        String leftValue = hasControlNodeContent ? textNodeValue : null;
        String rightValue = hasControlNodeContent ? null : textNodeValue;

        fieldDiff.setLeftValue(leftValue);
        fieldDiff.setRightValue(rightValue);
    }

    /**
     * Sets the text content of textNode on a {@link ContentPropertyDiff} field diff.
     *
     * @param fieldDiff the field diff
     * @param textNode the text node
     * @param hasControlNodeContent the has control node content
     */
    private static void setContentPropertyDiff(ContentPropertyDiff fieldDiff, Node textNode,
            boolean hasControlNodeContent) {

        String textNodeValue = textNode.getTextContent();

        String leftValue = hasControlNodeContent ? textNodeValue : null;
        String rightValue = hasControlNodeContent ? null : textNodeValue;

        setContentSubPropertyDiff(fieldDiff, textNode.getNodeName(), leftValue, rightValue);
    }

    protected static void setContentSubPropertyDiff(ContentPropertyDiff fieldDiff, String subPropertyName,
            String leftSubPropertyValue, String rightSubPropertyValue) {

        // Get or initialize left and right content
        ContentProperty leftContent = fieldDiff.getLeftContent();
        ContentProperty rightContent = fieldDiff.getRightContent();
        if (leftContent == null) {
            leftContent = new ContentProperty();
            fieldDiff.setLeftContent(leftContent);
        }
        if (rightContent == null) {
            rightContent = new ContentProperty();
            fieldDiff.setRightContent(rightContent);
        }

        // Set sub property on left and right content
        leftContent.setSubProperty(subPropertyName, leftSubPropertyValue);
        rightContent.setSubProperty(subPropertyName, rightSubPropertyValue);

        // Set difference type on content property diff
        if (ExportConstants.BLOB_FILENAME.equals(subPropertyName)) {
            if (DifferenceType.differentDigest.equals(fieldDiff.getDifferenceType())) {
                fieldDiff.setDifferenceType(DifferenceType.different);
            } else {
                fieldDiff.setDifferenceType(DifferenceType.differentFilename);
            }
        } else if (ExportConstants.BLOB_DIGEST.equals(subPropertyName)) {
            if (DifferenceType.differentFilename.equals(fieldDiff.getDifferenceType())) {
                fieldDiff.setDifferenceType(DifferenceType.different);
            } else {
                fieldDiff.setDifferenceType(DifferenceType.differentDigest);
            }
        }
    }
}
