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
package org.nuxeo.ecm.diff.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.diff.model.PropertyDiff;

/**
 * Implementation of PropertyDiff for a list property.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class ListPropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    private Map<Integer, PropertyDiff> diffMap;

    /**
     * Instantiates a new list property diff.
     */
    public ListPropertyDiff(String propertyType) {

        this.propertyType = propertyType;
        this.diffMap = new HashMap<Integer, PropertyDiff>();
    }

    /**
     * Gets the diff.
     *
     * @param index the index
     * @return the diff
     */
    public PropertyDiff getDiff(int index) {
        return diffMap.get(index);
    }

    /**
     * Puts the diff.
     *
     * @param index the index
     * @param diff the diff
     * @return the property diff
     */
    public PropertyDiff putDiff(int index, PropertyDiff diff) {
        return diffMap.put(index, diff);
    }

    /**
     * Put all diff.
     *
     * @param otherDiff the other diff
     */
    public void putAllDiff(ListPropertyDiff otherDiff) {
        diffMap.putAll(otherDiff.getDiffMap());
    }

    /**
     * Size.
     *
     * @return the diff map size
     */
    public int size() {
        return diffMap.size();
    }

    public Map<Integer, PropertyDiff> getDiffMap() {
        return diffMap;
    }

    /**
     * Gets the diff indexes.
     *
     * @return the diff indexes
     */
    public List<Integer> getDiffIndexes() {
        return new ArrayList<Integer>(diffMap.keySet());
    }

    @Override
    public boolean equals(Object other) {

        if (!super.equals(other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof ListPropertyDiff)) {
            return false;
        }
        Map<Integer, PropertyDiff> otherDiffMap = ((ListPropertyDiff) other).getDiffMap();
        return (diffMap == null && otherDiffMap == null)
                || (diffMap != null && otherDiffMap != null && diffMap.equals(otherDiffMap));

    }

    @Override
    public String toString() {

        return diffMap.toString();
    }
}
