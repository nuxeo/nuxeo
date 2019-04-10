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
package org.nuxeo.ecm.diff.service.impl;

import java.util.Arrays;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.NodeDetail;
import org.w3c.dom.Node;

/**
 * Custom DifferenceListener for XMLUnit Diff.
 * <p>
 * It ignores what we call "structural" differences, ie.:
 * <ul>
 * <li>DOCTYPE related</li>
 * <li>Namespace URI</li>
 * <li>Attribute value</li>
 * <li>Attribute name not found</li>
 * <li>Element tag name</li>
 * <li>Child node list sequence</li>
 * <li>Child node list length</li>
 * </ul>
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class IgnoreStructuralDifferenceListener implements DifferenceListener {

    private static final String SCHEMA_ELEMENT = "schema";

    /**
     * Difference types to be ignored.
     */
    private static final int[] IGNORE = new int[] { DifferenceConstants.HAS_DOCTYPE_DECLARATION_ID,
            DifferenceConstants.DOCTYPE_NAME_ID, DifferenceConstants.DOCTYPE_PUBLIC_ID_ID,
            DifferenceConstants.DOCTYPE_SYSTEM_ID_ID, DifferenceConstants.NAMESPACE_URI_ID,
            DifferenceConstants.ATTR_VALUE_ID, DifferenceConstants.ATTR_NAME_NOT_FOUND_ID,
            DifferenceConstants.ELEMENT_TAG_NAME_ID, DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID,
            DifferenceConstants.CHILD_NODELIST_LENGTH_ID };

    static {
        Arrays.sort(IGNORE);
    }

    /**
     * Here want to:
     * <ul>
     * <li>Take into account all difference types to be ignored.</li>
     * <li>Not consider an unbalanced schema, ie. a schema that exists for a document but not for the other one, as a
     * difference.</li>
     * </ul>
     */
    public int differenceFound(Difference difference) {

        boolean unBalancedSchema = false;

        NodeDetail controlNodeDetail = difference.getControlNodeDetail();
        NodeDetail testNodeDetail = difference.getTestNodeDetail();

        if (controlNodeDetail != null && testNodeDetail != null) {

            Node controlNode = controlNodeDetail.getNode();
            Node testNode = testNodeDetail.getNode();

            if (controlNode != null && SCHEMA_ELEMENT.equals(controlNode.getNodeName()) && testNode == null) {
                unBalancedSchema = true;
            }

            if (!unBalancedSchema && testNode != null && SCHEMA_ELEMENT.equals(testNode.getNodeName())
                    && controlNode == null) {
                unBalancedSchema = true;
            }
        }

        return (Arrays.binarySearch(IGNORE, difference.getId()) >= 0 || unBalancedSchema) ? RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL
                : RETURN_ACCEPT_DIFFERENCE;
    }

    public void skippedComparison(Node control, Node test) {
    }
}
