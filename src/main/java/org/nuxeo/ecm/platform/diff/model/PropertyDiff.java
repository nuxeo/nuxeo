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
package org.nuxeo.ecm.platform.diff.model;

import java.io.Serializable;

/**
 * <p>
 * Representation of a property (or field) diff.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class PropertyDiff implements Serializable {

    private static final long serialVersionUID = -8458912212588012911L;

    protected String propertyType;

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof PropertyDiff)) {
            return false;
        }
        String otherPropertyType = ((PropertyDiff) other).getPropertyType();
        return (propertyType == null && otherPropertyType == null)
                || (propertyType != null && otherPropertyType != null && propertyType.equals(otherPropertyType));
    }
}
