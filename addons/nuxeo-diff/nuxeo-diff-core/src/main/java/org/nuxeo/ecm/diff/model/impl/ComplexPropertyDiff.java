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
