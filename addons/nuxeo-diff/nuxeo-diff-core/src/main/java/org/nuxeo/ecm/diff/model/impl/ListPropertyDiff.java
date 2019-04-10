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
package org.nuxeo.ecm.diff.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.diff.model.PropertyDiff;

/**
 * Implementation of {@link PropertyDiff} for a list property.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class ListPropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    protected Map<Integer, PropertyDiff> diffMap;

    /**
     * Instantiates a new list property diff with a property type.
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
     * Put all diffs.
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
        return diffMap.toString() + super.toString();
    }
}
