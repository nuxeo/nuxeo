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
package org.nuxeo.ecm.platform.diff.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.diff.model.PropertyDiff;

/**
 * Implementation of PropertyDiff for a list property.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class ListPropertyDiff implements PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    private List<PropertyDiff> diffList;

    /**
     * Instantiates a new list property diff.
     */
    public ListPropertyDiff() {
        diffList = new ArrayList<PropertyDiff>();
    }

    public void addDiff(PropertyDiff diff) {
        diffList.add(diff);
    }

    public List<PropertyDiff> getDiffList() {
        return diffList;
    }

    public void setDiffList(List<PropertyDiff> diffList) {
        this.diffList = diffList;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other == null || !(other instanceof ListPropertyDiff)) {
            return false;
        }

        List<PropertyDiff> otherDiffList = ((ListPropertyDiff) other).getDiffList();

        return (diffList == null && otherDiffList == null)
                || (diffList != null && otherDiffList != null && diffList.equals(otherDiffList));

    }

    @Override
    public String toString() {

        return diffList.toString();
    }
}
