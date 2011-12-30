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

import java.util.Arrays;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;
import org.w3c.dom.Node;

/**
 * Implementation ...
 * <p>
 * The clas...
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class IgnoreStructuralDifferenceListener implements DifferenceListener {

    private static final int[] IGNORE = new int[] {
            DifferenceConstants.HAS_DOCTYPE_DECLARATION_ID,
            DifferenceConstants.DOCTYPE_NAME_ID,
            DifferenceConstants.DOCTYPE_PUBLIC_ID_ID,
            DifferenceConstants.DOCTYPE_SYSTEM_ID_ID,
            DifferenceConstants.NAMESPACE_URI_ID,
            DifferenceConstants.ATTR_VALUE_ID,
            DifferenceConstants.ELEMENT_TAG_NAME_ID,
            DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID,
            DifferenceConstants.CHILD_NODELIST_LENGTH_ID};

    static {
        Arrays.sort(IGNORE);
    }

    public int differenceFound(Difference difference) {
        return Arrays.binarySearch(IGNORE, difference.getId()) >= 0 ? RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL
                : RETURN_ACCEPT_DIFFERENCE;
    }

    public void skippedComparison(Node control, Node test) {
    }
}