/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.NodeDetail;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiffType;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper for computing a field diff.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public final class FieldDiffHelper {

    /**
     * Computes a field diff.
     * 
     * @param docDiff the doc diff
     * @param schema the schema
     * @param field the field
     * @param propertyDiffType the property diff type
     * @param differenceId the difference id
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    public static void computeFieldDiff(DocumentDiff docDiff, String schema,
            String field, PropertyDiffType propertyDiffType, int differenceId,
            NodeDetail controlNodeDetail, NodeDetail testNodeDetail) {

        // Get schema diff, initialize it if null
        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        if (schemaDiff == null) {
            schemaDiff = docDiff.initSchemaDiff(schema);
        }

        // Get field diff, initialize it if null
        PropertyDiff fieldDiff = schemaDiff.getFieldDiff(field);
        if (fieldDiff == null) {
            switch (propertyDiffType) {
            default:
                fieldDiff = new SimplePropertyDiff();
                break;
            case list:
                fieldDiff = new ListPropertyDiff();
                break;
            case complex:
                fieldDiff = new ComplexPropertyDiff();
                break;
            }
        }

        // Compute field diff depending on difference type.
        switch (differenceId) {
        default:// In most cases: TEXT_VALUE_ID
            computeTextValueDiff(fieldDiff, propertyDiffType,
                    controlNodeDetail, testNodeDetail);
            break;
        case DifferenceConstants.CHILD_NODE_NOT_FOUND_ID:
            computeChildNodeNotFoundDiff(fieldDiff, propertyDiffType,
                    controlNodeDetail, testNodeDetail);
            break;
        case DifferenceConstants.HAS_CHILD_NODES_ID:
            computeHasChildNodesDiff(fieldDiff, propertyDiffType,
                    controlNodeDetail, testNodeDetail);
            break;
        }

        schemaDiff.putFieldDiff(field, fieldDiff);
    }

    /**
     * Computes a TEXT_VALUE diff.
     * 
     * @param fieldDiff the field diff
     * @param propertyDiffType the property diff type
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeTextValueDiff(PropertyDiff fieldDiff,
            PropertyDiffType propertyDiffType, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        String leftValue = controlNodeDetail.getValue();
        String rightValue = testNodeDetail.getValue();

        switch (propertyDiffType) {
        default:
            ((SimplePropertyDiff) fieldDiff).setLeftValue(leftValue);
            ((SimplePropertyDiff) fieldDiff).setRightValue(rightValue);
            break;
        case list:
            ((ListPropertyDiff) fieldDiff).addDiff(new SimplePropertyDiff(
                    leftValue, rightValue));
            break;
        case complex:
            break;
        }
    }

    /**
     * Computes a CHILD_NODE_NOT_FOUND diff.
     * 
     * @param fieldDiff the field diff
     * @param propertyDiffType the property diff type
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeChildNodeNotFoundDiff(PropertyDiff fieldDiff,
            PropertyDiffType propertyDiffType, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        Node childNode;
        boolean isControlNodeNotFound = "null".equals(controlNodeDetail.getValue());
        if (isControlNodeNotFound) {
            childNode = testNodeDetail.getNode();
        } else {
            childNode = controlNodeDetail.getNode();
        }

        if (childNode != null) {
            String childTextNodeValue = childNode.getTextContent();

            String leftValue = isControlNodeNotFound ? null
                    : childTextNodeValue;
            String rightValue = isControlNodeNotFound ? childTextNodeValue
                    : null;

            switch (propertyDiffType) {
            default:
                ((SimplePropertyDiff) fieldDiff).setLeftValue(leftValue);
                ((SimplePropertyDiff) fieldDiff).setRightValue(rightValue);
                break;
            case list:
                ((ListPropertyDiff) fieldDiff).addDiff(new SimplePropertyDiff(
                        leftValue, rightValue));
                break;
            case complex:
                break;
            }
        }
    }

    /**
     * Computes a HAS_CHILD_NODES diff.
     * 
     * @param fieldDiff the field diff
     * @param propertyDiffType the property diff type
     * @param controlNodeDetail the control node detail
     * @param testNodeDetail the test node detail
     */
    private static void computeHasChildNodesDiff(PropertyDiff fieldDiff,
            PropertyDiffType propertyDiffType, NodeDetail controlNodeDetail,
            NodeDetail testNodeDetail) {

        Node nodeWithChilds;
        boolean hasControlNodeChildNodes = Boolean.valueOf(controlNodeDetail.getValue());
        if (hasControlNodeChildNodes) {
            nodeWithChilds = controlNodeDetail.getNode();
        } else {
            nodeWithChilds = testNodeDetail.getNode();
        }

        if (nodeWithChilds != null) {
            switch (propertyDiffType) {
            default:
                setChildTextNodeValue((SimplePropertyDiff) fieldDiff,
                        hasControlNodeChildNodes, nodeWithChilds);
                break;
            case list:
                setChildElementValues((ListPropertyDiff) fieldDiff,
                        hasControlNodeChildNodes, nodeWithChilds);
                break;
            case complex:
                break;
            }
        }
    }

    /**
     * Sets the child text node value from nodeWithChilds on fieldDiff.
     * 
     * @param fieldDiff the field diff
     * @param hasControlNodeChildNodes the control node has child nodes
     * @param nodeWithChilds the node with childs
     */
    private static void setChildTextNodeValue(SimplePropertyDiff fieldDiff,
            boolean hasControlNodeChildNodes, Node nodeWithChilds) {

        String childTextNodeValue = nodeWithChilds.getTextContent();

        String leftValue = hasControlNodeChildNodes ? childTextNodeValue : null;
        String rightValue = hasControlNodeChildNodes ? null
                : childTextNodeValue;

        fieldDiff.setLeftValue(leftValue);
        fieldDiff.setRightValue(rightValue);
    }

    /**
     * Sets the child element values from nodeWithChilds on fieldDiff.
     * 
     * @param fieldDiff the field diff
     * @param controlNodeHasChildNodes the control node has child nodes
     * @param nodeWithChilds the node with childs
     */
    private static void setChildElementValues(ListPropertyDiff fieldDiff,
            boolean controlNodeHasChildNodes, Node nodeWithChilds) {

        NodeList childNodes = nodeWithChilds.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            String childNodeValue = childNodes.item(i).getTextContent();

            String leftValue = controlNodeHasChildNodes ? childNodeValue : null;
            String rightValue = controlNodeHasChildNodes ? null
                    : childNodeValue;

            fieldDiff.addDiff(new SimplePropertyDiff(leftValue, rightValue));
        }
    }

}
