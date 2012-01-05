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
package org.nuxeo.ecm.platform.diff.model.impl;

import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyType;

/**
 * Implementation of PropertyDiff for a simple property.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class SimplePropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1100714461537900354L;

    private String leftValue;

    private String rightValue;

    /**
     * Instantiates a new simple property diff.
     */
    public SimplePropertyDiff() {
    }

    /**
     * Instantiates a new simple property diff with leftValue and rightValue.
     * 
     * @param leftValue the left value
     * @param rightValue the right value
     */
    public SimplePropertyDiff(String leftValue, String rightValue) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public PropertyType getPropertyType() {
        return PropertyType.simple;
    }

    public boolean isLeftSideEmpty() {
        return leftValue == null;
    }

    public boolean isRightSideEmpty() {
        return rightValue == null;
    }

    public String getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(String leftValue) {
        this.leftValue = leftValue;
    }

    public String getRightValue() {
        return rightValue;
    }

    public void setRightValue(String rightValue) {
        this.rightValue = rightValue;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof SimplePropertyDiff)) {
            return false;
        }

        String otherLeftValue = ((SimplePropertyDiff) other).getLeftValue();
        String otherRightValue = ((SimplePropertyDiff) other).getRightValue();

        return (leftValue == null && otherLeftValue == null
                && rightValue == null && otherRightValue == null)
                || (leftValue == null && otherLeftValue == null
                        && rightValue != null && rightValue.equals(otherRightValue))
                || (rightValue == null && otherRightValue == null
                        && leftValue != null && leftValue.equals(otherLeftValue))
                || (leftValue != null && rightValue != null
                        && leftValue.equals(otherLeftValue) && rightValue.equals(otherRightValue));
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(leftValue);
        sb.append(" --> ");
        sb.append(rightValue);

        return sb.toString();
    }
}
