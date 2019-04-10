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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;

/**
 * Implementation of {@link PropertyDiff} for a complex property.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class ComplexPropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    protected Map<String, PropertyDiff> diffMap;

    /**
     * Instantiates a new complex property diff with the {@link PropertyType#COMPLEX} property type.
     */
    public ComplexPropertyDiff() {

        this.propertyType = PropertyType.COMPLEX;
        diffMap = new HashMap<String, PropertyDiff>();
    }

    /**
     * Instantiates a new complex property diff with a property type.
     */
    public ComplexPropertyDiff(String propertyType) {

        this.propertyType = propertyType;
        diffMap = new HashMap<String, PropertyDiff>();
    }

    /**
     * Gets the diff.
     *
     * @param item the item
     * @return the diff
     */
    public PropertyDiff getDiff(String item) {
        return diffMap.get(item);
    }

    /**
     * Put diff.
     *
     * @param item the item
     * @param diff the diff
     * @return the property diff
     */
    public PropertyDiff putDiff(String item, PropertyDiff diff) {
        return diffMap.put(item, diff);
    }

    /**
     * Put all diffs.
     *
     * @param otherDiff the other diff
     */
    public void putAllDiff(ComplexPropertyDiff otherDiff) {
        diffMap.putAll(otherDiff.getDiffMap());
    }

    public Map<String, PropertyDiff> getDiffMap() {
        return diffMap;
    }

    @Override
    public boolean equals(Object other) {

        if (!super.equals(other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof ComplexPropertyDiff)) {
            return false;
        }
        Map<String, PropertyDiff> otherDiffMap = ((ComplexPropertyDiff) other).getDiffMap();
        return (diffMap == null && otherDiffMap == null)
                || (diffMap != null && otherDiffMap != null && diffMap.equals(otherDiffMap));

    }

    @Override
    public String toString() {
        return diffMap.toString() + super.toString();
    }
}
